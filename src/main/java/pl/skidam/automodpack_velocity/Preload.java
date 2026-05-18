package pl.skidam.automodpack_velocity;

import static pl.skidam.automodpack_velocity.Constants.logger;
import static pl.skidam.automodpack_velocity.Constants.*;

import java.nio.file.Path;
import pl.skidam.automodpack_velocity.config.ConfigTools;
import pl.skidam.automodpack_velocity.cache.BackendPortCache;
import pl.skidam.automodpack_velocity.config.Jsons;

public class Preload {

    public Preload() {
        try {
            loadConfigs();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadConfigs() {
        long startTime = System.currentTimeMillis();

        // Load proxy config
        Path proxyConfigFile = dataDirectory.resolve(proxyConfigFilename);
        proxyConfig = ConfigTools.load(
            proxyConfigFile,
            Jsons.ProxyConfigFieldsV2.class
        );

        // Load backend port cache
        Path cacheFile = dataDirectory.resolve(backendPortCacheFilename);
        backendPortCache = new BackendPortCache(cacheFile);

        logger.info(
            "Loaded config! took {}ms",
            System.currentTimeMillis() - startTime
        );
    }
}
