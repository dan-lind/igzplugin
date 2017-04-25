package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.adapter.StreamingApiAdapter;
import com.danlind.igz.config.PluginConfig;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.types.AccountType;
import com.danlind.igz.ig.api.client.rest.AuthenticationResponseAndConversationContext;
import com.danlind.igz.ig.api.client.rest.ConversationContext;
import com.danlind.igz.ig.api.client.rest.ConversationContextV3;
import com.danlind.igz.ig.api.client.rest.dto.session.createSessionV3.CreateSessionV3Request;
import com.danlind.igz.ig.api.client.rest.dto.session.refreshSessionV1.RefreshSessionV1Request;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

@Component
public class BrokerLogin {

    private final static Logger logger = LoggerFactory.getLogger(BrokerLogin.class);
    private final RestApiAdapter restApiAdapter;
    private final StreamingApiAdapter streamingApiAdapter;
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;
    private final PluginConfig pluginConfig;
    private AuthenticationResponseAndConversationContext authenticationContext;
    private AccountType zorroAccountType;
    private ScheduledFuture refreshAccessTokenFuture;

    @Autowired
    public BrokerLogin(StreamingApiAdapter streamingApiAdapter, RestApiAdapter restApiAdapter, ThreadPoolTaskScheduler threadPoolTaskScheduler, PluginConfig pluginConfig) {
        this.streamingApiAdapter = streamingApiAdapter;
        this.restApiAdapter = restApiAdapter;
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
        this.pluginConfig = pluginConfig;
    }

    public int connect(String identifier, String password, String accountType) {
        this.zorroAccountType = AccountType.valueOf(accountType);
        logger.info("Connecting to IG {}-account as {}", this.zorroAccountType.name(), identifier);
        ScheduledFuture future = indicateProgress();
        String apiKey = this.zorroAccountType == AccountType.Real ? pluginConfig.getRealApiKey() : pluginConfig.getDemoApiKey();

        try {
            CreateSessionV3Request authRequest = new CreateSessionV3Request();
            authRequest.setIdentifier(identifier);
            authRequest.setPassword(password);
            authenticationContext = restApiAdapter.createSessionV3(authRequest, apiKey);
            streamingApiAdapter.connect(authenticationContext);
            startRefreshAccessTokenScheduler();
            return ZorroReturnValues.LOGIN_OK.getValue();
        } catch (Exception e) {
            logger.error("Exception while logging in", e);
            Zorro.indicateError();
            return ZorroReturnValues.LOGIN_FAIL.getValue();
        } finally {
            future.cancel(true);
        }
    }

    public int disconnect() {
        logger.info("Disconnecting from IG");
        ScheduledFuture future = indicateProgress();
        streamingApiAdapter.disconnect();
        refreshAccessTokenFuture.cancel(true);
        future.cancel(true);
        return ZorroReturnValues.LOGOUT_OK.getValue();
    }

    private void refreshAccessToken(final ConversationContextV3 contextV3) {
        logger.debug("Refreshing access token");
        ConversationContextV3 newContextV3 = new ConversationContextV3(restApiAdapter.refreshSessionV1(contextV3, RefreshSessionV1Request.builder().refresh_token(contextV3.getRefreshToken()).build()), contextV3.getAccountId(), contextV3.getApiKey());
        authenticationContext.setConversationContext(newContextV3);
    }

    private void startRefreshAccessTokenScheduler() {
        refreshAccessTokenFuture = threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
                refreshAccessToken((ConversationContextV3) authenticationContext.getConversationContext());
            }, Date.from(Instant.now().plus(pluginConfig.getRefreshTokenInterval(), ChronoUnit.MILLIS))
            , pluginConfig.getRefreshTokenInterval());
    }

    private ScheduledFuture indicateProgress() {
        return threadPoolTaskScheduler.scheduleAtFixedRate(() -> Zorro.callProgress(1), 250);
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
