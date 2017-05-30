package com.danlind.igz.brokerapi;

import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.PriceDetails;
import com.danlind.igz.handler.AssetHandler;
import com.danlind.igz.misc.MarketDataProvider;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.Direction;
import net.openhft.chronicle.map.ChronicleMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class BrokerTrade {

    private final static Logger LOG = LoggerFactory.getLogger(BrokerTrade.class);
    private final static double rollOverNotSupported = 0.0;
    private final ChronicleMap<Integer, OrderDetails> orderReferenceMap;
    private final AssetHandler assetHandler;
    private final MarketDataProvider marketDataProvider;
    private final RestApiAdapter restApiAdapter;

    public BrokerTrade(ChronicleMap<Integer, OrderDetails> orderReferenceMap, AssetHandler assetHandler, MarketDataProvider marketDataProvider, RestApiAdapter restApiAdapter) {
        this.orderReferenceMap = orderReferenceMap;
        this.assetHandler = assetHandler;
        this.marketDataProvider = marketDataProvider;
        this.restApiAdapter = restApiAdapter;
    }

    public int getTradeStatus(final int nTradeID,
                              final double[] orderParams) {
        OrderDetails orderDetails = orderReferenceMap.get(nTradeID);
        if (Objects.isNull(orderDetails)) {
            return ZorroReturnValues.UNKNOWN_ORDER_ID.getValue();
        } else {
            return fillTradeParams(orderParams, orderDetails);
        }
    }

    public void checkPositionsValid() {
        if (orderReferenceMap.size() > 0) {
            LOG.info("Checking if {} previously opened positions are still open", orderReferenceMap.size());

            orderReferenceMap.entrySet().stream()
                .filter((entry) -> !restApiAdapter.getPositionStatus(entry.getValue().getDealId()))
                .forEach(missingEntry -> orderReferenceMap.remove(missingEntry.getKey()));
        }
    }

    private int fillTradeParams(double[] orderParams, OrderDetails orderDetails) {
        ContractDetails contractDetails = marketDataProvider.getContractDetails(orderDetails.getEpic());
        PriceDetails priceDetails = assetHandler.getAssetDetails(orderDetails.getEpic());

        final double pOpen = orderDetails.getEntryLevel();
        final double pClose = (orderDetails.getDirection() == Direction.BUY)
            ? priceDetails.getBid()
            : priceDetails.getAsk();
        final double pRoll = rollOverNotSupported;
        final double pProfit = (orderDetails.getDirection() == Direction.BUY)
            ? calculateProfit(orderDetails, contractDetails, pClose)
            : calculateProfit(orderDetails, contractDetails, pClose) * -1;
        orderParams[0] = pOpen;
        orderParams[1] = pClose;
        orderParams[2] = pRoll;
        orderParams[3] = pProfit;

        return orderDetails.getPositionSize() * (int) contractDetails.getLotAmount();
    }

    private double calculateProfit(OrderDetails orderDetails, ContractDetails contractDetails, double pClose) {
        return (pClose - orderDetails.getEntryLevel())  * orderDetails.getPositionSize() * contractDetails.getScalingFactor() * contractDetails.getPipCost();
    }
}
