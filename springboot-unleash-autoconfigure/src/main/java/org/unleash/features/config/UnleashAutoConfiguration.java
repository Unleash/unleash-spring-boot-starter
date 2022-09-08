package org.unleash.features.config;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.UnleashContextProvider;
import io.getunleash.repository.OkHttpFeatureFetcher;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.unleash.features.aop.UnleashContextThreadLocal;
import org.unleash.features.aop.Utils;
import org.unleash.features.autoconfigure.UnleashProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Configuration
@EnableConfigurationProperties(UnleashProperties.class)
@ComponentScan("org.unleash.features.aop")
public class UnleashAutoConfiguration {
    @Autowired(required = false)
    private Map<String, ? extends Strategy> strategyMap;

    private final BiFunction<UnleashConfig.Builder, UnleashProperties.HttpFetcher, UnleashConfig.Builder> httpFetcherFunc =
            UnleashAutoConfiguration::setHttpFetcherInBuilder;

    @Bean
    @ConditionalOnMissingBean
    public UnleashContextProvider unleashContextProvider(final UnleashProperties unleashProperties) {
        return () -> UnleashContext.builder()
                .appName(unleashProperties.getAppName())
                .environment(unleashProperties.getEnvironment())
                .build();
    }

    @Bean
    public Unleash unleash(final UnleashProperties unleashProperties, UnleashContextProvider unleashContextProvider) {
        final UnleashContextProvider provider = getUnleashContextProviderWithThreadLocalSupport(unleashContextProvider);
        final UnleashConfig unleashConfig = httpFetcherFunc.apply(UnleashConfig
                        .builder()
                        .unleashContextProvider(provider)
                        .appName(unleashProperties.getAppName())
                        .environment(unleashProperties.getEnvironment())
                        .unleashAPI(unleashProperties.getApiUrl())
                        .customHttpHeader("Authorization", unleashProperties.getApiToken())
                        .instanceId(!StringUtils.hasText(unleashProperties.getInstanceId()) ? unleashProperties.getInstanceId() :
                                UUID.randomUUID().toString()), unleashProperties.getHttpFetcher())
                .build();

        return !CollectionUtils.isEmpty(strategyMap) ? new DefaultUnleash(unleashConfig, strategyMap.values().toArray(new Strategy[0])) :
                new DefaultUnleash(unleashConfig);
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    /**
     * Method always wraps the created UnleashContextProvider with threadLocal support.
     */
    private UnleashContextProvider getUnleashContextProviderWithThreadLocalSupport(UnleashContextProvider unleashContextProvider) {
        return () -> {
            final Map<String, String> threadLocalContextMap = UnleashContextThreadLocal.getContextMap();

            if(CollectionUtils.isEmpty(threadLocalContextMap)) {
                return unleashContextProvider.getContext();
            } else {
                final var context = unleashContextProvider.getContext();
                final var builder = UnleashContext.builder();
                final var currentContextMap = new HashMap<>(context.getProperties() != null ? context.getProperties() : Collections.emptyMap());

                currentContextMap.putAll(threadLocalContextMap);

                context.getAppName().ifPresent(builder::appName);
                context.getEnvironment().ifPresent(builder::environment);
                context.getCurrentTime().ifPresent(builder::currentTime);
                context.getRemoteAddress().ifPresent(builder::remoteAddress);
                context.getSessionId().ifPresent(builder::sessionId);

                currentContextMap.forEach((key, value) -> Utils.setContextBuilderProperty(builder, key, value));

                return builder.build();
            }
        };
    }

    private static UnleashConfig.Builder setHttpFetcherInBuilder(UnleashConfig.Builder builder, UnleashProperties.HttpFetcher fetcher) {
        if (fetcher == UnleashProperties.HttpFetcher.HTTP_URL_CONNECTION_FETCHER) {
            return builder;
        } else {
            return builder.unleashFeatureFetcherFactory(OkHttpFeatureFetcher::new);
        }
    }
}
