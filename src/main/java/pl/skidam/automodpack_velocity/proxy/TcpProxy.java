package pl.skidam.automodpack_velocity.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import pl.skidam.automodpack_velocity.protocol.netty.TrafficShaper;
import pl.skidam.automodpack_velocity.utils.CustomThreadFactoryBuilder;
import pl.skidam.automodpack_velocity.proxy.handler.ProxyFrontendHandler;

import java.net.InetSocketAddress;

import static pl.skidam.automodpack_velocity.Constants.logger;
import static pl.skidam.automodpack_velocity.Constants.MOD_ID;
import static pl.skidam.automodpack_velocity.Constants.proxyConfig;

/**
 * Bidirectional TCP proxy that forwards AM protocol connections from clients
 * to the appropriate backend's AutoModpack NettyServer, routing by virtual hostname.
 *
 * <p>Clients always send the AMMH magic handshake (requiresMagic=true in the data
 * packet), which lets the proxy extract the virtual hostname from the first plaintext
 * bytes and look up the correct backend without any per-player global state.
 *
 * <p>If the resolved backend is in <em>shared-port mode</em>, the proxy additionally
 * sends its own AMMH to the backend and consumes the AMOK response.
 */
public final class TcpProxy {

    private MultithreadEventLoopGroup eventLoopGroup;
    private ChannelFuture serverChannel;

    // -----------------------------------------------------------------------

    public void start() {
        if (!proxyConfig.proxyHost) {
            return;
        }

        // Shared port
        if (proxyConfig.port == -1) {
            new TrafficShaper(null, proxyConfig.bandwidthLimit, TrafficShaper.TrafficShaperType.READ);
            return;
        }

        try {
            String address = proxyConfig.address;
            int port = proxyConfig.port;
            InetSocketAddress bindAddress;
            if (address == null || address.isBlank()) {
                bindAddress = new InetSocketAddress(port);
            } else {
                bindAddress = new InetSocketAddress(address, port);
            }

            logger.info("Starting modpack host server on {}", bindAddress);

            Class<? extends ServerChannel> socketChannelClass;
            if (Epoll.isAvailable()) {
                socketChannelClass = EpollServerSocketChannel.class;
                eventLoopGroup = new EpollEventLoopGroup(new CustomThreadFactoryBuilder().setNameFormat("AutoModpack Epoll Server IO #%d").setDaemon(true).build());
            } else {
                socketChannelClass = NioServerSocketChannel.class;
                eventLoopGroup = new NioEventLoopGroup(new CustomThreadFactoryBuilder().setNameFormat("AutoModpack Server IO #%d").setDaemon(true).build());
            }

            new TrafficShaper(eventLoopGroup, proxyConfig.bandwidthLimit, TrafficShaper.TrafficShaperType.READ);

            serverChannel = new ServerBootstrap()
                    .channel(socketChannelClass)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // Proxy handler
                            ch.pipeline().addLast(MOD_ID + "_proxy", new ProxyFrontendHandler());
                        }
                    })
                    .group(eventLoopGroup)
                    .localAddress(bindAddress)
                    .bind()
                    .syncUninterruptibly();
        } catch (Exception e) {
            logger.error("Failed to start Netty server", e);
        }
    }

    // Returns true if stopped successfully
    public void stop() {
        try {
            if (serverChannel != null) {
                serverChannel.channel().close().sync();
                serverChannel = null;
            }

            TrafficShaper.close();

            if (eventLoopGroup != null) {
                eventLoopGroup.shutdownGracefully().sync();
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted server channel", e);
        }
    }

    // -----------------------------------------------------------------------

    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
