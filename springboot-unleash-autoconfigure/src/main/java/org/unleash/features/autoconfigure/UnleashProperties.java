package org.unleash.features.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.unleash.features.autoconfigure.UnleashProperties.PREFIX;

@ConfigurationProperties(prefix = PREFIX)
public class UnleashProperties {
    private String appName;
    private String instanceId;
    private String environment;
    private String apiUrl;
    private String apiToken;

    public static final String PREFIX = "io.getunleash";

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }
}
