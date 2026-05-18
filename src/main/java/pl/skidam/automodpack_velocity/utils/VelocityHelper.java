package pl.skidam.automodpack_velocity.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import pl.skidam.automodpack_velocity.Constants;

import java.util.ArrayList;
import java.util.List;

public class VelocityHelper {
    /**
     * Given a hostname, determine the list of backend servers Velocity will attempt to connect them to, in order.
     * <a href="https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/connection/util/ServerListPingHandler.java#L174">...</a>
     */
    public static List<String> getServersToTry(String hostname) {
        List<String> serversToTry = Constants.proxyServer.getConfiguration().getForcedHosts()
                .getOrDefault(hostname, Constants.proxyServer.getConfiguration().getAttemptConnectionOrder());

        // Return a copy
        return new ArrayList<>(serversToTry);
    }

    /**
     * Rewrites the {@code address}, {@code port}, and {@code requiresMagic} fields
     * of a DataPacket JSON so the client connects to the Velocity AM proxy instead
     * of directly to the backend.
     */
    public static String rewriteDataPacket(JsonObject obj, String address, int port) {
        obj.addProperty("address", address);
        obj.addProperty("port", port);
        obj.addProperty("requiresMagic", true); // proxy reads AMMH from client for hostname-based routing

        return new Gson().toJson(obj);
    }
}