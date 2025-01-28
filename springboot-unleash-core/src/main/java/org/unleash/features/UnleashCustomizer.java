package org.unleash.features;

import io.getunleash.util.UnleashConfig;

/**
 * Callback interface that can be used to customize Unleash with a {@link UnleashConfig.Builder}.
 *
 * @author Ivan Rodriguez
 * @since 1.1.2
 */
@FunctionalInterface
public interface UnleashCustomizer {

    /**
     * Callback to customize a {@link UnleashConfig.Builder} instance.
     *
     * @param builder Unleash builder to customize
     */
    void customize(UnleashConfig.Builder builder);

}
