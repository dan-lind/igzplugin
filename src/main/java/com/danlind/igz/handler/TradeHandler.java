package com.danlind.igz.handler;

import com.danlind.igz.brokerapi.BrokerBuy;
import com.danlind.igz.brokerapi.BrokerSell;
import com.danlind.igz.brokerapi.BrokerStop;
import com.danlind.igz.brokerapi.BrokerTrade;
import com.danlind.igz.domain.types.Epic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TradeHandler {

    private final static Logger logger = LoggerFactory.getLogger(TradeHandler.class);
    private final BrokerBuy brokerBuy;
    private final BrokerSell brokerSell;
    private final BrokerTrade brokerTrade;
    private final BrokerStop brokerStop;

    @Autowired
    public TradeHandler(BrokerBuy brokerBuy, BrokerSell brokerSell, BrokerTrade brokerTrade, BrokerStop brokerStop) {
        this.brokerStop = brokerStop;
        this.brokerBuy = brokerBuy;
        this.brokerSell = brokerSell;
        this.brokerTrade = brokerTrade;
    }

    public int brokerBuy(final Epic epic, final double[] tradeParams) {
        return brokerBuy.createPosition(epic, tradeParams);
    }

    public int brokerTrade(final int nTradeID, final double[] orderParams) {
        return brokerTrade.getTradeStatus(nTradeID, orderParams);
    }

    public int brokerSell(final int nOrderId, final int nAmount) {
        return brokerSell.closePosition(nOrderId, nAmount);
    }

    public int brokerStop(final int orderId, final double newSLPrice) {
        return brokerStop.updateStop(orderId, newSLPrice);
    }

    public void checkTradesValid() {
        brokerTrade.checkPositionsValid();
    }
}
