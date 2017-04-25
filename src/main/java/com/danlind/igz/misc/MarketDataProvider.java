package com.danlind.igz.misc;

import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.PluginConfig;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.MarketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;

@Component
public class MarketDataProvider {

    private final static Logger logger = LoggerFactory.getLogger(MarketDataProvider.class);
    private final RestApiAdapter restApiAdapter;
    private final HashMap<Epic, ContractDetails> contractDetailsMap = new HashMap<>();
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;
    private final PluginConfig pluginConfig;
    private ScheduledFuture scheduledFuture;

    @Autowired
    public MarketDataProvider(RestApiAdapter restApiAdapter, ThreadPoolTaskScheduler threadPoolTaskScheduler, PluginConfig pluginConfig) {
        this.restApiAdapter = restApiAdapter;
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
        this.pluginConfig = pluginConfig;
    }

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
        return contractDetailsMap.keySet().stream().mapToInt(epic -> isEpicTradable(epic)).max().orElse(1);
    }

    public void startRefreshMarketScheduler() {
        logger.debug("Refreshing contract details");
        scheduledFuture = threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
                contractDetailsMap.keySet().stream().forEach(this::updateMarketDetails);
            }, Date.from(Instant.now().plus(pluginConfig.getRefreshMarketDataInterval(), ChronoUnit.MILLIS))
            , pluginConfig.getRefreshMarketDataInterval());
    }

    public void cancelScheduler() {
        scheduledFuture.cancel(true);
    }

    public ContractDetails getContractDetails(Epic epic) {
        return contractDetailsMap.get(epic);
    }

    public void updateMarketDetails(Epic epic) {
        ContractDetails contractDetails = restApiAdapter.getContractDetails(epic).blockingSingle();
        contractDetailsMap.put(epic, contractDetails);
    }
}
