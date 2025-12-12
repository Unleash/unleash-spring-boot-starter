package org.unleash.features.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.unleash.features.autoconfigure.UnleashProperties.PREFIX;

import io.getunleash.*;
import io.getunleash.event.NoOpSubscriber;
import io.getunleash.event.UnleashSubscriber;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import io.getunleash.util.UnleashScheduledExecutorImpl;
import java.net.URI;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.unleash.features.autoconfigure.UnleashProperties;

/**
 * Tests for {@link UnleashAutoConfiguration}.
 *
 * @author Ivan Rodriguez
 */
class UnleashAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(UnleashAutoConfiguration.class));

    final String[] requiredProperties = {PREFIX + ".appName=Foo", PREFIX + ".apiUrl=https://example.com:4242/api"};

    @Test
    void shouldSupplyDefaultBeans() {
        this.contextRunner
                .withPropertyValues(requiredProperties)
                .run((context) -> {
                    assertThat(context).hasSingleBean(UnleashContextProvider.class);
                    assertThat(context).hasSingleBean(UnleashSubscriber.class);
                    assertThat(context).hasSingleBean(Unleash.class);
                    assertThat(context).hasSingleBean(UnleashConfig.class);
                });
    }

    @Test
    void shouldConfigureDefaultUnleash() {
        this.contextRunner
                .withUserConfiguration(DefaultConfiguration.class)
                .withPropertyValues(requiredProperties)
                .run((context) -> {
                    Unleash unleash = context.getBean(Unleash.class);
                    assertThat(unleash)
                            .extracting("config")
                            .asInstanceOf(InstanceOfAssertFactories.type(UnleashConfig.class))
                            .hasFieldOrPropertyWithValue("appName", "Foo")
                            .hasFieldOrPropertyWithValue("unleashScheduledExecutor", UnleashScheduledExecutorImpl.getInstance())
                            .hasFieldOrPropertyWithValue("unleashAPI", URI.create("https://example.com:4242/api"));

                    assertThat(unleash)
                            .extracting("config.unleashSubscriber")
                            .isInstanceOf(NoOpSubscriber.class);
                });
    }

    @Test
    void shouldConfigureUnleashWithCustomizer() {
        this.contextRunner
                .withUserConfiguration(CustomizerConfiguration.class)
                .withPropertyValues(requiredProperties)
                .run((context) -> {
                    Unleash unleash = context.getBean(Unleash.class);
                    assertThat(unleash)
                            .extracting("config.fallbackStrategy")
                            .hasFieldOrPropertyWithValue("name", "a_fallback_for Foo");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class DefaultConfiguration {

        @Bean
        public Unleash unleash(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") UnleashConfig config) {
            return new DefaultUnleash(config,
                    mock(EngineProxy.class)
            );
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomizerConfiguration extends DefaultConfiguration {

        @Bean
        public UnleashCustomizer unleashFallbackCustomizer(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") UnleashProperties properties) {
            return builder -> builder.fallbackStrategy(new Strategy() {

                @Override
                public String getName() {
                    return "a_fallback_for " + properties.getAppName();
                }

                @Override
                public boolean isEnabled(Map<String, String> map, UnleashContext unleashContext) {
                    return false;
                }
            });
        }

    }
}
