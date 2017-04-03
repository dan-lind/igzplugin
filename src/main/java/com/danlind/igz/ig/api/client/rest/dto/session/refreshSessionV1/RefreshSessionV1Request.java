package com.danlind.igz.ig.api.client.rest.dto.session.refreshSessionV1;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshSessionV1Request {

   private String refresh_token;
}
