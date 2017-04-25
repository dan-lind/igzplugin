package com.danlind.igz.handler;

import com.danlind.igz.brokerapi.BrokerAsset;
import com.danlind.igz.domain.PriceDetails;
import com.danlind.igz.domain.types.Epic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssetHandler {

    private final BrokerAsset brokerAsset;

    @Autowired
    public AssetHandler(BrokerAsset brokerAsset) {
        this.brokerAsset = brokerAsset;
    }

    public int subscribeToLighstreamerTickUpdates(Epic epic) {
        return brokerAsset.subscribeToLighstreamerTickUpdates(epic);
    }

    public int getLatestAssetData(Epic epic, double assetParams[]) {
        return brokerAsset.getLatestAssetData(epic, assetParams);
    }

    public PriceDetails getAssetDetails(Epic epic) {
        return brokerAsset.getPriceDetails(epic);
    }
}
