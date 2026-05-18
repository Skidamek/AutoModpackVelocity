package pl.skidam.automodpack_velocity.proxy.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessagePacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

import static pl.skidam.automodpack_velocity.Constants.logger;
import static pl.skidam.automodpack_velocity.Constants.*;
import static pl.skidam.automodpack_velocity.utils.VelocityHelper.rewriteDataPacket;

public class AutoModpackDataHandler extends ChannelDuplexHandler {

    // -------------------------------------------------------------------------
    // [B -> V]  inbound (Backend → Velocity)
    // -------------------------------------------------------------------------

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("[DataHandler][B->V] {}", msg);

        switch (msg) {
            case LoginPluginMessagePacket packet -> {
                if (!packet.getChannel().equals(LOGIN_CHANNEL_DATA)) {
                    break; // Send packet
                }

                // Parse original JSON string from the ByteBuf (single parse)
                ByteBuf originalBuf = packet.content().duplicate();
                String originalStr = ProtocolUtils.readString(originalBuf);
                JsonObject obj = JsonParser.parseString(originalStr).getAsJsonObject();

                // Cache the backend's AM server port keyed by backend address
                cacheBackendPort(ctx, obj);

                // Modify the parsed JSON to set the proxy serverAddress and port
                String modifiedStr = rewriteDataPacket(obj, proxyConfig.bindAddress, proxyConfig.bindPort);
                logger.debug("[DataHandler] Modified Data {} -> {}",
                        originalStr, modifiedStr);

                // Replace the original content with our modified one
                ByteBuf modifiedBuf = ctx.alloc().buffer();
                ProtocolUtils.writeString(modifiedBuf, modifiedStr);
                packet.replace(modifiedBuf);

                // Remove handler from pipeline
                ctx.pipeline().remove(this);

                break; // Send packet
            }
            default -> {} // Send packet
        }

        // Send packet
        super.channelRead(ctx, msg);
    }

    private void cacheBackendPort(ChannelHandlerContext ctx, JsonObject obj) {
        try {
            if (!obj.has("port")) {
                return;
            }

            int amPort = obj.get("port").getAsInt();

            // The channel's remote address is the backend Minecraft server
            InetSocketAddress backendAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            if (backendAddress == null) {
                return;
            }

            backendPortCache.put(backendAddress, amPort);
        } catch (Exception e) {
            logger.warn("[DataHandler] Failed to cache backend port: {}", e.getMessage());
        }
    }
}