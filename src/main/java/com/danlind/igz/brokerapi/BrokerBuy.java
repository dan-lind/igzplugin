package com.danlind.igz.brokerapi;

import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.OrderText;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.domain.types.DealReference;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.CreateOTCPositionV2Request;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.Direction;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.OrderType;
import com.danlind.igz.misc.MarketDataProvider;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import net.openhft.chronicle.map.ChronicleMap;
import org.apache.commons.lang.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Component
public class BrokerBuy {

    private final static Logger LOG = LoggerFactory.getLogger(BrokerBuy.class);
    private final RestApiAdapter restApiAdapter;
    private final MarketDataProvider marketDataProvider;
    private final ChronicleMap<Integer, OrderDetails> orderReferenceMap;
    private final AtomicInteger atomicInteger;

    private OrderText orderText;

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
            .subscribeOn(Schedulers.io())
            .doOnSuccess(dealReference -> LOG.debug("Got dealReference {} when attempting to open position", dealReference.getValue()))
            .delay(500, TimeUnit.MILLISECONDS)
            .flatMap(this::getDealConfirmation)
                .flatMap(dealConfirmationResponse -> buyConfirmationHandler(dealConfirmationResponse, createPositionRequest.getDirection(), tradeParams))
            .onErrorReturn(e -> ZorroReturnValues.BROKER_BUY_FAIL.getValue())
            .blockingGet();
    }

    private Single<Optional<GetDealConfirmationV1Response>> getDealConfirmation(DealReference dealReference) {
        return restApiAdapter.getDealConfirmation(dealReference.getValue());
    }

    private Single<Integer> buyConfirmationHandler(Optional<GetDealConfirmationV1Response> maybeDealConfirmationResponse, Direction direction, double[] tradeParams) {
        if (maybeDealConfirmationResponse.isPresent()) {
            GetDealConfirmationV1Response dealConfirmationResponse = maybeDealConfirmationResponse.get();
            int orderId = atomicInteger.getAndIncrement();
            LOG.debug("Storing open position with orderId {} and dealId {}", orderId, dealConfirmationResponse.getDealId());
            orderReferenceMap.put(orderId, new OrderDetails(new Epic(dealConfirmationResponse.getEpic()), dealConfirmationResponse.getLevel(), direction, dealConfirmationResponse.getSize().intValue(), new DealId(dealConfirmationResponse.getDealId())));
            tradeParams[2] = dealConfirmationResponse.getLevel();
            return Single.just(orderId);
        } else {
            return Single.just(ZorroReturnValues.BROKER_BUY_FAIL.getValue());
        }

    }

    @NotNull
    private CreateOTCPositionV2Request createPositionRequest(Epic epic, double numberOfContracts, double stopDistance) {
        ContractDetails contractDetails = marketDataProvider.getContractDetails(epic);
        CreateOTCPositionV2Request createPositionRequest = new CreateOTCPositionV2Request();
        createPositionRequest.setEpic(epic.getName());
        createPositionRequest.setExpiry(contractDetails.getExpiry());
        createPositionRequest.setOrderType(OrderType.MARKET);
        createPositionRequest.setCurrencyCode(contractDetails.getCurrencyCode());

        createPositionRequest.setDealReference(generateDealReference());

        if (contractDetails.getLotAmount() >= 1) {
            createPositionRequest.setSize(BigDecimal.valueOf(Math.abs(numberOfContracts / contractDetails.getLotAmount())));
        } else {
            createPositionRequest.setSize(BigDecimal.valueOf(Math.abs(numberOfContracts)));
        }


        createPositionRequest.setGuaranteedStop(false);
        createPositionRequest.setForceOpen(true);

        if (numberOfContracts > 0) {
            createPositionRequest.setDirection(Direction.BUY);
        } else {
            createPositionRequest.setDirection(Direction.SELL);
        }

        if (stopDistance != 0) {
            createPositionRequest.setStopDistance(BigDecimal.valueOf(stopDistance * contractDetails.getScalingFactor()));
        }

        LOG.info(">>> Creating position for epic={} with \ndirection={}, \nexpiry={}, \nsize={}, \norderType={}, \ncurrency={}, \nstop loss distance={}",
            epic.getName(), createPositionRequest.getDirection(), createPositionRequest.getExpiry(),
            createPositionRequest.getSize(), createPositionRequest.getOrderType(), createPositionRequest.getCurrencyCode(), stopDistance);
        return createPositionRequest;
    }

    private String generateDealReference() {
        String dealReference = RandomStringUtils.randomAlphabetic(10);
        if (Objects.isNull(getOrderText())) {
            return dealReference;
        } else {
            return getOrderText().getValue() + "-" + dealReference;
        }
    }

    public OrderText getOrderText() {
        return orderText;
    }

    public int setOrderText(OrderText orderText) {
        Pattern p = Pattern.compile("[^A-Za-z0-9_-]");
        if (!p.matcher(orderText.getValue()).find() && orderText.getValue().length() <= 19) {
            this.orderText = orderText;
            return ZorroReturnValues.BROKER_COMMAND_OK.getValue();
        } else {
            LOG.warn("Order text must be alpha numeric and max 19 chars long: {}", orderText.getValue());
            return ZorroReturnValues.BROKER_COMMAND_FAIL.getValue();
        }

    }
}
