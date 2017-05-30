package com.danlind.igz.brokerapi;

import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.PositionStatus;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.closeOTCPositionV1.CloseOTCPositionV1Request;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.closeOTCPositionV1.Direction;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.closeOTCPositionV1.OrderType;
import com.danlind.igz.misc.MarketDataProvider;
import com.danlind.igz.misc.RetryWithDelay;
import net.openhft.chronicle.map.ChronicleMap;
import org.apache.http.annotation.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BrokerSell {

    private final static Logger LOG = LoggerFactory.getLogger(BrokerSell.class);
    private final RestApiAdapter restApiAdapter;
    private final ChronicleMap<Integer, OrderDetails> orderReferenceMap;
    private final AtomicInteger atomicInteger;
    private final MarketDataProvider marketDataProvider;

    public BrokerSell(RestApiAdapter restApiAdapter, ChronicleMap<Integer, OrderDetails> orderReferenceMap, AtomicInteger atomicInteger, MarketDataProvider marketDataProvider) {
        this.restApiAdapter = restApiAdapter;
        this.orderReferenceMap = orderReferenceMap;
        this.atomicInteger = atomicInteger;
        this.marketDataProvider = marketDataProvider;
    }

//TODO: Update to handle correct contract size
    public int closePosition(final int nOrderId,
                             final int nAmount) {
        OrderDetails orderDetails = orderReferenceMap.get(nOrderId);
        DealId dealId = orderDetails.getDealId();
        ContractDetails contractDetails = marketDataProvider.getContractDetails(orderDetails.getEpic());
        int lotSize = nAmount/(int) contractDetails.getLotAmount();
        CloseOTCPositionV1Request request = createClosePositionRequest(lotSize, dealId);

        LOG.info(">>> Closing size {} for position with dealId {}", lotSize, dealId.getValue() );

        return restApiAdapter.closePosition(request)
            .doOnNext(dealReference -> LOG.debug("Got dealReference {} when attempting to close position with dealId {}", dealReference.getValue(), dealId.getValue()))
            .delay(500, TimeUnit.MILLISECONDS)
            .flatMap(dealReference -> restApiAdapter.getDealConfirmationObservable(dealReference.getValue())
                .retryWhen(new RetryWithDelay(3, 1500))
                .map(dealConfirmationResponse -> closeConfirmationHandler(dealConfirmationResponse, nOrderId, lotSize))
            )
            .onErrorReturn(e -> ZorroReturnValues.BROKER_SELL_FAIL.getValue())
            .blockingSingle();
    }

    private int closeConfirmationHandler(GetDealConfirmationV1Response dealConfirmationResponse, int nOrderId, int lotSize ) {
        OrderDetails sellOrderDetails = orderReferenceMap.get(nOrderId);

        //TODO: Need to log PositionStatus. Some trades are marked as partially closed, when they are in fact fully closed
        LOG.debug("Position status is {}", dealConfirmationResponse.getStatus());
        if (dealConfirmationResponse.getStatus() == PositionStatus.CLOSED) {
            LOG.debug("Position with deal id {} now fully closed", sellOrderDetails.getDealId().getValue());
            orderReferenceMap.remove(nOrderId);
            return nOrderId;
        } else {
            LOG.debug("Position with deal id {} now partially closed", sellOrderDetails.getDealId().getValue());
            int newOrderId = atomicInteger.getAndIncrement();
            orderReferenceMap.put(newOrderId,
                new OrderDetails(sellOrderDetails.getEpic(),
                    sellOrderDetails.getEntryLevel(),
                    sellOrderDetails.getDirection(),
                    sellOrderDetails.getPositionSize() - Math.abs(lotSize),
                    new DealId(dealConfirmationResponse.getDealId())));
            return newOrderId;
        }
    }

    @NotNull
    private CloseOTCPositionV1Request createClosePositionRequest(int lotSize, DealId dealId) {
        CloseOTCPositionV1Request request = new CloseOTCPositionV1Request();
        request.setDealId(dealId.getValue());
        request.setSize(BigDecimal.valueOf(Math.abs(lotSize)));
        if (lotSize > 0) {
            request.setDirection(Direction.SELL);
        } else {
            request.setDirection(Direction.BUY);
        }
        request.setOrderType(OrderType.MARKET);
        return request;
    }
}
