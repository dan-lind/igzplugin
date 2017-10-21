package com.danlind.igz.brokerapi;

import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.domain.types.DealReference;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.updateOTCPositionV2.UpdateOTCPositionV2Request;
import io.reactivex.Single;
import net.openhft.chronicle.map.ChronicleMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class BrokerStop {

    private final static Logger LOG = LoggerFactory.getLogger(BrokerStop.class);
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

        LOG.debug("Attempting to update stop for dealId {} to {}", dealId.getValue(), newSLPrice);

        return restApiAdapter.updateStop(dealId.getValue(), request)
            .doOnSuccess(dealReference -> LOG.debug("Got dealReference {} when attempting to update stop for dealId", dealReference.getValue(), dealId.getValue()))
            .delay(500, TimeUnit.MILLISECONDS)
            .flatMap(this::getDealConfirmation)
                .flatMap(event ->  event.isPresent() ? Single.just(ZorroReturnValues.ADJUST_SL_OK.getValue()) : Single.just(ZorroReturnValues.ADJUST_SL_FAIL.getValue())
            )
            .onErrorReturn(e -> ZorroReturnValues.ADJUST_SL_FAIL.getValue())
            .blockingGet();
    }

    private Single<Optional<GetDealConfirmationV1Response>> getDealConfirmation(DealReference dealReference) {
        return restApiAdapter.getDealConfirmation(dealReference.getValue());
    }

}
