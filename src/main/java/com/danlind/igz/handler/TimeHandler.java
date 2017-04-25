package com.danlind.igz.handler;

import com.danlind.igz.brokerapi.BrokerTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TimeHandler {

    private final BrokerTime brokerTime;

    @Autowired
    public TimeHandler(BrokerTime brokerTime) {
        this.brokerTime = brokerTime;
    }


    public void subscribeToLighstreamerHeartbeat() {
        brokerTime.subscribeToLighstreamerHeartbeat();
    }

    public int getBrokerTime(final double pTimeUTC[]) {
        return brokerTime.getBrokerTime(pTimeUTC);
    }
}
