package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.misc.MarketDataProvider;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.CreateOTCPositionV2Request;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.Direction;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.OrderType;
import com.danlind.igz.misc.RetryWithDelay;
import net.openhft.chronicle.map.ChronicleMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BrokerBuy {

    private final static Logger LOG = LoggerFactory.getLogger(BrokerBuy.class);
    private final RestApiAdapter restApiAdapter;
    private final MarketDataProvider marketDataProvider;
    private final ChronicleMap<Integer, OrderDetails> orderReferenceMap;
    private final AtomicInteger atomicInteger;

    public BrokerBuy(RestApiAdapter restApiAdapter, MarketDataProvider marketDataProvider, ChronicleMap<Integer, OrderDetails> orderReferenceMap, AtomicInteger atomicInteger) {
        this.restApiAdapter = restApiAdapter;
        this.marketDataProvider = marketDataProvider;
        this.orderReferenceMap = orderReferenceMap;
        this.atomicInteger = atomicInteger;
    }

    public int createPosition(final Epic epic,
                              final double tradeParams[]) {
        double numberOfContracts = tradeParams[0];
        double stopDistance = tradeParams[1];
        CreateOTCPositionV2Request createPositionRequest = createPositionRequest(epic, numberOfContracts, stopDistance);

        return restApiAdapter.createPosition(createPositionRequest)
            .doOnNext(dealReference -> LOG.debug("Got dealReference {} when attempting to open position", dealReference.getValue()))
            .delay(1500, TimeUnit.MILLISECONDS)
            .flatMap(dealReference -> restApiAdapter.getDealConfirmationObservable(dealReference.getValue())
                .retryWhen(new RetryWithDelay(3, 1500))
                .map(dealConfirmationResponse -> buyConfirmationHandler(dealConfirmationResponse, createPositionRequest.getDirection(), tradeParams))
            )
            .doOnError(e -> Zorro.indicateError())
            .onErrorReturn(e -> ZorroReturnValues.BROKER_BUY_FAIL.getValue())
            .blockingSingle();
    }

    private int buyConfirmationHandler(GetDealConfirmationV1Response dealConfirmationResponse, Direction direction, double[] tradeParams) {
        int orderId = atomicInteger.getAndIncrement();
        LOG.debug("Storing open position with orderId {} and dealId {}", orderId, dealConfirmationResponse.getDealId());
        orderReferenceMap.put(orderId, new OrderDetails(new Epic(dealConfirmationResponse.getEpic()), dealConfirmationResponse.getLevel(), direction, dealConfirmationResponse.getSize().intValue(), new DealId(dealConfirmationResponse.getDealId())));
        tradeParams[2] = dealConfirmationResponse.getLevel();
        return orderId;
    }

    @NotNull
    private CreateOTCPositionV2Request createPositionRequest(Epic epic, double numberOfContracts, double stopDistance) {
        ContractDetails contractDetails = marketDataProvider.getContractDetails(epic);
        CreateOTCPositionV2Request createPositionRequest = new CreateOTCPositionV2Request();
        createPositionRequest.setEpic(epic.getName());
        createPositionRequest.setExpiry(contractDetails.getExpiry());
        createPositionRequest.setOrderType(OrderType.MARKET);
        createPositionRequest.setCurrencyCode(contractDetails.getCurrencyCode());
        createPositionRequest.setSize(BigDecimal.valueOf(Math.abs(numberOfContracts/contractDetails.getLotAmount())));
        createPositionRequest.setGuaranteedStop(false);
        createPositionRequest.setForceOpen(true);

        if (numberOfContracts > 0) {
            createPositionRequest.setDirection(Direction.BUY);
        } else {
            createPositionRequest.setDirection(Direction.SELL);
        }

        //TODO: Check if we should really use scalingFactor here
        if (stopDistance != 0) {
            createPositionRequest.setStopDistance(BigDecimal.valueOf(stopDistance));
        }

        LOG.info(">>> Creating long position epic={}, \ndirection={}, \nexpiry={}, \nsize={}, \norderType={}, \ncurrency={}, \nstop loss distance={}",
            epic.getName(), createPositionRequest.getDirection(), createPositionRequest.getExpiry(),
            createPositionRequest.getSize(), createPositionRequest.getOrderType(), createPositionRequest.getCurrencyCode(), stopDistance);
        return createPositionRequest;
    }
}
