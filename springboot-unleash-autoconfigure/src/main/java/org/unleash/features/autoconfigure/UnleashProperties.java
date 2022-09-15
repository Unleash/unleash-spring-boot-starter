package org.unleash.features.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

import static org.unleash.features.autoconfigure.UnleashProperties.PREFIX;

@ConfigurationProperties(prefix = PREFIX)
public class UnleashProperties {
    private String appName;
    private String instanceId;
    private String environment;
    private String apiUrl;
    private String apiToken;
    private String projectName;
    private boolean disableMetrics = false;
    private Duration fetchTogglesInterval = Duration.ofSeconds(10);
    private Duration fetchTogglesConnectTimeout = Duration.ofSeconds(10);
    private Duration fetchTogglesReadTimeout = Duration.ofSeconds(10);
    private Duration sendMetricsInterval = Duration.ofSeconds(10);
    private Duration sendMetricsConnectTimeout = Duration.ofSeconds(10);
    private Duration sendMetricsReadTimeout = Duration.ofSeconds(10);
    private HttpFetcher httpFetcher = HttpFetcher.HTTP_URL_CONNECTION_FETCHER;
    private boolean synchronousFetchOnInitialisation = false;

    private boolean proxyAuthenticationByJvmProperties = false;

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

    public HttpFetcher getHttpFetcher() {
        return httpFetcher;
    }

    public void setHttpFetcher(HttpFetcher httpFetcher) {
        this.httpFetcher = httpFetcher;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Duration getFetchTogglesInterval() {
        return fetchTogglesInterval;
    }

    public void setFetchTogglesInterval(Duration fetchTogglesInterval) {
        this.fetchTogglesInterval = fetchTogglesInterval;
    }

    public boolean isDisableMetrics() {
        return disableMetrics;
    }

    public void setDisableMetrics(boolean disableMetrics) {
        this.disableMetrics = disableMetrics;
    }

    public Duration getSendMetricsInterval() {
        return sendMetricsInterval;
    }

    public void setSendMetricsInterval(Duration sendMetricsInterval) {
        this.sendMetricsInterval = sendMetricsInterval;
    }

    public Duration getSendMetricsConnectTimeout() {
        return sendMetricsConnectTimeout;
    }

    public void setSendMetricsConnectTimeout(Duration sendMetricsConnectTimeout) {
        this.sendMetricsConnectTimeout = sendMetricsConnectTimeout;
    }

    public Duration getSendMetricsReadTimeout() {
        return sendMetricsReadTimeout;
    }

    public void setSendMetricsReadTimeout(Duration sendMetricsReadTimeout) {
        this.sendMetricsReadTimeout = sendMetricsReadTimeout;
    }

    public Duration getFetchTogglesConnectTimeout() {
        return fetchTogglesConnectTimeout;
    }

    public void setFetchTogglesConnectTimeout(Duration fetchTogglesConnectTimeout) {
        this.fetchTogglesConnectTimeout = fetchTogglesConnectTimeout;
    }

    public Duration getFetchTogglesReadTimeout() {
        return fetchTogglesReadTimeout;
    }

    public void setFetchTogglesReadTimeout(Duration fetchTogglesReadTimeout) {
        this.fetchTogglesReadTimeout = fetchTogglesReadTimeout;
    }

    public boolean isSynchronousFetchOnInitialisation() {
        return synchronousFetchOnInitialisation;
    }

    public void setSynchronousFetchOnInitialisation(boolean synchronousFetchOnInitialisation) {
        this.synchronousFetchOnInitialisation = synchronousFetchOnInitialisation;
    }

    public boolean isProxyAuthenticationByJvmProperties() {
        return proxyAuthenticationByJvmProperties;
    }

    public void setProxyAuthenticationByJvmProperties(boolean proxyAuthenticationByJvmProperties) {
        this.proxyAuthenticationByJvmProperties = proxyAuthenticationByJvmProperties;
    }

    public enum HttpFetcher {
        HTTP_URL_CONNECTION_FETCHER,
        OK_HTTP
    }
}
