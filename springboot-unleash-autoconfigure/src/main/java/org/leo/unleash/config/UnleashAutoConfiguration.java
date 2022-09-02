package org.leo.unleash.config;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import org.leo.unleash.aop.FeatureAdvisor;
import org.leo.unleash.aop.FeatureProxyAdvisor;
import org.leo.unleash.autoconfigure.UnleashProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Configuration
@EnableConfigurationProperties(UnleashProperties.class)
public class UnleashAutoConfiguration {
    @Autowired(required = false)
    private Map<String, ? extends Strategy> strategyMap;

    @Bean
    @ConditionalOnProperty(prefix = "io.unleash", value = {"appName", "environment", "apiUrl", "apiToken"})
    public Unleash unleash(final UnleashProperties unleashProperties) {
        final UnleashConfig unleashConfig = UnleashConfig
                .builder()
                .appName(unleashProperties.getAppName())
                .environment(unleashProperties.getEnvironment())
                .unleashAPI(unleashProperties.getApiUrl())
                .customHttpHeader("Authorization", unleashProperties.getApiToken())
                .instanceId(!StringUtils.hasText(unleashProperties.getInstanceId()) ? unleashProperties.getInstanceId() : UUID.randomUUID().toString())
                .build();

        return !CollectionUtils.isEmpty(strategyMap) ? new DefaultUnleash(unleashConfig, strategyMap.values().toArray(new Strategy[0])) :
                new DefaultUnleash(unleashConfig);
    }

    @Bean(name = "features.advisor")
    @ConditionalOnBean(Unleash.class)
    public FeatureAdvisor featureAdvisor(final Unleash unleash, final ApplicationContext applicationContext) {
        return new FeatureAdvisor(unleash, applicationContext);
    }

    @Bean(name = "features.autoproxy")
    @ConditionalOnBean({Unleash.class, FeatureAdvisor.class})
    public FeatureProxyAdvisor featureProxyAdvisor(final ApplicationContext applicationContext) {
        return new FeatureProxyAdvisor(applicationContext);
    }
}
