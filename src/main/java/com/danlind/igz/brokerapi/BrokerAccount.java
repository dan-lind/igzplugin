package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.adapter.StreamingApiAdapter;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.AccountDetails;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrokerAccount {

    private final static Logger logger = LoggerFactory.getLogger(BrokerAccount.class);
    private final RestApiAdapter restApiAdapter;
    private final StreamingApiAdapter streamingApiAdapter;

    private AccountDetails accountDetails;

    @Autowired
    public BrokerAccount(RestApiAdapter restApiAdapter, StreamingApiAdapter streamingApiAdapter) {
        this.restApiAdapter = restApiAdapter;
        this.streamingApiAdapter = streamingApiAdapter;
    }

    public void startAccountSubscription() {
        streamingApiAdapter.getAccountObservable(restApiAdapter.getAccountId())
            .subscribeOn(Schedulers.io())
            .subscribe(
                this::setAccountDetails,
                error -> {
                    logger.error("Failed when subscribing to account updates", error);
                    Zorro.indicateError();
                }
            );

        accountDetails = restApiAdapter.getAccountDetails(restApiAdapter.getAccountId()).blockingGet();
    }

    private void setAccountDetails(AccountDetails accountDetails) {
        this.accountDetails = accountDetails;
    }

    public int fillAccountParams(final double accountInfoParams[]) {
        accountInfoParams[0] = accountDetails.getBalance();
        accountInfoParams[1] = accountDetails.getProfitLoss();
        accountInfoParams[2] = accountDetails.getMarginValue();

        return ZorroReturnValues.ACCOUNT_AVAILABLE.getValue();
    }
}
