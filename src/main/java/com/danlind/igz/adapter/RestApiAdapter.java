package com.danlind.igz.adapter;

import com.danlind.igz.config.PluginProperties;
import com.danlind.igz.domain.AccountDetails;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.domain.types.DealReference;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.handler.LoginHandler;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.AuthenticationResponseAndConversationContext;
import com.danlind.igz.ig.api.client.rest.ConversationContextV3;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.DealStatus;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.closeOTCPositionV1.CloseOTCPositionV1Request;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.CreateOTCPositionV2Request;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.updateOTCPositionV2.UpdateOTCPositionV2Request;
import com.danlind.igz.ig.api.client.rest.dto.prices.getPricesV3.GetPricesV3Response;
import com.danlind.igz.ig.api.client.rest.dto.session.createSessionV3.AccessTokenResponse;
import com.danlind.igz.ig.api.client.rest.dto.session.createSessionV3.CreateSessionV3Request;
import com.danlind.igz.ig.api.client.rest.dto.session.refreshSessionV1.RefreshSessionV1Request;
import com.danlind.igz.misc.ExceptionHelper;
import com.danlind.igz.misc.RetryWithDelay;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * Created by danlin on 2017-04-07.
 */
@Component
public class RestApiAdapter {

    private final static Logger LOG = LoggerFactory.getLogger(RestApiAdapter.class);

    //Parameter injection here to avoid circular dependencies
    @Autowired
    private RestAPI restApi;

    @Autowired
    private LoginHandler loginHandler;

    @Autowired
    private PluginProperties pluginProperties;

    public Single<Long> getServerTime() {
        return Single.fromCallable(() -> restApi.getEncryptionKeySessionV1(loginHandler.getConversationContext()).getTimeStamp())
            .doOnError(err -> LOG.error("Exception getting time from server: {}", ExceptionHelper.getErrorMessage(err), err));
    }

    public Single<GetPricesV3Response> getHistoricPrices(String pageNumber, String maxTicks, String pageSize, String epic, String startDate, String endDate, String resolution) {
        return Single.fromCallable(() -> restApi.getPricesV3(loginHandler.getConversationContext(),
            pageNumber, maxTicks, pageSize, epic, startDate, endDate, resolution))
            .doOnError(err -> LOG.error("Exception when getting historic prices for epic {}, {}", epic, ExceptionHelper.getErrorMessage(err), err));
    }

    public Flowable<Integer> getTimeZoneOffset() {
        LOG.debug("Getting TimeZoneOffset");
        return Flowable.interval(0, 1, TimeUnit.HOURS, Schedulers.io())
            .map(x -> restApi.getSessionV1(loginHandler.getConversationContext(), false).getBody().getTimezoneOffset())
            .retryWhen(new RetryWithDelay(pluginProperties.getRestApiMaxRetry(), pluginProperties.getRestApiRetryInterval()))
            .doOnError(err -> LOG.error("Exception when getting time zone offset info: {}", ExceptionHelper.getErrorMessage(err), err));
    }

    public Single<Boolean> getPositionStatus(DealId dealId) {
        //Request will throw HttpClientErrorException if position is not found
        return Single.fromCallable(() -> restApi.getPositionByDealIdV2(loginHandler.getConversationContext(), dealId.getValue()))
            .map(__ -> true)
            .retryWhen(new RetryWithDelay(pluginProperties.getRestApiMaxRetry(), pluginProperties.getRestApiRetryInterval()))
            .onErrorReturn(err -> {
                if (err instanceof HttpClientErrorException) {
                    HttpClientErrorException e = (HttpClientErrorException) err;
                    if (e.getRawStatusCode() == 404 && e.getResponseBodyAsString().equals("{\"errorCode\":\"error.position.notfound\"}")) {
                        LOG.info("Position with dealId {} was not found, closed externally?", dealId.getValue());
                        return false;
                    }
                }
                //Some other exception happened and we can't be sure the order is really gone, so we keep it
                LOG.error("Exception when getting position for dealId {}, {}", dealId.getValue(), ExceptionHelper.getErrorMessage(err), err);
                return true;
            });


    }

    public Flowable<ContractDetails> getContractDetailsFlowable(Epic epic) {
        return Flowable.interval(pluginProperties.getRefreshMarketDataInterval(), TimeUnit.MILLISECONDS, Schedulers.io())
            .map(x -> restApi.getMarketDetailsV3(loginHandler.getConversationContext(), epic.getName()))
            .map(ContractDetails::createContractDetailsFromResponse)
            .retryWhen(new RetryWithDelay(pluginProperties.getRestApiMaxRetry(), pluginProperties.getRestApiRetryInterval()))
            .doOnError(err -> LOG.error("Exception when getting contract details for {}, {}", epic.getName(), ExceptionHelper.getErrorMessage(err), err));
    }

