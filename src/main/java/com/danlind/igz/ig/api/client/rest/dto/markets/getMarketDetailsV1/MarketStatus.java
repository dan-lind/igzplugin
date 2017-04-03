package com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV1;
/*
Describes the current status of a given market
*/
public enum MarketStatus {

/*
Closed
*/
CLOSED,
/*
Open for edits
*/
EDITS_ONLY,
/*
Offline
*/
OFFLINE,
/*
In auction mode
*/
ON_AUCTION,
/*
In no-edits mode
*/
ON_AUCTION_NO_EDITS,
/*
Suspended
*/
SUSPENDED,
/*
Open for trades
*/
TRADEABLE
}
