package org.unleash.features.aop;

import io.getunleash.UnleashContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UnleashContextThreadLocal {
    private static final ThreadLocal<ConcurrentHashMap<String, String>> UNLEASH_CONTEXT_BUILDER_THREAD_LOCAL = ThreadLocal.withInitial(ConcurrentHashMap::new);

    public static void addContextProperty(final String name, final String value) {
        final String previousValue = UNLEASH_CONTEXT_BUILDER_THREAD_LOCAL.get().putIfAbsent(name, value);

        if(previousValue != null) {
            throw new IllegalArgumentException(String.format("Context name %s already used", name));
        }
    }

    public static UnleashContext get() {
        final Map<String, String> contextMap = UNLEASH_CONTEXT_BUILDER_THREAD_LOCAL.get();
        final UnleashContext.Builder builder = UnleashContext.builder();

        if(!contextMap.isEmpty()) {
            contextMap.forEach((name, value) -> setContextBuilderProperty(builder, name, value));
        }

        return builder.build();
    }

    @SuppressWarnings("EnhancedSwitchMigration")
    private static void setContextBuilderProperty(final UnleashContext.Builder builder, final String name, final String value) {
        switch (name) {
            case "environment":
                builder.environment(value);
                break;
            case "appName":
                builder.appName(value);
                break;
            case "userId":
                builder.userId(value);
                break;
            case "sessionId":
                builder.sessionId(value);
                break;
            case "remoteAddress":
                builder.remoteAddress(value);
                break;
            default:
                builder.addProperty(name, value);
        }
    }

    public static void unset() {
        UNLEASH_CONTEXT_BUILDER_THREAD_LOCAL.remove();
    }
}
