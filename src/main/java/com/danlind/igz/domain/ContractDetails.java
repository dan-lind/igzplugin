package com.danlind.igz.domain;

import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.GetMarketDetailsV3Response;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.MarketStatus;

import java.util.Objects;

/**
 * Created by danlin on 2017-03-21.
 */
public class ContractDetails {
    private final Epic epic;
    private final double pipSize;
    private final double pipCost;
    private final double lotAmount;
    private final double marginCost;
    private final double snapshotBid;
    private final double snapshotAsk;
    private final String expiry;
    private final String currencyCode;
    private final int scalingFactor;
    private final MarketStatus marketStatus;

    public ContractDetails(Epic epic, double pipSize, double pipCost, double lotAmount, double marginCost, double snapshotBid, double snapshotAsk, String expiry, String currencyCode, int scalingFactor, MarketStatus marketStatus) {
        Objects.requireNonNull(epic);
        Objects.requireNonNull(marketStatus);
        Objects.requireNonNull(expiry);
        Objects.requireNonNull(currencyCode);
        this.currencyCode = currencyCode;
        this.expiry = expiry;
        this.scalingFactor = scalingFactor;
        this.snapshotBid = snapshotBid;
        this.snapshotAsk = snapshotAsk;
        this.epic = epic;
        this.pipSize = pipSize;
        this.pipCost = pipCost;
        this.lotAmount = lotAmount;
        this.marginCost = marginCost;
        this.marketStatus = marketStatus;
    }

    public static ContractDetails createContractDetailsFromResponse(GetMarketDetailsV3Response marketDetails) {
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

    public Epic getEpic() {
        return epic;
    }

    public double getPipSize() {
        return pipSize;
    }

    public double getPipCost() {
        return pipCost;
    }

    public double getLotAmount() {
        return lotAmount;
    }

    public double getMarginCost() {
        return marginCost;
    }

    public MarketStatus getMarketStatus() {
        return marketStatus;
    }

    public double getSnapshotBid() {
        return snapshotBid;
    }

    public double getSnapshotAsk() {
        return snapshotAsk;
    }

    public int getScalingFactor() {
        return scalingFactor;
    }

    public String getExpiry() {
        return expiry;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }
}
