package org.unleash.features;

import java.util.function.Supplier;

public interface UnleashContextPreProcessor {
    default <T> Supplier<T> preProcess(final Supplier<T> supplier) {
        return () -> {
            try {
                process();
                return supplier.get();
            } finally {
                cleanup();
            }
        };
    }

    void process();
    void cleanup();
}
