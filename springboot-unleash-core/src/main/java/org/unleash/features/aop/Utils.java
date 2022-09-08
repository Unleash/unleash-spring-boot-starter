package org.unleash.features.aop;

import io.getunleash.UnleashContext;

public final class Utils {
    @SuppressWarnings("EnhancedSwitchMigration")
    // Not using enhanced switch to keep Java 11 compatibility
    public static void setContextBuilderProperty(final UnleashContext.Builder builder, final String name, final String value) {
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
}
