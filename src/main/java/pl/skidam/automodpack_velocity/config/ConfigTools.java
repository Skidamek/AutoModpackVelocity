/*
 * Adapted from AutoModpack Core at https://github.com/Skidamek/AutoModpack/blob/224a62e78abee10c0ad711e2c7b3c44488ae1b23/core/src/main/java/pl/skidam/automodpack_core/config/ConfigTools.java
 */

package pl.skidam.automodpack_velocity.config;

import com.google.gson.*;
import pl.skidam.automodpack_velocity.utils.AddressHelpers;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static pl.skidam.automodpack_velocity.Constants.*;

public class ConfigTools {

    public static Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .registerTypeAdapter(InetSocketAddress.class, new InetSocketAddressTypeAdapter())
            .create();

    private static class InetSocketAddressTypeAdapter implements JsonSerializer<InetSocketAddress>,JsonDeserializer<InetSocketAddress> {
        @Override
        public JsonElement serialize(InetSocketAddress src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getHostString() + ":" + src.getPort());
        }

        @Override
        public InetSocketAddress deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String address = json.getAsString();
            return AddressHelpers.parse(address);
        }
    }

    public static <T> T getConfigObject(Class<T> configClass) {
        T object = null;
        try {
            object = configClass.getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    public static <T> T load(Path configFile, Class<T> configClass) {
        try {
            if (!Files.isDirectory(configFile.getParent())) {
                Files.createDirectories(configFile.getParent());
            }

            if (Files.isRegularFile(configFile)) {
                String json = Files.readString(configFile);
                T obj = GSON.fromJson(json, configClass);
                if (obj == null) {
                    logger.error("Parsed object is null. Possible JSON syntax error in file: " + configFile);
                    return null;
                }

                save(configFile, obj);
                return obj;
            }
        } catch (JsonSyntaxException e) {
            logger.error("JSON syntax error while loading config! {} {}", configClass, e.getMessage());
            logger.error("This error most often happens when you e.g. forget to put a comma between fields in JSON file. Check the file: " + configFile.toAbsolutePath().normalize());
            return null;
        } catch (Exception e) {
            logger.error("Couldn't load config! " + configClass);
            e.printStackTrace();
        }

        try { // create new config
            T obj = getConfigObject(configClass);
            save(configFile, obj);
            return obj;
        } catch (Exception e) {
            logger.error("Invalid config class! " + configClass);
            e.printStackTrace();
            return null;
        }
    }

    public static void save(Path configFile, Object configObject) {

        try {
            if (!Files.isDirectory(configFile.getParent())) {
                Files.createDirectories(configFile.getParent());
            }

            Files.writeString(configFile, GSON.toJson(configObject), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            logger.error("Couldn't save config! " + configObject.getClass());
            e.printStackTrace();
        }
    }
}

