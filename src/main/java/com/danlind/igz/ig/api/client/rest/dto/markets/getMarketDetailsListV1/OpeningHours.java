package com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsListV1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
Market open and close times
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpeningHours {

/*
List of market open and close times
*/
private java.util.List<MarketTimesItem> marketTimes;

public java.util.List<MarketTimesItem> getMarketTimes() { return marketTimes; }
public void setMarketTimes(java.util.List<MarketTimesItem> marketTimes) { this.marketTimes=marketTimes; }
}
