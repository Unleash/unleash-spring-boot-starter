package org.unleash.features.aop;

import io.getunleash.UnleashContext;

public class UnleashContextThreadLocal {
    private static final ThreadLocal<UnleashContext> UNLEASH_CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    public static void set(final UnleashContext unleashContext) {
        UNLEASH_CONTEXT_THREAD_LOCAL.set(unleashContext);
    }

    public static UnleashContext get() {
        return UNLEASH_CONTEXT_THREAD_LOCAL.get();
    }

    public static void unset() {
        UNLEASH_CONTEXT_THREAD_LOCAL.remove();
    }
}
