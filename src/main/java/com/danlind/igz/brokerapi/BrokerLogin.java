package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.OauthTokenInvalidException;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.adapter.StreamingApiAdapter;
import com.danlind.igz.config.PluginProperties;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.types.AccountType;
import com.danlind.igz.ig.api.client.rest.AuthenticationResponseAndConversationContext;
import com.danlind.igz.ig.api.client.rest.ConversationContext;
import com.danlind.igz.ig.api.client.rest.ConversationContextV3;
import com.danlind.igz.ig.api.client.rest.dto.session.createSessionV3.CreateSessionV3Request;
import com.danlind.igz.ig.api.client.rest.dto.session.refreshSessionV1.RefreshSessionV1Request;
import com.danlind.igz.misc.RetryWithDelay;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
public class BrokerLogin {

    private final static Logger logger = LoggerFactory.getLogger(BrokerLogin.class);
    private final RestApiAdapter restApiAdapter;
    private final StreamingApiAdapter streamingApiAdapter;
    private final PluginProperties pluginProperties;
    private AuthenticationResponseAndConversationContext authenticationContext;
    private AccountType zorroAccountType;
    private Disposable tokenSubscription;

    @Autowired
    public BrokerLogin(StreamingApiAdapter streamingApiAdapter, RestApiAdapter restApiAdapter, PluginProperties pluginProperties) {
        this.streamingApiAdapter = streamingApiAdapter;
        this.restApiAdapter = restApiAdapter;
        this.pluginProperties = pluginProperties;
    }

    public int connect(String identifier, String password, String accountType) {
        this.zorroAccountType = AccountType.valueOf(accountType);
        logger.info("Connecting to IG {}-account as {}", this.zorroAccountType.name(), identifier);
        Disposable progress = indicateProgress();
        String apiKey = this.zorroAccountType == AccountType.Real ? pluginProperties.getRealApiKey() : pluginProperties.getDemoApiKey();

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
            progress.dispose();
        }
    }

    public int disconnect() {
        logger.info("Disconnecting from IG");
        Disposable progress = indicateProgress();
        streamingApiAdapter.disconnect();
        tokenSubscription.dispose();
        progress.dispose();
        return ZorroReturnValues.LOGOUT_OK.getValue();
    }

    private void refreshAccessToken(final ConversationContextV3 contextV3) {
        logger.debug("Refreshing access token");
        try {
            ConversationContextV3 newContextV3 = new ConversationContextV3(restApiAdapter.refreshSessionV1(contextV3, RefreshSessionV1Request.builder().refresh_token(contextV3.getRefreshToken()).build()), contextV3.getAccountId(), contextV3.getApiKey());
            authenticationContext.setConversationContext(newContextV3);
        } catch (OauthTokenInvalidException e) {
            logger.info("Detected invalid oauth token, attempting to reconnect");
            disconnect();
        }
    }

    //TOOD: What happens if an exception is thrown when attepting to refresh token? Is the observable cancelled?
    private void startRefreshAccessTokenScheduler() {
        if (Objects.nonNull(tokenSubscription)) {
            logger.debug("Disposing of existing access token subscription");
            tokenSubscription.dispose();
        }
        tokenSubscription = Observable.interval(pluginProperties.getRefreshTokenInterval(), TimeUnit.MILLISECONDS, Schedulers.io())
            .doOnError(e -> logger.debug("Error when refreshing session token, retrying"))
            .retryWhen(new RetryWithDelay(60, 5000))
            .subscribe(x -> {
                    ConversationContextV3 contextV3 = (ConversationContextV3) authenticationContext.getConversationContext();
                    refreshAccessToken(contextV3);
                },
                error -> {
                    logger.error("Exception after retrying refreshing session token, disconnecting");
                    disconnect();
                }
            );
    }

    private Disposable indicateProgress() {
        return Observable.interval(250, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribe(x -> Zorro.callProgress(1));
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
