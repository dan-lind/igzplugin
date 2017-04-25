package com.danlind.igz.handler;


import com.danlind.igz.brokerapi.BrokerHistory;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.ig.api.client.rest.dto.prices.getPricesV3.PricesItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class HistoryHandler {

    private final BrokerHistory brokerHistory;

    @Autowired
    public HistoryHandler(BrokerHistory brokerHistory) {
        this.brokerHistory = brokerHistory;
    }

    public int getPriceHistory(final Epic epic,
                               final double tStart,
                               final double tEnd,
                               final int nTickMinutes,
                               final int nTicks,
                               final double tickParams[]) {
        return brokerHistory.getPriceHistory(epic, tStart, tEnd,nTickMinutes, nTicks, tickParams);
    }

    public List<PricesItem> getPriceHistory(final Epic epic,
                                            final int ticks) {
        return brokerHistory.getPriceHistory(epic, ticks);
    }
}
