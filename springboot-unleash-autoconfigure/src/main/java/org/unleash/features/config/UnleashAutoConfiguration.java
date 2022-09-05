package org.unleash.features.config;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.repository.OkHttpFeatureFetcher;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.unleash.features.autoconfigure.UnleashProperties;

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
    public Unleash unleash(final UnleashProperties unleashProperties) {
        final UnleashConfig unleashConfig = httpFetcherFunc.apply(UnleashConfig
                        .builder()
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

    private static UnleashConfig.Builder setHttpFetcherInBuilder(UnleashConfig.Builder builder, UnleashProperties.HttpFetcher fetcher) {
        if (fetcher == UnleashProperties.HttpFetcher.HTTP_URL_CONNECTION_FETCHER) {
            return builder;
        } else {
            return builder.unleashFeatureFetcherFactory(OkHttpFeatureFetcher::new);
        }
    }
}
