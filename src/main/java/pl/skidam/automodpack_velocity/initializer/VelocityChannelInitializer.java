package pl.skidam.automodpack_velocity.initializer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import pl.skidam.automodpack_velocity.Constants;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static pl.skidam.automodpack_velocity.Constants.logger;
import static pl.skidam.automodpack_velocity.Constants.proxyConfig;

public class VelocityChannelInitializer {

    private static final String COMPATIBILITY_ERROR = "Unable to inject AutoModpack into Velocity. Velocity's "
            + "private network initializer layout is not compatible with this plugin version.";

    public static void inject() {
        try {
            // com.velocitypowered.proxy.network.ConnectionManager
            Object connectionManager = readRequiredField(Constants.proxyServer, "cm", "proxy.cm");

            // com.velocitypowered.proxy.network.ServerChannelInitializerHolder
            Object serverHolder = readRequiredField(
                    connectionManager,
                    "serverChannelInitializer",
                    "proxy.cm.serverChannelInitializer");
            InitializerInjection frontendInjection = prepareInitializerInjection(
                    serverHolder,
                    "proxy.cm.serverChannelInitializer.initializer",
                    InitializerSide.FRONTEND);

            // com.velocitypowered.proxy.network.BackendChannelInitializerHolder
            Object backendHolder = readRequiredField(
                    connectionManager,
                    "backendChannelInitializer",
                    "proxy.cm.backendChannelInitializer");
            InitializerInjection backendInjection = prepareInitializerInjection(
                    backendHolder,
                    "proxy.cm.backendChannelInitializer.initializer",
                    InitializerSide.BACKEND);

            // Install our ChannelInitializers to inject the FrontendInterceptor/BackendInterceptor into the pipeline
            backendInjection.install();
            if (proxyConfig.proxyHost && proxyConfig.port == -1)
                frontendInjection.install();
            
            logger.info("Velocity network initializers injected successfully.");
        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
            throw new VelocityCompatibilityException(COMPATIBILITY_ERROR + " " + e.getMessage(), e);
        } catch (Exception e) {
            throw new VelocityCompatibilityException("Failed to inject AutoModpack Velocity channel initializers.", e);
        }
    }

    private static Object readRequiredField(Object instance, String name, String path) throws ReflectiveOperationException {
        if (instance == null) {
            throw new IllegalArgumentException(path + " owner is null");
        }

        Field field = field(instance, name);
        Object value = field.get(instance);
        if (value == null) {
            throw new IllegalArgumentException(path + " is null");
        }

        logger.debug("Velocity compatibility check: {} resolved to {}", path, value.getClass().getName());
        return value;
    }

    private static InitializerInjection prepareInitializerInjection(Object holder, String path, InitializerSide side)
            throws ReflectiveOperationException {
        Field field = field(holder, "initializer");
        Object currentInitializer = field.get(holder);
        ChannelInitializer<Channel> initializer = channelInitializer(currentInitializer, path);

        if (!field.getType().isAssignableFrom(side.wrapperClass())) {
            throw new IllegalArgumentException(path + " field type is " + field.getType().getName()
                    + ", cannot assign " + side.wrapperClass().getName());
        }

        return new InitializerInjection(holder, field, initializer, side, path);
    }

    @SuppressWarnings("unchecked")
    private static ChannelInitializer<Channel> channelInitializer(Object value, String path) {
        if (value == null) {
            throw new IllegalArgumentException(path + " is null");
        }

        if (!(value instanceof ChannelInitializer<?> initializer)) {
            throw new IllegalArgumentException(path + " is " + value.getClass().getName()
                    + ", expected " + ChannelInitializer.class.getName());
        }

        return (ChannelInitializer<Channel>) initializer;
    }

    private static Field field(Object instance, String name) throws NoSuchFieldException {
        return field(instance.getClass(), name);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + name + "' not found in " + type.getName()
                + " or its superclasses");
    }

    protected static Method method(Class<?> type, String name, Class<?> arg) throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, arg);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException("Method '" + name + "(" + arg.getName() + ")' not found in "
                + type.getName() + " or its superclasses");
    }

    public static final class VelocityCompatibilityException extends RuntimeException {
        public VelocityCompatibilityException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record InitializerInjection(
            Object holder,
            Field field,
            ChannelInitializer<Channel> initializer,
            InitializerSide side,
            String path) {

        void install() throws IllegalAccessException {
            field.set(holder, side.wrap(initializer));
            logger.debug("Installed {} Velocity channel initializer wrapper around {}.",
                    side.logName,
                    initializer.getClass().getName());
        }
    }

    private enum InitializerSide {
        FRONTEND("frontend") {
            @Override
            ChannelInitializer<Channel> wrap(ChannelInitializer<Channel> initializer) {
                return new FrontendChannelInitializer(initializer);
            }

            @Override
            Class<?> wrapperClass() {
                return FrontendChannelInitializer.class;
            }
        },
        BACKEND("backend") {
            @Override
            ChannelInitializer<Channel> wrap(ChannelInitializer<Channel> initializer) {
                return new BackendChannelInitializer(initializer);
            }

            @Override
            Class<?> wrapperClass() {
                return BackendChannelInitializer.class;
            }
        };

        private final String logName;

        InitializerSide(String logName) {
            this.logName = logName;
        }

        abstract ChannelInitializer<Channel> wrap(ChannelInitializer<Channel> initializer);

        abstract Class<?> wrapperClass();
    }
}
