package com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
Market rollover details
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class RolloverDetails {

/*
Last rollover date
*/
private String lastRolloverTime;

/*
Rollover info
*/
private String rolloverInfo;

public String getLastRolloverTime() { return lastRolloverTime; }
public void setLastRolloverTime(String lastRolloverTime) { this.lastRolloverTime=lastRolloverTime; }
public String getRolloverInfo() { return rolloverInfo; }
public void setRolloverInfo(String rolloverInfo) { this.rolloverInfo=rolloverInfo; }
}
