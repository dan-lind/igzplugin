package com.danlind.igz.ig.api.client.rest.dto.positions.getPositionsV1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
Open position data
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class PositionsItem {

/*
Open position data
*/
private Position position;

/*
Market data
*/
private Market market;

public Position getPosition() { return position; }
public void setPosition(Position position) { this.position=position; }
public Market getMarket() { return market; }
public void setMarket(Market market) { this.market=market; }
}
