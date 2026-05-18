package pl.skidam.automodpack_velocity.proxy.handler;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.util.ReferenceCountUtil;
import pl.skidam.automodpack_velocity.Constants;
import pl.skidam.automodpack_velocity.protocol.netty.TrafficShaper;
import pl.skidam.automodpack_velocity.protocol.netty.detectors.AMMHDetector;
import pl.skidam.automodpack_velocity.protocol.netty.detectors.HAProxyDetector;
import pl.skidam.automodpack_velocity.protocol.netty.detectors.MatchResult;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static pl.skidam.automodpack_velocity.Constants.logger;
import static pl.skidam.automodpack_velocity.Constants.MOD_ID;
import static pl.skidam.automodpack_velocity.Constants.*;
import static pl.skidam.automodpack_velocity.proxy.TcpProxy.closeOnFlush;
import static pl.skidam.automodpack_velocity.utils.VelocityHelper.getServersToTry;

public class ProxyFrontendHandler extends ByteToMessageDecoder {
    private boolean proxyCheckFinished = false;
    private boolean magicCheckFinished = false;
    private SocketAddress remoteAddress = null;
    private ByteBuf originalBuffer = null;

    private volatile Channel backendChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.remoteAddress = ctx.channel().remoteAddress();
        ctx.read();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!proxyCheckFinished) {
            MatchResult res = handleProxyCheck(ctx, in);
            if (res == MatchResult.PARTIAL) {
                return;
            }
            proxyCheckFinished = true;
        }

        if (!magicCheckFinished) {
            MatchResult res = handleMagicCheck(ctx, in, out);
            if (res == MatchResult.PARTIAL) {
                return;
            }
            magicCheckFinished = true;
        }

        // AMMH is done — retain and pass all remaining bytes downstream as a regular message.
        if (in.isReadable()) {
            out.add(in.retain());
        }
    }

    private MatchResult handleProxyCheck(ChannelHandlerContext ctx, ByteBuf in) {
        MatchResult result = HAProxyDetector.check(in);
        if (result != MatchResult.MATCHED) {
            return result;
        }

        HAProxyDetector.DecodeResult decodeResult = HAProxyDetector.decode(in);
        if (decodeResult == null) return MatchResult.PARTIAL;
        if (decodeResult.message() == null) return MatchResult.MISMATCH;

        onProxyMatch(ctx, in, decodeResult);
        return MatchResult.MATCHED;
    }

    private MatchResult handleMagicCheck(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        MatchResult result = AMMHDetector.check(in);
        if (result != MatchResult.MATCHED) {
            if (result == MatchResult.MISMATCH) onMagicMismatch(ctx, in, out);
            return result;
        }

        AMMHDetector.DecodeResult decodeResult = AMMHDetector.decode(in);
        if (decodeResult == null) return MatchResult.PARTIAL;
        if (decodeResult.hostname() == null) return MatchResult.MISMATCH;

        onMagicMatch(ctx, in, decodeResult);
        return MatchResult.MATCHED;
    }

    private void onProxyMatch(ChannelHandlerContext ctx, ByteBuf in, HAProxyDetector.DecodeResult result) {
        HAProxyMessage msg = result.message();
        try {
            appendConsumedBytes(ctx, in.readRetainedSlice(result.consumedBytes()));

            if (msg != null && msg.sourceAddress() != null) {
                remoteAddress = new InetSocketAddress(msg.sourceAddress(), msg.sourcePort());
                logger.debug("[onProxyMatch] Remote address set to {}", remoteAddress);
            }
        } catch (Exception e) {
            logger.error("Error processing HAProxy message", e);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void onMagicMatch(ChannelHandlerContext ctx, ByteBuf in, AMMHDetector.DecodeResult result) {
        in.skipBytes(result.consumedBytes());
        logger.debug("[onMagicMatch] AMMH Hostname: {}", result.hostname());

        // Retrieve list of backend servers Velocity will attempt to connect them to based on the hostname
        List<String> serversToTry = getServersToTry(result.hostname());

        safeReleaseOriginalBuffer();

        // Claim this channel striping all Velocity handlers before any async work.
        // This prevents read-timeout, minecraft-decoder, and handler from killing the channel
        stripVelocityHandlers(ctx);

        Channel clientChannel = ctx.channel();
        clientChannel.config().setAutoRead(false); // pause until backend ready

        // After handshake completes in onMagicMatch, replace FrontendHandler with a
        // simple forwarding handler that stays in the pipeline (so traffic shaper sees everything):
        ctx.pipeline().replace(this, MOD_ID + "_forwarder", new Forwarder());

        tryNextServer(clientChannel, serversToTry, 0, result.hostname());
    }

    class Forwarder extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            backendChannel.writeAndFlush(msg.retain())
                    .addListener(f -> { if (!f.isSuccess()) closeOnFlush(backendChannel); });
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel bc = backendChannel;
            backendChannel = null;
            if (bc != null && bc.isActive()) {
                ((SocketChannel) bc).shutdownOutput();
            }
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.warn("Pipeline handler ProxyFrontendHandler.Forwarder exception: {}",
                    cause.getMessage());
            Channel bc = backendChannel;
            backendChannel = null;
            if (bc != null) closeOnFlush(bc);
            closeOnFlush(ctx.channel());
        }
    }

    private void tryNextServer(Channel clientChannel, List<String> serversToTry, int index, String hostname) {
        if (index >= serversToTry.size()) {
            logger.warn("All {} backend(s) exhausted for host '{}'", serversToTry.size(), hostname);
            clientChannel.close();
            return;
        }

        String serverName = serversToTry.get(index);
        Optional<RegisteredServer> registered = proxyServer.getServer(serverName);
        if (registered.isEmpty()) {
            tryNextServer(clientChannel, serversToTry, index + 1, hostname); // recurse to next
            return;
        }

        ServerInfo serverInfo = registered.get().getServerInfo();
        InetSocketAddress serverAddress = serverInfo.getAddress();

        // Look up cached AM port for this backend address
        Integer amPort = backendPortCache.get(serverAddress);
        if (amPort == null) {
            logger.warn("[tryNextServer] No entry found for {}", serverAddress);
            tryNextServer(clientChannel, serversToTry, index + 1, hostname);
            return;
        }

        InetSocketAddress effectiveAddress;
        if (amPort == -1) {
            effectiveAddress = serverAddress;
        } else {
            effectiveAddress = new InetSocketAddress(serverAddress.getHostString(), amPort);
        }

        logger.debug("[tryNextServer] Trying to connect to server: {} (originally {})", effectiveAddress, serverAddress);

        Bootstrap b = new Bootstrap()
                .group(clientChannel.eventLoop())
                .channel(clientChannel.getClass())
                .option(ChannelOption.AUTO_READ, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addFirst("traffic-shaper",
                                TrafficShaper.trafficShaper.getTrafficShapingHandler()
                        );
                        ch.pipeline().addLast(new ProxyBackendHandler(clientChannel));
                    }
                });

        b.connect(effectiveAddress).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                logger.warn("Cannot connect to backend '{}' for host '{}': {}; trying next",
                        serverName, hostname, future.cause().getMessage());
                tryNextServer(clientChannel, serversToTry, index + 1, hostname); // recurse to next
                return;
            }

            logger.debug("[tryNextServer] Established proxy connection {} (client) <-> {} (backend server)",
                    this.remoteAddress, effectiveAddress);

            backendChannel = future.channel();

            byte[] hostBytes = hostname.getBytes(StandardCharsets.UTF_8);
            ByteBuf ammh = backendChannel.alloc().buffer(6 + hostBytes.length);
            ammh.writeInt(Constants.MAGIC_AMMH);
            ammh.writeShort(hostBytes.length);
            ammh.writeBytes(hostBytes);
            backendChannel.writeAndFlush(ammh);

            clientChannel.config().setAutoRead(true);
        });
    }

    private void stripVelocityHandlers(ChannelHandlerContext ctx) {
        ctx.pipeline().toMap().forEach((name, handler) -> {
            if (handler == this) return;
            ctx.pipeline().remove(handler);
        });
    }

    private void onMagicMismatch(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        boolean isSharedPort = proxyConfig.bindPort == -1;
        if (isSharedPort) {
            fallbackToOriginalPipeline(ctx, in, out);
        } else {
            // Not a shared-port and no AMMH magic — unknown client, just close.
            ctx.close();
        }
    }

    private void fallbackToOriginalPipeline(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        ByteBuf payload = reconstructPayload(ctx, in);
        out.add(payload);
        ctx.pipeline().remove(this);
    }

    private ByteBuf reconstructPayload(ChannelHandlerContext ctx, ByteBuf in) {
        if (originalBuffer == null) {
            return in.readRetainedSlice(in.readableBytes());
        }

        CompositeByteBuf composite = ctx.alloc().compositeBuffer();
        // Add originalBuffer (Proxy header bytes)
        composite.addComponent(true, originalBuffer);

        // originalBuffer ownership is now with the Composite.
        // We null it out so we don't accidentally release it again in channelInactive.
        originalBuffer = null;

        // Add the current buffer content
        composite.addComponent(true, in.readRetainedSlice(in.readableBytes()));
        return composite;
    }

    private void appendConsumedBytes(ChannelHandlerContext ctx, ByteBuf consumed) {
        if (originalBuffer == null) {
            originalBuffer = consumed;
            return;
        }

        if (originalBuffer instanceof CompositeByteBuf) {
            ((CompositeByteBuf) originalBuffer).addComponent(true, consumed);
        } else {
            CompositeByteBuf composite = ctx.alloc().compositeBuffer();
            composite.addComponent(true, originalBuffer);
            composite.addComponent(true, consumed);
            originalBuffer = composite;
        }
    }

    private void safeReleaseOriginalBuffer() {
        if (originalBuffer != null) {
            if (originalBuffer.refCnt() > 0) originalBuffer.release();
            originalBuffer = null;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        safeReleaseOriginalBuffer();
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("Pipeline handler ProxyFrontendHandler exception: {}", cause.getMessage());
        closeOnFlush(ctx.channel());
    }
}
