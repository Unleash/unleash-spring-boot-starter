package org.unleash.features.config;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.UnleashContextProvider;
import io.getunleash.event.NoOpSubscriber;
import io.getunleash.event.UnleashSubscriber;
import io.getunleash.repository.OkHttpFeatureFetcher;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.unleash.features.aop.UnleashContextThreadLocal;
import org.unleash.features.aop.Utils;
import org.unleash.features.autoconfigure.UnleashProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@EnableConfigurationProperties(UnleashProperties.class)
@AutoConfiguration
@ComponentScan("org.unleash.features.aop")
public class UnleashAutoConfiguration {
    @Autowired(required = false)
    private Map<String, ? extends Strategy> strategyMap;

    @Bean
    @ConditionalOnMissingBean
    public UnleashContextProvider unleashContextProvider(final UnleashProperties unleashProperties) {
        return () -> UnleashContext.builder()
                .appName(unleashProperties.getAppName())
                .environment(unleashProperties.getEnvironment())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public UnleashSubscriber unleashSubscriber() {
        return new NoOpSubscriber();
    }

    @Bean
    @ConditionalOnMissingBean
    public UnleashConfig unleashConfig(final UnleashProperties unleashProperties,
                                       UnleashContextProvider unleashContextProvider,
                                       UnleashSubscriber unleashSubscriber,
                                       ObjectProvider<UnleashCustomizer> customizers
    ) {
        final var provider = getUnleashContextProviderWithThreadLocalSupport(unleashContextProvider);
        final var builder = UnleashConfig
                .builder()
                .unleashContextProvider(provider)
                .appName(unleashProperties.getAppName())
                .environment(unleashProperties.getEnvironment())
                .unleashAPI(unleashProperties.getApiUrl())
                .fetchTogglesConnectTimeout(unleashProperties.getFetchTogglesConnectTimeout())
                .fetchTogglesReadTimeout(unleashProperties.getFetchTogglesReadTimeout())
                .fetchTogglesInterval(unleashProperties.getFetchTogglesInterval().getSeconds())
                .sendMetricsInterval(unleashProperties.getSendMetricsInterval().getSeconds())
                .sendMetricsConnectTimeout(unleashProperties.getSendMetricsConnectTimeout())
                .sendMetricsReadTimeout(unleashProperties.getSendMetricsReadTimeout())
                .customHttpHeader("Authorization", unleashProperties.getApiToken())
                .projectName(unleashProperties.getProjectName())
                .subscriber(unleashSubscriber)
                .synchronousFetchOnInitialisation(unleashProperties.isSynchronousFetchOnInitialisation())
                .instanceId(StringUtils.hasText(unleashProperties.getInstanceId()) ? unleashProperties.getInstanceId() :
                        UUID.randomUUID().toString());

        setDisableMetrics(builder, unleashProperties);
        setHttpFetcherInBuilder(builder, unleashProperties);
        setProxyAuthenticationByJvmProps(builder, unleashProperties);
        setCustomHeaderProvider(builder, unleashProperties);

        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public Unleash unleash(UnleashConfig config) {
        return !CollectionUtils.isEmpty(strategyMap) ? new DefaultUnleash(config, strategyMap.values().toArray(new Strategy[0])) :
                new DefaultUnleash(config);
    }

    /**
     * Method always wraps the created UnleashContextProvider with threadLocal support.
     */
    @NotNull
    @SuppressWarnings("ConstantConditions")
    private UnleashContextProvider getUnleashContextProviderWithThreadLocalSupport(UnleashContextProvider unleashContextProvider) {
        return () -> {
            final Map<String, String> threadLocalContextMap = UnleashContextThreadLocal.getContextMap();

            if (CollectionUtils.isEmpty(threadLocalContextMap)) {
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

    private void setHttpFetcherInBuilder(UnleashConfig.Builder builder, UnleashProperties unleashProperties) {
        if (unleashProperties.getHttpFetcher() != UnleashProperties.HttpFetcher.HTTP_URL_CONNECTION_FETCHER) {
            builder.unleashFeatureFetcherFactory(OkHttpFeatureFetcher::new);
        }
    }

    private void setProxyAuthenticationByJvmProps(UnleashConfig.Builder builder, UnleashProperties properties) {
        if (properties.isProxyAuthenticationByJvmProperties()) {
            builder.enableProxyAuthenticationByJvmProperties();
        }
    }


    private void setCustomHeaderProvider(UnleashConfig.Builder builder, UnleashProperties properties) {
        if (!CollectionUtils.isEmpty(properties.getCustomHttpHeadersProvider())) {
            builder.customHttpHeadersProvider(() -> properties.getCustomHttpHeadersProvider().stream()
                    .collect(Collectors.toMap(UnleashProperties.CustomHeader::getName, UnleashProperties.CustomHeader::getValue)));
        }
    }


    private void setDisableMetrics(UnleashConfig.Builder builder, UnleashProperties properties) {
        if (properties.isDisableMetrics()) {
            builder.disableMetrics();
        }
    }
}