    public Single<ContractDetails> getContractDetailsBlocking(Epic epic) {
        return Single.fromCallable(() -> restApi.getMarketDetailsV3(loginHandler.getConversationContext(), epic.getName()))
            .map(ContractDetails::createContractDetailsFromResponse)
            .doOnError(err -> LOG.error("Exception when getting contract details blocking for {}, {}", epic.getName(), ExceptionHelper.getErrorMessage(err), err));
    }

    public Single<AccountDetails> getAccountDetails(String accountId) {
        return Single.fromCallable(() -> restApi.getAccountsV1(loginHandler.getConversationContext()).getAccounts())
            .doOnError(err -> LOG.error("Exception when getting info for broker account {}, {}", accountId, ExceptionHelper.getErrorMessage(err), err))
            .map(accountsItems -> accountsItems.stream().filter(account -> account.getAccountId().equals(accountId)).findFirst().get().getBalance())
            .map(balance -> new AccountDetails(balance.getBalance(), balance.getProfitLoss(), balance.getDeposit()));
    }

    public Single<DealReference> createPosition(CreateOTCPositionV2Request createPositionRequest) {
        return Single.fromCallable(() -> new DealReference(restApi.createOTCPositionV2(loginHandler.getConversationContext(), createPositionRequest).getDealReference()))
            .retryWhen(new RetryWithDelay(pluginProperties.getRestApiMaxRetry(), pluginProperties.getRestApiRetryInterval()))
            .doOnError(err -> LOG.error("Exception when creating position for {}, {}", createPositionRequest.getEpic(), ExceptionHelper.getErrorMessage(err), err));
    }

    public Single<DealReference> closePosition(CloseOTCPositionV1Request closePositionRequest) {
        return Single.fromCallable(() ->
            new DealReference(restApi.closeOTCPositionV1(loginHandler.getConversationContext(), closePositionRequest).getDealReference()))
            .retryWhen(new RetryWithDelay(pluginProperties.getRestApiMaxRetry(), pluginProperties.getRestApiRetryInterval()))
            .doOnError(err -> LOG.error("Exception when closing position for deal id {}, {}", closePositionRequest.getDealId(), ExceptionHelper.getErrorMessage(err), err));
    }

    public Single<DealReference> updateStop(String dealId, UpdateOTCPositionV2Request updatePositionRequest) {
        return Single.fromCallable(() ->
            new DealReference(restApi.updateOTCPositionV2(loginHandler.getConversationContext(), dealId, updatePositionRequest).getDealReference()))
            .retryWhen(new RetryWithDelay(pluginProperties.getRestApiMaxRetry(), pluginProperties.getRestApiRetryInterval()))
            .doOnError(err -> LOG.error("Exception when updating position for deal id {}, {}", dealId, ExceptionHelper.getErrorMessage(err), err));
    }

    public Single<Optional<GetDealConfirmationV1Response>> getDealConfirmation(String dealReference) {
        LOG.debug("Attempting to get confirmation for dealReference {}", dealReference);
        return Single.fromCallable(() -> restApi.getDealConfirmationV1(loginHandler.getConversationContext(), dealReference))
            .flatMap(dealConfirmation -> {
                if (dealConfirmation.getDealStatus() == DealStatus.ACCEPTED) {
                    LOG.debug("Deal accepted for dealReference {}", dealReference);
                    return Single.just(Optional.of(dealConfirmation));
                } else {
                    LOG.warn("Order with deal id {} was rejected with reason code {}", dealConfirmation.getDealId(), dealConfirmation.getReason());
//                    Zorro.indicateError();
                    return Single.just(Optional.<GetDealConfirmationV1Response>empty());
                }
            })
            .retryWhen(new RetryWithDelay(pluginProperties.getRestApiMaxRetry(), pluginProperties.getRestApiRetryInterval()))
            .doOnError(err -> LOG.error("Exception when getting deal confirmation for deal reference {}, {}", dealReference, ExceptionHelper.getErrorMessage(err), err));
    }

    public Single<AuthenticationResponseAndConversationContext> createSessionV3(CreateSessionV3Request authRequest, String apiKey) {
        return Single.fromCallable(() -> restApi.createSessionV3(authRequest, apiKey))
            .retryWhen(new RetryWithDelay(pluginProperties.getRefreshTokenMaxRetry(), pluginProperties.getRefreshTokenRetryInterval()))
            .doOnError(err -> LOG.error("Exception when logging in, {}", ExceptionHelper.getErrorMessage(err), err));
    }

    public Flowable<AccessTokenResponse> refreshSessionV1(ConversationContextV3 contextV3, RefreshSessionV1Request build) {
        return Flowable.fromCallable(() -> restApi.refreshSessionV1(contextV3, build))
            .retryWhen(new RetryWithDelay(pluginProperties.getRefreshTokenMaxRetry(), pluginProperties.getRefreshTokenRetryInterval()))
            .doOnError(err -> LOG.error("Exception when refreshing session token,  {}", ExceptionHelper.getErrorMessage(err), err));
    }

    public String getAccountId() {
        return loginHandler.getAccountId();
    }

}
