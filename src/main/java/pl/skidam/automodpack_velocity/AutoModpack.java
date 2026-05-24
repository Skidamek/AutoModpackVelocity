package pl.skidam.automodpack_velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bstats.velocity.Metrics;
import pl.skidam.automodpack_velocity.initializer.VelocityChannelInitializer;
import pl.skidam.automodpack_velocity.proxy.TcpProxy;
import org.slf4j.Logger;
import java.nio.file.Path;

import static pl.skidam.automodpack_velocity.Constants.logger;

@Plugin(
        id = BuildConstants.ID,
        name = BuildConstants.NAME,
        version = BuildConstants.VERSION,
        description = BuildConstants.DESCRIPTION,
        url = "https://github.com/skidamek/AutoModpack",
        authors = {"Caio Stoduto", "Skidam"},
        dependencies = {@Dependency(id = "loginphaseproxy"),})
public class AutoModpack {

    private final TcpProxy proxy = new TcpProxy();

    @Inject
    public AutoModpack(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        Constants.proxyServer = server;
        Constants.logger = logger;
        Constants.dataDirectory = dataDirectory;
        Constants.metricsFactory = metricsFactory; // bStats
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Initialize bStats
        int pluginId = 31550;
        Metrics metrics = Constants.metricsFactory.make(this, pluginId);

        long start = System.currentTimeMillis();
        logger.info("Launching AutoModpack...");

        try {
            // Load configs
            new Preload(); // TODO: updateAll(), *do it after injecting AutoModpack on ProxyServer*

            // Inject Velocity ServerChannelInitializer
            VelocityChannelInitializer.inject();

            // Start the TCP Proxy (shared or dedicated port)
            proxy.start();

            logger.info("AutoModpack launched! took {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        proxy.stop();
    }
}
