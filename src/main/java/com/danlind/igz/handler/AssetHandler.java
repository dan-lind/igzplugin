package com.danlind.igz.handler;

import com.danlind.igz.Zorro;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.PriceDetails;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.ig.api.client.StreamingAPI;
import com.danlind.igz.ig.api.client.rest.dto.prices.getPricesV3.PricesItem;
import com.danlind.igz.ig.api.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AssetHandler {

    private static final double valueNotSupported = 0.0;
    private final static Logger logger = LoggerFactory.getLogger(AssetHandler.class);
    private final StreamingAPI streamingAPI;
    private final ArrayList<HandyTableListenerAdapter> listeners;
    private final Map<Epic,PriceDetails> priceDataMap = new ConcurrentHashMap<>();
    private final MarketHandler marketHandler;
    private final VolumeProvider volumeProvider;
    private final HistoryHandler historyHandler;
    private static final String MINUTE = "1MINUTE";
    private static final String CONS_END = "CONS_END";
    private static final String ONE = "1";
    private static final String LAST_TRADED_VOLUME = "LTV";
    private static final String OFFER = "OFR";
    private static final String BID = "BID";
    private static final int VOLUME_WINDOW_LENGTH = 5;


    @Autowired
    public AssetHandler(StreamingAPI streamingAPI, ArrayList<HandyTableListenerAdapter> listeners, MarketHandler marketHandler, VolumeProvider volumeProvider, HistoryHandler historyHandler) {
        this.streamingAPI = streamingAPI;
        this.listeners = listeners;
        this.marketHandler = marketHandler;
        this.volumeProvider = volumeProvider;
        this.historyHandler = historyHandler;
    }

    public int subscribeToLighstreamerTickUpdates(Epic epic) {
        logger.info("Subscribing to Lightstreamer tick updates for market: {} ", epic.getName());

        try {
            listeners.add(streamingAPI.subscribeForChartTicks(epic.getName(), new HandyTableListenerAdapter() {
                @Override
                public void onUpdate(int i, String s, UpdateInfo updateInfo) {
//                    logger.debug("Received tick update from Lightstreamer for asset {} with data {}", s, updateInfo);
                    if (Objects.nonNull(updateInfo.getNewValue(1))) {
                        updatePriceDetails(new Epic(updateInfo.getItemName().split(":")[1]),
                                Double.parseDouble(updateInfo.getNewValue(BID)),
                                Double.parseDouble(updateInfo.getNewValue(OFFER)));
                    }
                }
            }));

            //Subscribe to chart candles for volume data
            listeners.add(streamingAPI.subscribeForChartCandles(epic.getName(), MINUTE, new HandyTableListenerAdapter() {
                @Override
                public void onUpdate(int i, String s, UpdateInfo updateInfo) {
//                  logger.debug("Received chart update from Lightstreamer for asset {} with data {}", s, updateInfo);
                    if (updateInfo.getNewValue(CONS_END).equals(ONE)) {
                        logger.debug("Minute candle ended with volume {}", updateInfo.getNewValue(LAST_TRADED_VOLUME));
                        volumeProvider.updateRollingVolume(epic, Integer.parseInt(updateInfo.getNewValue(LAST_TRADED_VOLUME)));
                    }
                }
            }));

            List<PricesItem> pricesItems = historyHandler.getPriceHistory(epic, VOLUME_WINDOW_LENGTH);
            pricesItems.stream().forEach(x -> volumeProvider.updateRollingVolume(epic, x.getLastTradedVolume().intValue()));

            marketHandler.updateMarketDetails(epic);

            //Initialize prices to prevent NPE in case Zorro calls for asset details before first stream update
            ContractDetails contractDetails = marketHandler.getContractDetails(epic);
            updatePriceDetails(epic, contractDetails.getSnapshotBid(), contractDetails.getSnapshotAsk());

            return ZorroReturnValues.ASSET_AVAILABLE.getValue();
        } catch (Exception e) {
            logger.error("Error when subscribing to {}", epic.getName(), e);
            Zorro.indicateError();
            return ZorroReturnValues.ASSET_UNAVAILABLE.getValue();
        }
    }

    private void updatePriceDetails(Epic epic, double bid, double ask) {
        PriceDetails priceDetails = new PriceDetails(epic, bid, ask, volumeProvider.getAverageVolume(epic));
        priceDataMap.put(priceDetails.getEpic(), priceDetails);
    }

    public int getLatestAssetData(Epic epic, double assetParams[]) {
        ContractDetails contractDetails = marketHandler.getContractDetails(epic);
        PriceDetails priceDetails = priceDataMap.get(epic);

        if (Objects.nonNull(contractDetails) && Objects.nonNull(priceDetails)) {
            assetParams[0] = priceDetails.getAsk();
            assetParams[1] = priceDetails.getSpread();
            assetParams[2] = priceDetails.getVolume(); //Volume
            assetParams[3] = contractDetails.getPipSize(); //PipSize, , size of one Pip, e.g. 0.0001 for EUR/USD
            assetParams[4] = contractDetails.getPipCostInAccountCurrency(); //PipCost
            assetParams[5] = contractDetails.getLotAmount(); //LotAmount
            assetParams[6] = contractDetails.getMarginCostInAccountCurrency(); //MarginCost
            assetParams[7] = valueNotSupported; //Rollover cost Long
            assetParams[8] = valueNotSupported; //Rollover cost Short
//            logger.debug("Returning asset data Ask: {}, Spread: {}, Volume: {}, PipSize: {}, PipCost: {}, LotAmount: {}, MarginCost: {}",
//                    priceDetails.getAsk(), priceDetails.getSpread(), priceDetails.getVolume(), contractDetails.getPipSize(), contractDetails.getPipCostInAccountCurrency(),
//                    contractDetails.getLotAmount(), contractDetails.getMarginCostInAccountCurrency());

            return ZorroReturnValues.ASSET_AVAILABLE.getValue();
        } else {
            logger.warn("Epic {}, ContractDetails exist: {}, PriceDetails exists: {}", epic.getName(), Objects.nonNull(contractDetails), Objects.nonNull(priceDetails));
            return ZorroReturnValues.ASSET_UNAVAILABLE.getValue();
        }
    }

    public PriceDetails getAssetDetails(Epic epic) {
        return priceDataMap.get(epic);
    }
}
