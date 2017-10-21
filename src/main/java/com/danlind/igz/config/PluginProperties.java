package com.danlind.igz.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Created by danlin on 2017-03-30.
 */
@Validated
@ConfigurationProperties(prefix = "plugin")
public class PluginProperties {

    private String realApiKey;

    private String demoApiKey;

    private String realApiUrl;

    private String demoApiUrl;

    private int restApiRetryInterval;

    private int restApiMaxRetry;

    private int refreshTokenInterval;

    private int refreshTokenMaxRetry;

    private int refreshTokenRetryInterval;

    private int refreshMarketDataInterval;

    public String getDemoApiKey() {
        return demoApiKey;
    }

    public void setDemoApiKey(String demoApiKey) {
        this.demoApiKey = demoApiKey;
    }

    public String getRealApiUrl() {
        return realApiUrl;
    }

    public void setRealApiUrl(String realApiUrl) {
        this.realApiUrl = realApiUrl;
    }

    public String getDemoApiUrl() {
        return demoApiUrl;
    }

    public void setDemoApiUrl(String demoApiUrl) {
        this.demoApiUrl = demoApiUrl;
    }

    public String getRealApiKey() {
        return realApiKey;
    }

    public void setRealApiKey(String realApiKey) {
        this.realApiKey = realApiKey;
    }

    public int getRefreshTokenInterval() {
        return refreshTokenInterval;
    }

    public void setRefreshTokenInterval(int refreshTokenInterval) {
        this.refreshTokenInterval = refreshTokenInterval;
    }

    public int getRefreshMarketDataInterval() {
        return refreshMarketDataInterval;
    }

    public void setRefreshMarketDataInterval(int refreshMarketDataInterval) {
        this.refreshMarketDataInterval = refreshMarketDataInterval;
    }

    public int getRefreshTokenMaxRetry() {
        return refreshTokenMaxRetry;
    }

    public void setRefreshTokenMaxRetry(int refreshTokenMaxRetry) {
        this.refreshTokenMaxRetry = refreshTokenMaxRetry;
    }

    public int getRefreshTokenRetryInterval() {
        return refreshTokenRetryInterval;
    }

    public void setRefreshTokenRetryInterval(int refreshTokenRetryInterval) {
        this.refreshTokenRetryInterval = refreshTokenRetryInterval;
    }
    public int getRestApiRetryInterval() {
        return restApiRetryInterval;
    }

    public void setRestApiRetryInterval(int restApiRetryInterval) {
        this.restApiRetryInterval = restApiRetryInterval;
    }

    public int getRestApiMaxRetry() {
        return restApiMaxRetry;
    }

    public void setRestApiMaxRetry(int restApiMaxRetry) {
        this.restApiMaxRetry = restApiMaxRetry;
    }


}