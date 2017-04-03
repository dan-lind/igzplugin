package com.danlind.igz.ig.api.client.rest.dto.workingorders.otc.deleteOTCWorkingOrderV2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/*
Delete working order response
*/
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteOTCWorkingOrderV2Response {

/*
Deal reference of the transaction
*/
private String dealReference;

public String getDealReference() { return dealReference; }
public void setDealReference(String dealReference) { this.dealReference=dealReference; }
}
