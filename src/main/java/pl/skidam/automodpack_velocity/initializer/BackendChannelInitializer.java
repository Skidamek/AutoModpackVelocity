package pl.skidam.automodpack_velocity.initializer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import pl.skidam.automodpack_velocity.proxy.handler.AutoModpackDataHandler;

import static pl.skidam.automodpack_velocity.Constants.MOD_ID;
import static pl.skidam.automodpack_velocity.initializer.VelocityChannelInitializer.method;

public class BackendChannelInitializer extends ChannelInitializer<Channel> {
    private final ChannelInitializer<Channel> initializer;

    public BackendChannelInitializer(ChannelInitializer<Channel> initializer) {
        this.initializer = initializer;
    }

    @Override
    protected void initChannel(Channel ch) {
        try {
            method(initializer.getClass(), "initChannel", Channel.class).invoke(initializer, ch);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke initChannel", e);
        }

        // Modify automodpack:data
        ch.pipeline().addBefore("minecraft-encoder", MOD_ID + "_data", new AutoModpackDataHandler());
    }
}