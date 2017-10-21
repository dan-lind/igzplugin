package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
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
import io.reactivex.Flowable;
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

        String apiKey = this.zorroAccountType == AccountType.Real ? pluginProperties.getRealApiKey() : pluginProperties.getDemoApiKey();
        CreateSessionV3Request authRequest = new CreateSessionV3Request();
        authRequest.setIdentifier(identifier);
        authRequest.setPassword(password);
        Disposable progress = indicateProgress();

        return restApiAdapter.createSessionV3(authRequest, apiKey)
            .doOnSuccess(this::setAuthenticationContext)
            .flatMap(authenticationContext -> streamingApiAdapter.connect(authenticationContext))
            .map(__ -> ZorroReturnValues.LOGIN_OK.getValue())
            .doOnSuccess(__ -> startRefreshAccessTokenScheduler())
            .onErrorReturn(err -> ZorroReturnValues.LOGIN_FAIL.getValue())
            .doFinally(() -> progress.dispose())
            .blockingGet();
    }

    public int disconnect() {
        logger.info("Disconnecting from IG");
        Disposable progress = indicateProgress();
        streamingApiAdapter.disconnect();
        tokenSubscription.dispose();
        progress.dispose();
        return ZorroReturnValues.LOGOUT_OK.getValue();
    }

    private void refreshAccessToken() {
        logger.debug("Refreshing access token");
        ConversationContextV3 contextV3 = (ConversationContextV3) authenticationContext.getConversationContext();
        restApiAdapter.refreshSessionV1((ConversationContextV3) authenticationContext.getConversationContext(),
            RefreshSessionV1Request.builder().refresh_token(contextV3.getRefreshToken()).build())
            .subscribe(accessToken -> authenticationContext.setConversationContext(
                new ConversationContextV3(accessToken, contextV3.getAccountId(), contextV3.getApiKey())), error -> disconnect());
    }

    private void startRefreshAccessTokenScheduler() {
        if (Objects.nonNull(tokenSubscription)) {
            logger.debug("Disposing of existing access token subscription");
            tokenSubscription.dispose();
        }
        tokenSubscription = Flowable.interval(pluginProperties.getRefreshTokenInterval(), TimeUnit.MILLISECONDS, Schedulers.io())
            .doOnError(error -> logger.error("Got error from interval", error))
            .retry(3)
            .subscribe(x -> refreshAccessToken());
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

    private void setAuthenticationContext(AuthenticationResponseAndConversationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }
}
