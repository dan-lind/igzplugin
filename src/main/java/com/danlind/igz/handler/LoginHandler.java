package com.danlind.igz.handler;

import com.danlind.igz.Zorro;
import com.danlind.igz.ZorroLogger;
import com.danlind.igz.config.PluginConfig;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.types.AccountType;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.StreamingAPI;
import com.danlind.igz.ig.api.client.rest.AuthenticationResponseAndConversationContext;
import com.danlind.igz.ig.api.client.rest.ConversationContext;
import com.danlind.igz.ig.api.client.rest.ConversationContextV3;
import com.danlind.igz.ig.api.client.rest.dto.session.createSessionV3.CreateSessionV3Request;
import com.danlind.igz.ig.api.client.rest.dto.session.refreshSessionV1.RefreshSessionV1Request;
import com.danlind.igz.ig.api.client.streaming.HandyTableListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;

@Component
public class LoginHandler {

    private final static Logger logger = LoggerFactory.getLogger(LoginHandler.class);
    private final RestAPI restApi;
    private final StreamingAPI streamingAPI;
    private final ArrayList<HandyTableListenerAdapter> listeners;
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;
    private final PluginConfig pluginConfig;
    private AuthenticationResponseAndConversationContext authenticationContext;
    private AccountType zorroAccountType;

    @Autowired
    public LoginHandler(StreamingAPI streamingAPI, RestAPI restApi, ThreadPoolTaskScheduler threadPoolTaskScheduler, ArrayList<HandyTableListenerAdapter> listeners, PluginConfig pluginConfig) {
        this.streamingAPI = streamingAPI;
        this.restApi = restApi;
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
        this.listeners = listeners;
        this.pluginConfig = pluginConfig;
    }

    //TODO: Handle account type, pickup apiKey from properties
    public int connect(String identifier, String password, String accountType)  {
        this.zorroAccountType = AccountType.valueOf(accountType);
        logger.info("Connecting to IG {}-account as {}", this.zorroAccountType.name(), identifier);
        ScheduledFuture future = indicateProgress();
        String apiKey = this.zorroAccountType == AccountType.Real ? pluginConfig.getRealApiKey() : pluginConfig.getDemoApiKey();

        try {
            CreateSessionV3Request authRequest = new CreateSessionV3Request();
            authRequest.setIdentifier(identifier);
            authRequest.setPassword(password);
            authenticationContext = restApi.createSessionV3(authRequest, apiKey);
            streamingAPI.connect(authenticationContext.getAccountId(), authenticationContext.getConversationContext(), authenticationContext.getLightstreamerEndpoint());
            startRefreshAccessTokenScheduler();
            return ZorroReturnValues.LOGIN_OK.getValue();
        } catch (Exception e) {
            logger.error("Exception while logging in", e);
            ZorroLogger.logPopUp(e.getMessage());
            return ZorroReturnValues.LOGIN_FAIL.getValue();
        } finally {
            future.cancel(true);
        }
    }

    public int disconnect() {
        logger.info("Disconnecting from IG");
        ScheduledFuture future = indicateProgress();
        unsubscribeAllLightstreamerListeners();
        streamingAPI.disconnect();
        future.cancel(true);
        return ZorroReturnValues.LOGOUT_OK.getValue();
    }

    private ConversationContextV3 refreshAccessToken(final ConversationContextV3 contextV3) throws Exception {
        logger.debug("Refreshing access token");
        ConversationContextV3 newContextV3 = new ConversationContextV3(restApi.refreshSessionV1(contextV3, RefreshSessionV1Request.builder().refresh_token(contextV3.getRefreshToken()).build()), contextV3.getAccountId(), contextV3.getApiKey());
        authenticationContext.setConversationContext(newContextV3);
        return newContextV3;
    }

    //TODO: Stop scheduler on disconnect
    private void startRefreshAccessTokenScheduler() {
        threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
            try {
                authenticationContext.setConversationContext(refreshAccessToken((ConversationContextV3) authenticationContext.getConversationContext()));
            } catch (Exception e) {
                logger.error("Failed to refresh access token");
            }
        }, 30000);
    }

    private void unsubscribeAllLightstreamerListeners() {
        for (HandyTableListenerAdapter listener : listeners) {
            try {
                streamingAPI.unsubscribe(listener.getSubscribedTableKey());
            } catch (Exception e) {
                logger.error("Failed to unsubscribe Lightstreamer listener", e);
            }
        }
        listeners.clear();
    }

    private ScheduledFuture indicateProgress() {
        logger.debug("Indicating progress to Zorro");
        ScheduledFuture future = threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
            Zorro.callProgress(1);
        }, 250);

        return future;
    }

    public ConversationContext getConversationContext() {
        return authenticationContext.getConversationContext();
    }

    public String getAccountId() {
        return authenticationContext.getAccountId();
    }

    public AccountType getZorroAccountType() {
        return zorroAccountType;
    }
}
