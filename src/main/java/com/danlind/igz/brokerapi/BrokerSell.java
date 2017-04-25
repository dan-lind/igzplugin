package com.danlind.igz.brokerapi;

import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.PositionStatus;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.closeOTCPositionV1.CloseOTCPositionV1Request;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.closeOTCPositionV1.Direction;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.closeOTCPositionV1.OrderType;
import net.openhft.chronicle.map.ChronicleMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BrokerSell {

    private final static Logger LOG = LoggerFactory.getLogger(BrokerSell.class);
    private final RestApiAdapter restApiAdapter;
    private final ChronicleMap<Integer, OrderDetails> orderReferenceMap;
    private final AtomicInteger atomicInteger;

    public BrokerSell(RestApiAdapter restApiAdapter, ChronicleMap<Integer, OrderDetails> orderReferenceMap, AtomicInteger atomicInteger) {
        this.restApiAdapter = restApiAdapter;
        this.orderReferenceMap = orderReferenceMap;
        this.atomicInteger = atomicInteger;
    }


    public int closePosition(final int nOrderId,
                             final int nAmount) {
        DealId dealId = orderReferenceMap.get(nOrderId).getDealId();
        CloseOTCPositionV1Request request = createClosePositionRequest(nAmount, dealId);

        return restApiAdapter.closePosition(request)
            .flatMap(dealReference -> restApiAdapter.getDealConfirmationObservable(dealReference.getValue())
                .map(dealConfirmationResponse -> closeConfirmationHandler(dealConfirmationResponse, nOrderId, nAmount))
            )
            .onErrorReturn(e -> ZorroReturnValues.BROKER_SELL_FAIL.getValue())
            .blockingSingle();
    }

    private int closeConfirmationHandler(GetDealConfirmationV1Response dealConfirmationResponse, int nOrderId, int nAmount ) {
        OrderDetails sellOrderDetails = orderReferenceMap.get(nOrderId);
        if (dealConfirmationResponse.getStatus() == PositionStatus.CLOSED) {
            LOG.debug("Position with deal id {} now fully closed", sellOrderDetails.getDealId());
            orderReferenceMap.remove(nOrderId);
            return nOrderId;
        } else {
            LOG.debug("Position with deal id {} now partially closed", sellOrderDetails.getDealId());
            int newOrderId = atomicInteger.getAndIncrement();
            orderReferenceMap.put(newOrderId,
                new OrderDetails(sellOrderDetails.getEpic(),
                    sellOrderDetails.getEntryLevel(),
                    sellOrderDetails.getDirection(),
                    sellOrderDetails.getPositionSize() - Math.abs(nAmount),
                    new DealId(dealConfirmationResponse.getDealId())));
            return newOrderId;
        }
    }

    @NotNull
    private CloseOTCPositionV1Request createClosePositionRequest(int nAmount, DealId dealId) {
        CloseOTCPositionV1Request request = new CloseOTCPositionV1Request();
        request.setDealId(dealId.getValue());
        request.setSize(BigDecimal.valueOf(Math.abs(nAmount)));
        if (nAmount > 0) {
            request.setDirection(Direction.SELL);
        } else {
            request.setDirection(Direction.BUY);
        }
        request.setOrderType(OrderType.MARKET);
        return request;
    }
}
