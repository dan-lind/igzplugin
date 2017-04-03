package com.danlind.igz.handler;

import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.AccountDetails;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.StreamingAPI;
import com.danlind.igz.ig.api.client.rest.dto.getAccountsV1.AccountsItem;
import com.danlind.igz.ig.api.client.rest.dto.getAccountsV1.Balance;
import com.danlind.igz.ig.api.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class AccountHandler {

    private final static Logger logger = LoggerFactory.getLogger(AccountHandler.class);
    private final RestAPI restAPI;
    private final LoginHandler loginHandler;
    private final StreamingAPI streamingAPI;
    private final ArrayList<HandyTableListenerAdapter> listeners;


    private AccountDetails accountDetails;

    @Autowired
    public AccountHandler(RestAPI restAPI, LoginHandler loginHandler, StreamingAPI streamingAPI, ArrayList<HandyTableListenerAdapter> listeners) {
        this.restAPI = restAPI;
        this.loginHandler = loginHandler;
        this.streamingAPI = streamingAPI;
        this.listeners = listeners;
    }

    public void startAccountSubscription() {

        try {
            listeners.add(streamingAPI.subscribeForAccountBalanceInfo(loginHandler.getAccountId(), new HandyTableListenerAdapter() {
                @Override
                public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                    accountDetails = new AccountDetails(
                            Double.parseDouble(updateInfo.getNewValue(0)),
                            Double.parseDouble(updateInfo.getNewValue(1)),
                            Double.parseDouble(updateInfo.getNewValue(2)));
                    logger.debug("Received Account update from Lightstreamer for AccountID {} with data {}", s, updateInfo);
                }
            }));

        } catch (Exception e) {
            logger.error("Failed when subscribing to account updates", e);
        }
    }

    public int brokerAccount(final double accountInfoParams[]) {

        if (Objects.isNull(accountDetails)) {
            try {
                String accountId = loginHandler.getAccountId();
                List<AccountsItem> accountsItemList = restAPI.getAccountsV1(loginHandler.getConversationContext()).getAccounts();
                Balance balance = accountsItemList.stream().filter(account -> account.getAccountId().equals(accountId)).findFirst().get().getBalance();
                accountDetails = new AccountDetails(balance.getBalance(), balance.getProfitLoss(), balance.getDeposit());
            } catch (Exception e) {
                logger.error("Failed when getting broker account info", e);
                return ZorroReturnValues.ACCOUNT_UNAVAILABLE.getValue();
            }
        }

        accountInfoParams[0] = accountDetails.getBalance();
        accountInfoParams[1] = accountDetails.getProfitLoss();
        accountInfoParams[2] = accountDetails.getMarginValue();

//        Logging BrokerAccount calls will produce A LOT of log output
//        logger.debug("BrokerAccount fill params: \n"
//                + "balance: {}\n"
//                + "ProfitLoss: {}\n"
//                + "Deposit: {}\n", accountInfoParams[0], accountInfoParams[1], accountInfoParams[2]);

        return ZorroReturnValues.ACCOUNT_AVAILABLE.getValue();
    }

}
