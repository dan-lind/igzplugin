package com.danlind.igz.misc;

import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.MarketStatus;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

@Component
public class MarketDataProvider {

    private final static Logger logger = LoggerFactory.getLogger(MarketDataProvider.class);
    private final RestApiAdapter restApiAdapter;
    private final HashMap<Epic, ContractDetails> contractDetailsMap = new HashMap<>();
    private HashMap<Epic, Disposable> marketDetailsSubscriptions = new HashMap<>();

    @Autowired
    public MarketDataProvider(RestApiAdapter restApiAdapter) {
        this.restApiAdapter = restApiAdapter;
    }

    private int isEpicTradable(Epic epic) {
        try {
            return contractDetailsMap.get(epic).getMarketStatus() == MarketStatus.TRADEABLE
                ? ZorroReturnValues.CONNECTION_OK.getValue()
                : ZorroReturnValues.CONNECTION_OK_BUT_MARKET_CLOSED.getValue();
        } catch (Exception e) {
            logger.error("Error getting market details", e);
            return ZorroReturnValues.CONNECTION_LOST_NEW_LOGIN_REQUIRED.getValue();
        }
    }

    public int isAnySubscribedEpicTradable() {
        return contractDetailsMap.keySet().stream().mapToInt(epic -> isEpicTradable(epic)).max().orElse(1);
    }

    public void cancelSubscription(Epic epic) {
        marketDetailsSubscriptions.get(epic).dispose();
        marketDetailsSubscriptions.remove(epic);
    }

    public Set<Epic> getAllSubscribedEpics() {
        return contractDetailsMap.keySet();
    }

    public ContractDetails getContractDetails(Epic epic) {
        return contractDetailsMap.get(epic);
    }


    public void updateMarketDetails(Epic epic) {
        ContractDetails contractDetails = restApiAdapter.getContractDetailsBlocking(epic).blockingGet();
        contractDetailsMap.put(epic, contractDetails);

        if (Objects.nonNull(marketDetailsSubscriptions.get(epic))) {
            logger.debug("Disposing of existing market data subscription for epic {}", epic.getName());
            marketDetailsSubscriptions.get(epic).dispose();
            marketDetailsSubscriptions.remove(epic);
        }

        marketDetailsSubscriptions.put(epic,restApiAdapter.getContractDetailsObservable(epic)
            .subscribeOn(Schedulers.io())
            .subscribe(
                updatedContractDetails -> {
                    logger.debug("Updating contract details for {}",updatedContractDetails.getEpic().getName());
                    contractDetailsMap.put(epic, updatedContractDetails);
                }
            ));


    }
}
