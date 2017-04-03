package com.danlind.igz.ig.api.client.rest.dto.watchlists.updateWatchlistMarketV1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
Add instrument to watchlist request
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateWatchlistMarketV1Request {

/*
Instrument epic identifier
*/
private String epic;

public String getEpic() { return epic; }
public void setEpic(String epic) { this.epic=epic; }
}
