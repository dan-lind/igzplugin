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
import io.reactivex.Observable;
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


    public long getServerTime() {
        try {
            return restApi.getEncryptionKeySessionV1(loginHandler.getConversationContext()).getTimeStamp();
        } catch (HttpClientErrorException e) {
            LOG.error("Exception getting time from server: {}", e.getResponseBodyAsString(), e);
            Zorro.indicateError();
            throw e;
        } catch (Exception e) {
            LOG.error("Exception getting time from server", e);
            Zorro.indicateError();
            throw new RuntimeException(e.getMessage());
        }
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

    public int getTimeZoneOffset() {
        try {
            return restApi.getSessionV1(loginHandler.getConversationContext(), false).getBody().getTimezoneOffset();
        } catch (HttpClientErrorException e) {
            LOG.error("Exception when getting time zone offset info: {}", e.getResponseBodyAsString(), e);
            Zorro.indicateError();
            throw e;
        } catch (Exception e) {
            LOG.error("Exception when getting broker account info", e);
            Zorro.indicateError();
            throw new RuntimeException(e.getMessage());
        }
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
        try {
            return Observable.interval(pluginProperties.getRefreshMarketDataInterval(), TimeUnit.MILLISECONDS, Schedulers.io())
                .map(x -> restApi.getMarketDetailsV3(loginHandler.getConversationContext(), epic.getName()))
                .map(this::createContractDetails);
        } catch (HttpClientErrorException e) {
            LOG.error("Exception when getting broker account info: {}", e.getResponseBodyAsString(), e);
            Zorro.indicateError();
            throw e;
        } catch (Exception e) {
            LOG.error("Exception when getting broker account info", e);
            Zorro.indicateError();
            throw new RuntimeException(e.getMessage());
        }
    }

    public Observable<ContractDetails> getContractDetailsBlocking(Epic epic) {
        try {
            GetMarketDetailsV3Response marketDetails = restApi.getMarketDetailsV3(loginHandler.getConversationContext(), epic.getName());
            return Observable.just(createContractDetails(marketDetails));
        } catch (HttpClientErrorException e) {
            LOG.error("Exception when getting broker account info: {}", e.getResponseBodyAsString(), e);
            Zorro.indicateError();
            throw e;
        } catch (Exception e) {
            LOG.error("Exception when getting broker account info", e);
            Zorro.indicateError();
            throw new RuntimeException(e.getMessage());
        }
    }

    private ContractDetails createContractDetails(GetMarketDetailsV3Response marketDetails) {
        return new ContractDetails(new Epic(marketDetails.getInstrument().getEpic()),
            (1d / marketDetails.getSnapshot().getScalingFactor()),
            Double.parseDouble(marketDetails.getInstrument().getValueOfOnePip()),
            marketDetails.getDealingRules().getMinDealSize().getValue(),
            marketDetails.getInstrument().getMarginFactor().doubleValue(),
            marketDetails.getInstrument().getCurrencies().get(0).getBaseExchangeRate(),
            marketDetails.getSnapshot().getBid().doubleValue(),
            marketDetails.getSnapshot().getOffer().doubleValue(),
            marketDetails.getInstrument().getExpiry(),
            marketDetails.getInstrument().getCurrencies().get(0).getCode(),
            marketDetails.getSnapshot().getScalingFactor(),
            marketDetails.getSnapshot().getMarketStatus());
    }


    public Observable<AccountDetails> getAccountDetails(String accountId) {
        try {
            List<AccountsItem> accountsItemList = restApi.getAccountsV1(loginHandler.getConversationContext()).getAccounts();
            Balance balance = accountsItemList.stream().filter(account -> account.getAccountId().equals(accountId)).findFirst().get().getBalance();
            return Observable.just(new AccountDetails(balance.getBalance(), balance.getProfitLoss(), balance.getDeposit()));
        } catch (HttpClientErrorException e) {
            LOG.error("Exception when getting info for broker account {}, error was: {}", accountId, e.getResponseBodyAsString(), e);
            Zorro.indicateError();
            throw e;
        } catch (Exception e) {
            LOG.error("Exception when getting info for broker account {}", accountId, e);
            Zorro.indicateError();
            throw new RuntimeException(e.getMessage());
        }
    }

    public Observable<DealReference> createPosition(CreateOTCPositionV2Request createPositionRequest) {
        try {
            return Observable.just(new DealReference(restApi.createOTCPositionV2(loginHandler.getConversationContext(), createPositionRequest).getDealReference()));
        } catch (HttpClientErrorException e) {
            LOG.error("Exception when creating position for {}, error was: {}", createPositionRequest.getEpic(), e.getResponseBodyAsString(), e);
            Zorro.indicateError();
            return Observable.error(e);
        } catch (Exception e) {
            LOG.error("Exception when creating position for {}", createPositionRequest.getEpic(), e);
            Zorro.indicateError();
            return Observable.error(e);
        }
    }

    public Observable<DealReference> closePosition(CloseOTCPositionV1Request closePositionRequest) {
        try {
            return Observable.just(new DealReference(restApi.closeOTCPositionV1(loginHandler.getConversationContext(), closePositionRequest).getDealReference()));
        } catch (HttpClientErrorException e) {
            LOG.error("Exception when closing position for epic {} with deal id {}, error was {}", closePositionRequest.getEpic(), closePositionRequest.getDealId(), e.getResponseBodyAsString(), e);
            Zorro.indicateError();
            return Observable.error(e);
        } catch (Exception e) {
            LOG.error("Exception when closing position for epic {} with deal id {}, ", closePositionRequest.getEpic(), closePositionRequest.getDealId(), e);
            Zorro.indicateError();
            return Observable.error(e);
        }
    }

    public Observable<DealReference> getUpdateStopObservable(String dealId, UpdateOTCPositionV2Request updatePositionRequest) {
        try {
            return Observable.just(new DealReference(restApi.updateOTCPositionV2(loginHandler.getConversationContext(), dealId, updatePositionRequest).getDealReference()));
        } catch (HttpClientErrorException e) {
            LOG.error("Exception when updating position for deal id {}, error was {}", dealId, e.getResponseBodyAsString(), e);
            Zorro.indicateError();
            return Observable.error(e);
        } catch (Exception e) {
            LOG.error("Exception when updating position for deal id {}", dealId, e);
            Zorro.indicateError();
            return Observable.error(e);
        }
    }

    public Observable<GetDealConfirmationV1Response> getDealConfirmationObservable(String dealReference) {
        try {
            return Observable.just(restApi.getDealConfirmationV1(loginHandler.getConversationContext(), dealReference))
                .flatMap(dealConfirmation -> {
                    if (dealConfirmation.getDealStatus() == DealStatus.ACCEPTED) {
                        return Observable.just(dealConfirmation);
                    } else {
                        LOG.warn("Order with deal id {} was rejected with reason code {}", dealConfirmation.getDealId(), dealConfirmation.getReason());
                        Zorro.indicateError();
                        return Observable.error(new RuntimeException());
                    }
                });
        } catch (HttpClientErrorException e) {
            LOG.error("Exception when getting deal confirmation for deal reference {}, error was {}", dealReference, e.getResponseBodyAsString(), e);
            Zorro.indicateError();
            return Observable.error(e);
        } catch (Exception e) {
            LOG.error("Exception when getting deal confirmation for deal reference {}", dealReference, e);
            Zorro.indicateError();
            return Observable.error(e);
        }
    }

    public String getAccountId() {
        return loginHandler.getAccountId();
    }

    public AuthenticationResponseAndConversationContext createSessionV3(CreateSessionV3Request authRequest, String apiKey) {
        try {
            return restApi.createSessionV3(authRequest, apiKey);
        } catch (HttpClientErrorException e) {
            LOG.error("Exception when creating session with api key {}, error was {}", apiKey, e.getResponseBodyAsString(), e);
            Zorro.indicateError();
            throw e;
        } catch (Exception e) {
            LOG.error("Exception when creating session with api key {}", apiKey, e);
            Zorro.indicateError();
            throw new RuntimeException(e.getMessage());
        }
    }

    public AccessTokenResponse refreshSessionV1(ConversationContextV3 contextV3, RefreshSessionV1Request build) {
        try {
            return restApi.refreshSessionV1(contextV3, build);
        } catch (HttpClientErrorException e) {
            if (e.getRawStatusCode() == 401 && e.getResponseBodyAsString().equals("{\"errorCode\":\"error.security.oauth-token-invalid\"}")) {
                throw new OauthTokenInvalidException();
            } else {
                LOG.error("Exception when refreshing session token, error was {}", e.getResponseBodyAsString(), e);
                Zorro.indicateError();
                throw e;
            }
        } catch (Exception e) {
            LOG.error("Exception when refreshing session token", e);
            Zorro.indicateError();
            throw new RuntimeException(e.getMessage());
        }
    }
}
