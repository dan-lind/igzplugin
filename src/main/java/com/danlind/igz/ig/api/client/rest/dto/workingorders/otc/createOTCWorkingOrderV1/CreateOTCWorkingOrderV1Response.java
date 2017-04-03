package com.danlind.igz.ig.api.client.rest.dto.workingorders.otc.createOTCWorkingOrderV1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
Create working order response
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOTCWorkingOrderV1Response {

/*
Deal reference of the transaction
*/
private String dealReference;

public String getDealReference() { return dealReference; }
public void setDealReference(String dealReference) { this.dealReference=dealReference; }
}
