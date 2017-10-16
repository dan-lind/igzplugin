package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.adapter.StreamingApiAdapter;
import com.danlind.igz.brokerapi.BrokerAccount;
import com.danlind.igz.brokerapi.BrokerStop;
import com.danlind.igz.domain.AccountDetails;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.handler.LoginHandler;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.dto.getAccountsV1.AccountsItem;
import com.danlind.igz.ig.api.client.rest.dto.getAccountsV1.Balance;
import com.danlind.igz.ig.api.client.rest.dto.getAccountsV1.GetAccountsV1Response;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.DealStatus;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.Reason;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.updateOTCPositionV2.UpdateOTCPositionV2Response;
import io.reactivex.subjects.PublishSubject;
import net.openhft.chronicle.map.ChronicleMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;
import java.util.Collections;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by danlin on 2017-04-08.
 */
@PrepareForTest(Zorro.class)
@RunWith(PowerMockRunner.class)
public class BrokerAccountTest {

    @Mock
    RestAPI restApi;

    @Mock
    LoginHandler loginHandler;

    @InjectMocks
    RestApiAdapter restApiAdapter;

    @Mock
    StreamingApiAdapter streamingApiAdapter;

    BrokerAccount brokerAccount;
    AccountDetails accountDetails;

    @Before
    public void setUp() throws Exception {
        brokerAccount = new BrokerAccount(restApiAdapter, streamingApiAdapter);
        PowerMockito.mockStatic(Zorro.class);
        PowerMockito.when(Zorro.class,"callProgress",anyInt()).thenReturn(1);
        PowerMockito.doNothing().when(Zorro.class,"indicateError");

        accountDetails = new AccountDetails(5000, 10, 15);
        when(loginHandler.getAccountId()).thenReturn("TestAccountId");

        AccountsItem accountsItem = new AccountsItem();
        Balance balance = new Balance();
        balance.setBalance(5000F);
        balance.setProfitLoss(10F);
        balance.setDeposit(15F);
        accountsItem.setBalance(balance);
        accountsItem.setAccountId("TestAccountId");
        GetAccountsV1Response accountsV1Response = new GetAccountsV1Response();
        accountsV1Response.setAccounts(Collections.singletonList(accountsItem));
        when(restApi.getAccountsV1(any())).thenReturn(accountsV1Response);

    }

    @Test
    public void testBrokerAccountSubscribe() throws Exception {
        PublishSubject<AccountDetails> subject = PublishSubject.create();
        when(streamingApiAdapter.getAccountObservable("TestAccountId")).thenReturn(subject);

        double[] accountParams = new double[3];
        brokerAccount.startAccountSubscription();
        brokerAccount.fillAccountParams(accountParams);

        assertArrayEquals(new double[]{5000, 10, 15}, accountParams,0);
        subject.onNext(new AccountDetails(20000, 20, 30));
        brokerAccount.fillAccountParams(accountParams);
        assertArrayEquals(new double[]{20000, 20, 30}, accountParams,0);
    }

    @Test
    public void testBrokerAccountSubscribeError() throws Exception {
        PublishSubject<AccountDetails> subject = PublishSubject.create();
        when(streamingApiAdapter.getAccountObservable("TestAccountId")).thenReturn(subject);

        double[] accountParams = new double[3];
        brokerAccount.startAccountSubscription();
        brokerAccount.fillAccountParams(accountParams);

        subject.onError(new Exception("TestException"));
        System.out.println(subject.getThrowable().getMessage());
        assertEquals(true,subject.hasThrowable());
        assertEquals("TestException", subject.getThrowable().getMessage());
    }
}
