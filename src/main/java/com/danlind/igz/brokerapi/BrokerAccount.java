package com.danlind.igz.brokerapi;

import com.danlind.igz.config.ZorroReturnValues;

import com.danlind.igz.misc.AccountInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerAccount {

//    private final AccountInfo accountInfo;
//
//    private final static Logger logger = LoggerFactory.getLogger(BrokerBuy.class);
//
//    public BrokerAccount(final AccountInfo accountInfo) {
//        this.accountInfo = accountInfo;
//    }
//
//    public int handle(final double accountInfoParams[]) {
//        if (!accountInfo.isConnected())
//            return ZorroReturnValues.ACCOUNT_UNAVAILABLE.getValue();
//
//        fillAccountParams(accountInfoParams);
//        return ZorroReturnValues.ACCOUNT_AVAILABLE.getValue();
//    }
//
//    private void fillAccountParams(final double accountInfoParams[]) {
//        accountInfoParams[0] = accountInfo.baseEquity();
//        accountInfoParams[1] = accountInfo.tradeValue();
//        accountInfoParams[2] = accountInfo.usedMargin();
//
//        logger.trace("BrokerAccount fill params: \n"
//                + "baseEquity:  " + accountInfoParams[0] + "\n"
//                + "tradeValue:  " + accountInfoParams[1] + "\n"
//                + "usedMargin:  " + accountInfoParams[2] + "\n");
//    }
}
