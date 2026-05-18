/*
 * Adapted from AutoModpack Core at https://github.com/Skidamek/AutoModpack/blob/224a62e78abee10c0ad711e2c7b3c44488ae1b23/core/src/main/java/pl/skidam/automodpack_core/utils/AddressHelpers.java
 */

package pl.skidam.automodpack_velocity.utils;

import java.net.*;

import static pl.skidam.automodpack_velocity.Constants.logger;

public class AddressHelpers {

    public static InetSocketAddress format(String host, int port) {
        if (host.endsWith(".")) { // It breaks our checks and looks ugly, but its a valid domain...
            host = host.substring(0, host.length() - 1);
        }
        host = host.toLowerCase(); // #382
        return InetSocketAddress.createUnresolved(host, port);
    }

    public static InetSocketAddress parse(String address) {
        if (address == null) return null;
        InetSocketAddress socketAddress = null;
        try {
            int portIndex = address.lastIndexOf(':');
            if (portIndex != -1) {
                String host = address.substring(0, portIndex);
                String port = address.substring(portIndex + 1);
                if (port.matches("\\d+")) {
                    socketAddress = format(host, Integer.parseInt(port));
                }
            }
            if (socketAddress == null) {
                socketAddress = format(address, 0);
            }
        } catch (Exception e) {
            logger.error("Error while parsing address", e);
        }

        return socketAddress;
    }
}
