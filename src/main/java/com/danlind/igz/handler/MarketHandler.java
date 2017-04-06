package com.danlind.igz.handler;

import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.GetMarketDetailsV3Response;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.MarketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class MarketHandler {

    private final static Logger logger = LoggerFactory.getLogger(MarketHandler.class);
    private final RestAPI restAPI;
    private final LoginHandler loginHandler;
    private final HashMap<Epic, ContractDetails> contractDetailsMap;
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private static final int REFRESH_INTERVAL = 30000;

    @Autowired
    public MarketHandler(RestAPI restApi, LoginHandler loginHandler, ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        this.restAPI = restApi;
        this.loginHandler = loginHandler;
        this.contractDetailsMap = new HashMap<>();
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
    }

    /**
     * Subscribe to Lightstreamer heartbeat which arrives every second.
     * Heartbeat sends seconds since epoch, so multiply by 1000 for millis
     */
    public int isEpicTradable(Epic epic) {
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
        return contractDetailsMap.keySet().stream().mapToInt(epic -> isEpicTradable(epic)).max().getAsInt();
    }

    //TODO: Stop scheduler on disconnect
    //TODO: Allow user config to control frequency of REFRESH_INTERVAL
    private void refreshContractDetails() {
        threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
            try {
                contractDetailsMap.keySet().stream().forEach(this::getContractDetails);
            } catch (Exception e) {
                logger.error("Failed to update market details");
            }
        }, REFRESH_INTERVAL);
    }

    public ContractDetails getContractDetails(Epic epic) {
        return contractDetailsMap.get(epic);
    }

    public void updateMarketDetails(Epic epic) {
        try {
            GetMarketDetailsV3Response marketDetails = restAPI.getMarketDetailsV3(loginHandler.getConversationContext(),epic.getName());
            ContractDetails contractDetails =
                    new ContractDetails(epic,
                            (1d/marketDetails.getSnapshot().getScalingFactor()),
                            Double.parseDouble(marketDetails.getInstrument().getValueOfOnePip()),
                            marketDetails.getDealingRules().getMinDealSize().getValue(),
                            marketDetails.getInstrument().getMarginFactor().doubleValue(),
                            marketDetails.getInstrument().getCurrencies().get(0).getBaseExchangeRate(),
                            marketDetails.getSnapshot().getBid().doubleValue(),
                            marketDetails.getSnapshot().getOffer().doubleValue(),
                            marketDetails.getSnapshot().getScalingFactor(),
                            marketDetails.getSnapshot().getMarketStatus());
            logger.debug("Creating new ContractDetails for epic {} with scaling factor {}, pip value {}, lot size {}, margin factor {}, marget status {}",
                    epic.getName(),
                    (1d/marketDetails.getSnapshot().getScalingFactor()),
                    Double.parseDouble(marketDetails.getInstrument().getValueOfOnePip()),
                    marketDetails.getDealingRules().getMinDealSize().getValue(),
                    marketDetails.getInstrument().getMarginFactor().doubleValue(),
                    marketDetails.getSnapshot().getMarketStatus());
            contractDetailsMap.put(epic, contractDetails);
        } catch (Exception e) {
            logger.error("Error getting market details", e);
        }

    }
}
