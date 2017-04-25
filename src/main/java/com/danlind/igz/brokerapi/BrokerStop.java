package com.danlind.igz.brokerapi;

import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.updateOTCPositionV2.UpdateOTCPositionV2Request;
import net.openhft.chronicle.map.ChronicleMap;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BrokerStop {

    private final RestApiAdapter restApiAdapter;
    private final ChronicleMap<Integer, OrderDetails> orderReferenceMap;

    public BrokerStop(RestApiAdapter restApiAdapter, ChronicleMap<Integer, OrderDetails> orderReferenceMap) {
        this.restApiAdapter = restApiAdapter;
        this.orderReferenceMap = orderReferenceMap;
    }

    public int updateStop(final int orderId,
                          final double newSLPrice) {
        UpdateOTCPositionV2Request request = new UpdateOTCPositionV2Request();
        request.setStopLevel(BigDecimal.valueOf(newSLPrice));
        request.setTrailingStop(false);
        DealId dealId = orderReferenceMap.get(orderId).getDealId();

        return restApiAdapter.getUpdateStopObservable(dealId.getValue(), request)
            .flatMap(dealReference -> restApiAdapter.getDealConfirmationObservable(dealReference.getValue())
                .map(event -> ZorroReturnValues.ADJUST_SL_OK.getValue())
            )
            .onErrorReturn(e -> ZorroReturnValues.ADJUST_SL_FAIL.getValue())
            .blockingSingle();
    }
}
