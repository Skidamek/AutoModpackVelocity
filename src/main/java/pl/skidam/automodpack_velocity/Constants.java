package pl.skidam.automodpack_velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import pl.skidam.automodpack_velocity.cache.BackendPortCache;
import pl.skidam.automodpack_velocity.config.Jsons;
import org.slf4j.Logger;

import java.nio.file.Path;

public class Constants {

    // https://github.com/Skidamek/AutoModpack/blob/224a62e78abee10c0ad711e2c7b3c44488ae1b23/core/src/main/java/pl/skidam/automodpack_core/Constants.java#L16
    public static final String MOD_ID = "automodpack";
    // https://github.com/Skidamek/AutoModpack/blob/224a62e78abee10c0ad711e2c7b3c44488ae1b23/core/src/main/java/pl/skidam/automodpack_core/protocol/NetUtils.java#L29
    public static final int MAGIC_AMMH = 0x414D4D48;

    // Login plugin message channel IDs - Must match LoginNetworkingIDs enum names
    // https://github.com/Skidamek/AutoModpack/blob/224a62e78abee10c0ad711e2c7b3c44488ae1b23/src/main/java/pl/skidam/automodpack/networking/LoginNetworkingIDs.java#L11
    public static final String LOGIN_CHANNEL_DATA = MOD_ID + ":data";

    // Path constants
    public static String proxyConfigFilename = "automodpack-proxy.json";
    public static String backendPortCacheFilename = "automodpack-backend-port-cache.json";

    // Globals
    public static ProxyServer proxyServer;
    public static Logger logger;
    public static Path dataDirectory;
    public static Jsons.ProxyConfigFieldsV2 proxyConfig;
    public static BackendPortCache backendPortCache;
}
