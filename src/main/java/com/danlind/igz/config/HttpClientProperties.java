package com.danlind.igz.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Created by danlin on 2017-03-30.
 */
@Validated
@ConfigurationProperties(prefix = "plugin.httpclient")
public class HttpClientProperties {

    // Determines the timeout in milliseconds until a connection is established.
    private int connectionTimeout = 30000;
    // Returns the timeout in milliseconds used when requesting a connection from the connection manager.
    private int requestTimeout = 30000;
    // Defines the socket timeout (SO_TIMEOUT) in milliseconds, which is the timeout for waiting for data or,
    // put differently, a maximum period inactivity between two consecutive data packets).
    private int socketTimeout = 60000;

    private int maxTotalConnections = 20;
    private int defaultKeepAliveTimeMillis = 5000;
    private int closeIdleConnectionWaitTimeSecs = 35;

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    public int getDefaultKeepAliveTimeMillis() {
        return defaultKeepAliveTimeMillis;
    }

    public void setDefaultKeepAliveTimeMillis(int defaultKeepAliveTimeMillis) {
        this.defaultKeepAliveTimeMillis = defaultKeepAliveTimeMillis;
    }

    public int getCloseIdleConnectionWaitTimeSecs() {
        return closeIdleConnectionWaitTimeSecs;
    }

    public void setCloseIdleConnectionWaitTimeSecs(int closeIdleConnectionWaitTimeSecs) {
        this.closeIdleConnectionWaitTimeSecs = closeIdleConnectionWaitTimeSecs;
    }
}