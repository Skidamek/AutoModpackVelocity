/*
 * Adapted from AutoModpack Core at https://github.com/Skidamek/AutoModpack/blob/224a62e78abee10c0ad711e2c7b3c44488ae1b23/core/src/main/java/pl/skidam/automodpack_core/protocol/netty/TrafficShaper.java
 */

package pl.skidam.automodpack_velocity.protocol.netty;

import io.netty.handler.traffic.GlobalTrafficShapingHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static pl.skidam.automodpack_velocity.Constants.logger;

public class TrafficShaper {

    private final GlobalTrafficShapingHandler trafficShapingHandler;
    private ScheduledExecutorService executor = null;
    public static TrafficShaper trafficShaper;

    public enum TrafficShaperType {
        READ, WRITE
    }

    public TrafficShaper(ScheduledExecutorService executor, int configBandwidthLimit, TrafficShaperType type) {
        close(); // There can be only one traffic shaper instance, close previous one if exists
        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor();
            this.executor = executor;
        }

        long bandwidthLimit = configBandwidthLimit * 1024L * 1024L / 8L;
        if (bandwidthLimit < 0) {
            bandwidthLimit = 0;
            logger.warn("Invalid configured bandwidth limit ({} Mbps). Setting effective limit to 0 (unlimited).", configBandwidthLimit);
        } else if (bandwidthLimit > 0) {
            logger.info("Setting bandwidth limit to {} Mbps.", configBandwidthLimit);
        }

        if (type == TrafficShaperType.READ) {
            this.trafficShapingHandler = new GlobalTrafficShapingHandler(executor, 0, bandwidthLimit);
        } else {
            this.trafficShapingHandler = new GlobalTrafficShapingHandler(executor, bandwidthLimit, 0);
        }

        TrafficShaper.trafficShaper = this;
    }

    public GlobalTrafficShapingHandler getTrafficShapingHandler() {
        return this.trafficShapingHandler;
    }

    public ScheduledExecutorService getExecutor() {
        return this.executor;
    }

    public static void close() {
        if (TrafficShaper.trafficShaper != null) {
            TrafficShaper.trafficShaper.getTrafficShapingHandler().release();
            if (TrafficShaper.trafficShaper.getExecutor() != null) {
                TrafficShaper.trafficShaper.getExecutor().shutdown();
            }
            TrafficShaper.trafficShaper = null;
        }
    }
}
