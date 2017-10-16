package com.danlind.igz.adapter;

import com.danlind.igz.Zorro;
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
import com.danlind.igz.ig.api.client.rest.dto.getAccountsV1.AccountsItem;
import com.danlind.igz.ig.api.client.rest.dto.getAccountsV1.Balance;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.DealStatus;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.GetMarketDetailsV3Response;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.closeOTCPositionV1.CloseOTCPositionV1Request;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.CreateOTCPositionV2Request;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.updateOTCPositionV2.UpdateOTCPositionV2Request;
import com.danlind.igz.ig.api.client.rest.dto.prices.getPricesV3.GetPricesV3Response;
import com.danlind.igz.ig.api.client.rest.dto.session.createSessionV3.AccessTokenResponse;
import com.danlind.igz.ig.api.client.rest.dto.session.createSessionV3.CreateSessionV3Request;
import com.danlind.igz.ig.api.client.rest.dto.session.refreshSessionV1.RefreshSessionV1Request;
import com.danlind.igz.misc.ExceptionHelper;
import com.danlind.igz.misc.RetryWithDelay;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
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

    public GetPricesV3Response getHistoricPrices(String pageNumber, String maxTicks, String pageSize, String epic, String startDate, String endDate, String resolution) {
        try {
            return restApi.getPricesV3(loginHandler.getConversationContext(),
                pageNumber,
                maxTicks,
                pageSize,
                epic,
                startDate,
                endDate,
                resolution);
        } catch (HttpClientErrorException e) {
            LOG.error("Exception when getting historic prices for epic {}, error was {}", epic, e.getResponseBodyAsString(), e);
            Zorro.indicateError();
            throw e;
        } catch (Exception e) {
            LOG.error("Exception when getting historic prices for epic {}", epic, e);
            Zorro.indicateError();
            throw new RuntimeException(e.getMessage());
        }
    }

    public Observable<Integer> getTimeZoneOffset() {
        LOG.debug("Getting TimeZoneOffset");
        return Observable.interval(0, 1, TimeUnit.HOURS, Schedulers.io())
            .map(x -> restApi.getSessionV1(loginHandler.getConversationContext(), false).getBody().getTimezoneOffset())
            .retryWhen(new RetryWithDelay(5, 2000))
            .doOnError(err -> LOG.error("Exception when getting time zone offset info: {}", ExceptionHelper.getErrorMessage(err), err));
    }

    public boolean getPositionStatus(DealId dealId) {
        try {
            //Request will throw HttpClientErrorException if position is not found
            restApi.getPositionByDealIdV2(loginHandler.getConversationContext(), dealId.getValue());
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getRawStatusCode() == 404 && e.getResponseBodyAsString().equals("{\"errorCode\":\"error.position.notfound\"}")) {
                LOG.info("Position with dealId {} was not found, closed externally?", dealId.getValue());
                return false;
            } else {
                LOG.error("Exception when getting position status for dealId {}, error was {}", dealId.getValue(), e.getResponseBodyAsString(), e);
                Zorro.indicateError();
                throw e;
            }
        } catch (Exception e) {
            LOG.error("Exception when getting position status for dealId {}", dealId.getValue(), e);
            Zorro.indicateError();
            throw new RuntimeException(e.getMessage());
        }
    }

    public Observable<ContractDetails> getContractDetailsObservable(Epic epic) {
        return Observable.interval(pluginProperties.getRefreshMarketDataInterval(), TimeUnit.MILLISECONDS, Schedulers.io())
            .map(x -> restApi.getMarketDetailsV3(loginHandler.getConversationContext(), epic.getName()))
            .map(this::createContractDetails)
            .retryWhen(new RetryWithDelay(5, 2000))
            .doOnError(err -> LOG.error("Exception when getting contract details for {}, {}", epic.getName(), ExceptionHelper.getErrorMessage(err), err));
    }

    public Single<ContractDetails> getContractDetailsBlocking(Epic epic) {
        return Single.fromCallable(() -> restApi.getMarketDetailsV3(loginHandler.getConversationContext(), epic.getName()))
            .map(marketDetails -> createContractDetails(marketDetails))
            .doOnError(err -> LOG.error("Exception when getting contract details blocking for {}, {}", epic.getName(), ExceptionHelper.getErrorMessage(err),err));
    }

    private ContractDetails createContractDetails(GetMarketDetailsV3Response marketDetails) {
        return new ContractDetails(new Epic(marketDetails.getInstrument().getEpic()),
            (1d / marketDetails.getSnapshot().getScalingFactor()),
            Double.parseDouble(marketDetails.getInstrument().getValueOfOnePip().replace(",", "")) / marketDetails.getInstrument().getCurrencies().get(0).getBaseExchangeRate(),
            Double.parseDouble(marketDetails.getInstrument().getContractSize()),
            100 / marketDetails.getInstrument().getMarginFactor().doubleValue() * -1,
            marketDetails.getSnapshot().getBid().doubleValue(),
            marketDetails.getSnapshot().getOffer().doubleValue(),
            marketDetails.getInstrument().getExpiry(),
            marketDetails.getInstrument().getCurrencies().get(0).getCode(),
            marketDetails.getSnapshot().getScalingFactor(),
            marketDetails.getSnapshot().getMarketStatus());
    }


    public Single<AccountDetails> getAccountDetails(String accountId) {
        return Single.fromCallable(() -> restApi.getAccountsV1(loginHandler.getConversationContext()).getAccounts())
            .doOnError(err -> LOG.error("Exception when getting info for broker account {}, {}", accountId, ExceptionHelper.getErrorMessage(err),err))
            .map(accountsItems -> accountsItems.stream().filter(account -> account.getAccountId().equals(accountId)).findFirst().get().getBalance())
            .map(balance -> new AccountDetails(balance.getBalance(), balance.getProfitLoss(), balance.getDeposit()));
    }

    public Observable<DealReference> createPosition(CreateOTCPositionV2Request createPositionRequest) {
        return Observable.fromCallable(() -> new DealReference(restApi.createOTCPositionV2(loginHandler.getConversationContext(), createPositionRequest).getDealReference()))
            .retryWhen(new RetryWithDelay(3, 1500))
            .doOnError(err -> LOG.error("Exception when creating position for {}, {}", createPositionRequest.getEpic(), ExceptionHelper.getErrorMessage(err), err));
    }

    public Observable<DealReference> closePosition(CloseOTCPositionV1Request closePositionRequest) {
        return Observable.fromCallable(() ->
            new DealReference(restApi.closeOTCPositionV1(loginHandler.getConversationContext(), closePositionRequest).getDealReference()))
            .retryWhen(new RetryWithDelay(3, 1500))
            .doOnError(err -> LOG.error("Exception when closing position for deal id {}, {}", closePositionRequest.getDealId(), ExceptionHelper.getErrorMessage(err), err));
    }

    public Observable<DealReference> getUpdateStopObservable(String dealId, UpdateOTCPositionV2Request updatePositionRequest) {
        return Observable.fromCallable(() ->
            new DealReference(restApi.updateOTCPositionV2(loginHandler.getConversationContext(), dealId, updatePositionRequest).getDealReference()))
            .retryWhen(new RetryWithDelay(3, 1500))
            .doOnError(err -> LOG.error("Exception when updating position for deal id {}, {}", dealId, ExceptionHelper.getErrorMessage(err), err));
    }

    public Observable<GetDealConfirmationV1Response> getDealConfirmationObservable(String dealReference) {
        LOG.debug("Attempting to get confirmation for dealReference {}", dealReference);
        return Observable.fromCallable(() -> restApi.getDealConfirmationV1(loginHandler.getConversationContext(), dealReference))
            .flatMap(dealConfirmation -> {
                if (dealConfirmation.getDealStatus() == DealStatus.ACCEPTED) {
                    LOG.debug("Deal accepted for dealReference {}", dealReference);
                    return Observable.just(dealConfirmation);
                } else {
                    LOG.warn("Order with deal id {} was rejected with reason code {}", dealConfirmation.getDealId(), dealConfirmation.getReason());
                    Zorro.indicateError();
                    return Observable.error(new RuntimeException());
                }
            })
            .retryWhen(new RetryWithDelay(3, 1500))
            .doOnError(err -> LOG.error("Exception when getting deal confirmation for deal reference {}, {}", dealReference, ExceptionHelper.getErrorMessage(err),err));
    }

    public String getAccountId() {
        return loginHandler.getAccountId();
    }

    public Single<AuthenticationResponseAndConversationContext> createSessionV3(CreateSessionV3Request authRequest, String apiKey) {
        return Single.fromCallable(() -> restApi.createSessionV3(authRequest, apiKey))
            .doOnError(err -> LOG.error("Exception when logging in, {}",ExceptionHelper.getErrorMessage(err),err));
    }

    public Observable<AccessTokenResponse> refreshSessionV1(ConversationContextV3 contextV3, RefreshSessionV1Request build) {
        return Observable.fromCallable(() -> restApi.refreshSessionV1(contextV3, build))
            .retryWhen(new RetryWithDelay(pluginProperties.getRefreshTokenRetires(), pluginProperties.getRefreshTokenRetryInterval()))
            .doOnError(err -> LOG.error("Exception when refreshing session token,  {}",ExceptionHelper.getErrorMessage(err),err));
    }
}
