package com.danlind.igz.ig.api.client.rest.dto.prices.getPricesByNumberOfPointsV2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/*
Price
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClosePrice {

/*
Bid price
*/
private BigDecimal bid;

/*
Ask price
*/
private BigDecimal ask;

/*
Last traded price.  This will generally be null for non exchange-traded instruments
*/
private BigDecimal lastTraded;

public BigDecimal getBid() { return bid; }
public void setBid(BigDecimal bid) { this.bid=bid; }
public BigDecimal getAsk() { return ask; }
public void setAsk(BigDecimal ask) { this.ask=ask; }
public BigDecimal getLastTraded() { return lastTraded; }
public void setLastTraded(BigDecimal lastTraded) { this.lastTraded=lastTraded; }
}
