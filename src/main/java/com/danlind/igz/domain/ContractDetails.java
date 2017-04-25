package com.danlind.igz.domain;

import com.danlind.igz.domain.types.Epic;
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
    private final double baseExchangeRate;
    private final double snapshotBid;
    private final double snapshotAsk;
    private final String expiry;
    private final String currencyCode;
    private final int scalingFactor;
    private final MarketStatus marketStatus;

    public ContractDetails(Epic epic, double pipSize, double pipCost, double lotAmount, double marginCost, double baseExchangeRate, double snapshotBid, double snapshotAsk, String expiry, String currencyCode, int scalingFactor, MarketStatus marketStatus) {
        Objects.requireNonNull(epic);
        Objects.requireNonNull(marketStatus);
        Objects.requireNonNull(expiry);
        Objects.requireNonNull(currencyCode);
        this.currencyCode = currencyCode;
        this.expiry = expiry;
        this.scalingFactor = scalingFactor;
        this.snapshotBid = snapshotBid;
        this.snapshotAsk = snapshotAsk;
        this.baseExchangeRate = baseExchangeRate;
        this.epic = epic;
        this.pipSize = pipSize;
        this.pipCost = pipCost;
        this.lotAmount = lotAmount;
        this.marginCost = marginCost;
        this.marketStatus = marketStatus;
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

    public double getPipCostInAccountCurrency() {
        return pipCost/baseExchangeRate;
    }

    public double getLotAmount() {
        return lotAmount;
    }

    public double getMarginCost() {
        return marginCost;
    }

    public double getMarginCostInAccountCurrency() {
        return marginCost/baseExchangeRate;
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

    public double getBaseExchangeRate() {
        return baseExchangeRate;
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
