package com.danlind.igz.ig.api.client.rest.dto.watchlists.deleteWatchlistMarketV1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*

*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteWatchlistMarketV1Response {

/*
Status
*/
private Status status;

public Status getStatus() { return status; }
public void setStatus(Status status) { this.status=status; }
}
