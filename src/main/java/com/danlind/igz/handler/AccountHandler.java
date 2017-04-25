package com.danlind.igz.handler;

import com.danlind.igz.brokerapi.BrokerAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountHandler {

    private final BrokerAccount brokerAccount;

    @Autowired
    public AccountHandler(BrokerAccount brokerAccount) {
        this.brokerAccount = brokerAccount;
    }

    public void startAccountSubscription() {
        brokerAccount.startAccountSubscription();
    }

    public int brokerAccount(final double accountInfoParams[]) {
        return brokerAccount.fillAccountParams(accountInfoParams);
    }

}
