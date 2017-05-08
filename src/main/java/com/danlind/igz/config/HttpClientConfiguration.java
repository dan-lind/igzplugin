package com.danlind.igz.config;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 *
 * Adapted from http://www.baeldung.com/httpclient-connection-management
 *
 *
 * Spring Configuration for HTTP Clients.
 * <p/>
 * - Supports both HTTP and HTTPS
 * - Uses a connection pool to re-use connections and save overhead of creating connections.
 * - Has a custom connection keep-alive strategy (to apply a default keep-alive if one isn't specified)
 * - Starts an idle connection monitor to continuously clean up stale connections.
 * <p/>
 * Tested with:
 * - org.apache.httpcomponents:httpclient:4.3.5
 * - org.springframework:spring-context:4.1.6.RELEASE
 * see http://www.baeldung.com/httpclient-connection-management
 */
@Configuration
@EnableScheduling
public class HttpClientConfiguration {

    private final HttpClientProperties httpClientProperties;
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientConfiguration.class);
    private static final int ONE_SECOND_IN_MILLIS = 1000;

    @Autowired
    public HttpClientConfiguration(HttpClientProperties httpClientProperties) {
        this.httpClientProperties = httpClientProperties;
    }

    @Bean
    public RestTemplate getRestTemplate(HttpClient httpClient) {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Bean
    public PoolingHttpClientConnectionManager getPoolingConnectionManager() {
        SSLContextBuilder builder = new SSLContextBuilder();
        try {
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            LOGGER.error("Pooling Connection Manager Initialisation failure because of " + e.getMessage(), e);
        }

        SSLConnectionSocketFactory sslsf = null;
        try {
            sslsf = new SSLConnectionSocketFactory(builder.build());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            LOGGER.error("Pooling Connection Manager Initialisation failure because of " + e.getMessage(), e);
        }

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
            .<ConnectionSocketFactory>create().register("https", sslsf)
            .register("http", new PlainConnectionSocketFactory())
            .build();

        PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        poolingConnectionManager.setMaxTotal(httpClientProperties.getMaxTotalConnections());
        return poolingConnectionManager;
    }

    @Bean
    public Runnable getIdleConnectionMonitor(final PoolingHttpClientConnectionManager connectionManager) {
        return new Runnable() {
            @Override
            @Scheduled(fixedDelay = 10000)
            public void run() {
                try {
                    if (connectionManager != null) {
                        LOGGER.trace("run IdleConnectionMonitor - Closing expired and idle connections...");
                        connectionManager.closeExpiredConnections();
                        connectionManager.closeIdleConnections(httpClientProperties.getCloseIdleConnectionWaitTimeSecs(), TimeUnit.SECONDS);
                    } else {
                        LOGGER.trace("run IdleConnectionMonitor - Http Client Connection manager is not initialised");
                    }
                } catch (Exception e) {
                    LOGGER.error("run IdleConnectionMonitor - Exception occurred. msg={}, e={}", e.getMessage(), e);
                }
            }

        };
    }

    public ConnectionKeepAliveStrategy getConnectionKeepAliveStrategy() {
        return (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator
                (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();

                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * ONE_SECOND_IN_MILLIS;
                }
            }
            return httpClientProperties.getDefaultKeepAliveTimeMillis();
        };
    }

    @Bean
    public CloseableHttpClient getHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(httpClientProperties.getRequestTimeout())
            .setConnectTimeout(httpClientProperties.getConnectionTimeout())
            .setSocketTimeout(httpClientProperties.getSocketTimeout()).build();

        return HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(getPoolingConnectionManager())
            .setKeepAliveStrategy(getConnectionKeepAliveStrategy())
            .build();
    }

}