package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.StreamingApiAdapter;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.PriceDetails;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.domain.types.Volume;
import com.danlind.igz.handler.HistoryHandler;
import com.danlind.igz.misc.MarketDataProvider;
import com.danlind.igz.misc.VolumeProvider;
import com.danlind.igz.ig.api.client.rest.dto.prices.getPricesV3.PricesItem;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BrokerAsset {

    private static final double valueNotSupported = 0.0;
    private final static Logger LOG = LoggerFactory.getLogger(BrokerAsset.class);
    private final Map<Epic, PriceDetails> priceDataMap = new ConcurrentHashMap<>();
    private final MarketDataProvider marketDataProvider;
    private final VolumeProvider volumeProvider;
    private final StreamingApiAdapter streamingApiAdapter;
    private final HistoryHandler historyHandler;
    private static final int VOLUME_WINDOW_LENGTH = 5;


    @Autowired
    public BrokerAsset(MarketDataProvider marketDataProvider, VolumeProvider volumeProvider, StreamingApiAdapter streamingApiAdapter, HistoryHandler historyHandler) {
        this.marketDataProvider = marketDataProvider;
        this.volumeProvider = volumeProvider;
        this.streamingApiAdapter = streamingApiAdapter;
        this.historyHandler = historyHandler;
    }

    /**
     * Used to reconnect to the streaming API for all epics in case we lose the session and need to login again
     */
    public void reconnectAll() {
        marketDataProvider.getAllSubscribedEpics().stream().forEach(epic -> {
            LOG.debug("Subscribing for epic {}", epic.getName());
            subscribeToLighstreamerTickUpdates(epic);
        });
    }

    public int subscribeToLighstreamerTickUpdates(Epic epic) {
        try {
            streamingApiAdapter.getTickObservable(epic)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    priceDetails -> priceDataMap.put(priceDetails.getEpic(), priceDetails),
                    e -> {
                        LOG.error("Error subscribing to tick observable", e);
                        Zorro.indicateError();
                    },
                    () -> {
                        //TODO: How to handle close of stream on weekends?
                        LOG.info("Received complete signal from TickObservable for epic {}", epic.getName());
                        marketDataProvider.cancelSubscription();
                    }
                );

            streamingApiAdapter.getVolumeObservable(epic)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    volume -> volumeProvider.updateRollingVolume(epic, volume),
                    e -> {
                        LOG.error("Error subscribing to volume observable", e);
                        Zorro.indicateError();
                    },
                    () -> LOG.info("Received complete signal from VolumeObservable for epic {}", epic.getName())
                );

            //Init volume from historic data
            List<PricesItem> pricesItems = historyHandler.getPriceHistory(epic, VOLUME_WINDOW_LENGTH);
            pricesItems.stream().forEach(pricesItem -> volumeProvider.updateRollingVolume(epic, new Volume(pricesItem.getLastTradedVolume().intValue())));
            marketDataProvider.updateMarketDetails(epic);

            //Initialize prices to prevent NPE in case Zorro calls for asset details before first stream update
            ContractDetails contractDetails = marketDataProvider.getContractDetails(epic);
            priceDataMap.put(epic, new PriceDetails(epic, contractDetails.getSnapshotBid(), contractDetails.getSnapshotAsk()));

            return ZorroReturnValues.ASSET_AVAILABLE.getValue();
        } catch (Exception e) {
            LOG.error("Error when subscribing to {}", epic.getName(), e);
            Zorro.indicateError();
            return ZorroReturnValues.ASSET_UNAVAILABLE.getValue();
        }
    }

    public int getLatestAssetData(Epic epic, double assetParams[]) {
        ContractDetails contractDetails = marketDataProvider.getContractDetails(epic);
        PriceDetails priceDetails = priceDataMap.get(epic);

        if (Objects.nonNull(contractDetails) && Objects.nonNull(priceDetails)) {
            assetParams[0] = priceDetails.getAsk();
            assetParams[1] = priceDetails.getSpread();
            assetParams[2] = volumeProvider.getAverageVolume(epic); //Volume
            assetParams[3] = contractDetails.getPipSize(); //PipSize, , size of one Pip, e.g. 0.0001 for EUR/USD
            assetParams[4] = contractDetails.getPipCost(); //PipCost
            assetParams[5] = contractDetails.getLotAmount(); //LotAmount
            assetParams[6] = contractDetails.getMarginCost(); //MarginCost, here leverage is returned instead
            assetParams[7] = valueNotSupported; //Rollover cost Long
            assetParams[8] = valueNotSupported; //Rollover cost Short
//            logger.debug("Returning asset data Ask: {}, Spread: {}, Volume: {}, PipSize: {}, PipCost: {}, LotAmount: {}, MarginCost: {}",
//                    priceDetails.getAsk(), priceDetails.getSpread(), priceDetails.getVolume(), contractDetails.getPipSize(), contractDetails.getPipCostInAccountCurrency(),
//                    contractDetails.getLotAmount(), contractDetails.getMarginCostInAccountCurrency());

            return ZorroReturnValues.ASSET_AVAILABLE.getValue();
        } else {
            return ZorroReturnValues.ASSET_UNAVAILABLE.getValue();
        }
    }

    public PriceDetails getPriceDetails(Epic epic) {
        return priceDataMap.get(epic);
    }
}
