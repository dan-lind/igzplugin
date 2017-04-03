package com.danlind.igz.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

/**
 * Created by danlin on 2017-03-30.
 */
@Validated
@ConfigurationProperties("plugin")
public class PluginConfig {

    @NotNull
    private String realApiKey;

    private String demoApiKey;

    private String realApiUrl;

    private String demoApiUrl;

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
}