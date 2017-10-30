package com.danlind.igz.brokerapi;

import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.domain.types.DealReference;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.domain.types.OrderText;
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
import java.util.regex.Pattern;

public abstract class BrokerOrder {

    private final RestApiAdapter restApiAdapter;
    private final ChronicleMap<Integer, OrderDetails> orderReferenceMap;

    private OrderText orderText;

    public BrokerOrder(RestApiAdapter restApiAdapter, ChronicleMap<Integer, OrderDetails> orderReferenceMap) {
        this.restApiAdapter = restApiAdapter;
        this.orderReferenceMap = orderReferenceMap;
    }

    protected Single<Optional<GetDealConfirmationV1Response>> getDealConfirmation(DealReference dealReference) {
        return restApiAdapter.getDealConfirmation(dealReference.getValue());
    }

    protected int getNextOrderId() {
        return orderReferenceMap.keySet().stream().max(Integer::compareTo).orElse(1000) + 1;
    }
}
