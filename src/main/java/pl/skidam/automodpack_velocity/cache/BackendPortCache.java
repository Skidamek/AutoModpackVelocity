package pl.skidam.automodpack_velocity.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

import static pl.skidam.automodpack_velocity.Constants.logger;

/**
 * Thread-safe cache that maps a backend Minecraft server address ({@code ip:port})
 * to the port of its AutoModpack NettyServer (the {@code port} field from the
 * {@code automodpack:data} login plugin message).
 *
 * <p>The cache is persisted as a simple JSON object so it survives proxy restarts.
 * Entries are populated lazily by {@link pl.skidam.automodpack_velocity.proxy.handler.AutoModpackDataHandler}
 * whenever a player logs in and the backend sends its {@code DataPacket}.
 */
public class BackendPortCache {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Type MAP_TYPE = new TypeToken<ConcurrentHashMap<String, Integer>>() {}.getType();

    private final Path cacheFile;
    private final ConcurrentHashMap<String, Integer> cache;

    public BackendPortCache(Path cacheFile) {
        this.cacheFile = cacheFile;
        this.cache = load();
    }

    /**
     * Stores (or updates) the AM server port for the given backend address.
     *
     * @param address backend Minecraft server
     * @param amPort  the AutoModpack NettyServer port reported by that backend
     */
    public void put(InetSocketAddress address, int amPort) {
        String backendAddress = getBackendAddress(address);

        Integer previous = cache.put(backendAddress, amPort);
        if (previous == null || previous != amPort) {
            logger.debug("[BackendPortCache] {} -> {} (was {})", backendAddress, amPort, previous);
            save();
        }
    }

    /**
     * Returns the cached AM server port for the given backend address,
     * or {@code null} if no entry exists.
     */
    public Integer get(InetSocketAddress address) {
        String backendAddress = getBackendAddress(address);

        Integer amPort = cache.get(backendAddress);
        if (amPort == null) {
            logger.debug("[BackendPortCache] No entry found for {}", backendAddress);
        }

        return amPort;
    }

    private String getBackendAddress(InetSocketAddress address) {
        return address.getHostString() + ":" + address.getPort();
    }

    // -----------------------------------------------------------------------

    private ConcurrentHashMap<String, Integer> load() {
        try {
            if (Files.isRegularFile(cacheFile)) {
                String json = Files.readString(cacheFile);
                ConcurrentHashMap<String, Integer> map = GSON.fromJson(json, MAP_TYPE);
                if (map != null) {
                    logger.debug("[BackendPortCache] Loaded {} entries from {}", map.size(), cacheFile);
                    return map;
                }
            }
        } catch (Exception e) {
            logger.warn("[BackendPortCache] Failed to load cache from {}: {}", cacheFile, e.getMessage());
        }
        return new ConcurrentHashMap<>();
    }

    private void save() {
        try {
            if (!Files.isDirectory(cacheFile.getParent())) {
                Files.createDirectories(cacheFile.getParent());
            }
            Files.writeString(cacheFile, GSON.toJson(cache),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.warn("[BackendPortCache] Failed to save cache to {}: {}", cacheFile, e.getMessage());
        }
    }
}
