package pl.skidam.automodpack_velocity.proxy.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import static pl.skidam.automodpack_velocity.Constants.logger;
import static pl.skidam.automodpack_velocity.proxy.TcpProxy.closeOnFlush;

public class ProxyBackendHandler extends ChannelInboundHandlerAdapter {
    private final Channel clientChannel;

    ProxyBackendHandler(Channel clientChannel) {
        this.clientChannel = clientChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.read();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!clientChannel.isActive()) {
            // Client is gone — release and let AUTO_READ consume the rest until backend closes
            ReferenceCountUtil.release(msg);
            return;
        }

        clientChannel.writeAndFlush(msg).addListener(f -> {
            if (f.isSuccess()) ctx.channel().read();
            else closeOnFlush(clientChannel);
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        closeOnFlush(clientChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("Pipeline handler ProxyBackendHandler exception: {}", cause.getMessage());
        closeOnFlush(ctx.channel());
        closeOnFlush(clientChannel);
    }
}