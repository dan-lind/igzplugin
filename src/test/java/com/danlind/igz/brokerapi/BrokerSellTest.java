package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.handler.LoginHandler;
import com.danlind.igz.handler.MarketHandler;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.DealStatus;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.MarketStatus;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.CreateOTCPositionV2Response;
import net.openhft.chronicle.map.ChronicleMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by danlin on 2017-04-08.
 */
@PrepareForTest(Zorro.class)
@RunWith(PowerMockRunner.class)
public class BrokerSellTest {

    @Mock
    RestAPI restApi;

    @Mock
    LoginHandler loginHandler;

    @Mock
    MarketHandler marketHandler;

    @Mock
    ChronicleMap<Integer, OrderDetails> orderReferenceMap;

    @Mock
    OrderDetails orderDetails;

    @InjectMocks
    RestApiAdapter restApiAdapter;

    AtomicInteger atomicInteger = new AtomicInteger(1000);

    BrokerBuy brokerBuy;

    ContractDetails contractDetails;
    Epic testEpic = new Epic("TestEpic");
    double[] tradeParams;
    private GetDealConfirmationV1Response getDealConfirmationV1Response = new GetDealConfirmationV1Response();
//
    @Before
    public void setUp() throws Exception {
        brokerBuy = new BrokerBuy(restApiAdapter, marketHandler, orderReferenceMap, atomicInteger);

        PowerMockito.mockStatic(Zorro.class);
        PowerMockito.doNothing().when(Zorro.class,"indicateError");

        getDealConfirmationV1Response.setDealId("TestDealId");
        getDealConfirmationV1Response.setEpic(testEpic.getName());
        getDealConfirmationV1Response.setLevel(105F);
        getDealConfirmationV1Response.setSize(100F);

        tradeParams = new double[3];
        tradeParams[0] = 100;
        tradeParams[1] = 10;

        CreateOTCPositionV2Response response = new CreateOTCPositionV2Response();
        response.setDealReference("TestDealReference");

        when(restApi.getDealConfirmationV1(any(), any())).thenReturn(getDealConfirmationV1Response);
        when(orderDetails.getDealId()).thenReturn(new DealId("TestDealId"));
        when(orderReferenceMap.get(any())).thenReturn(orderDetails);
        when(restApi.createOTCPositionV2(any(), any())).thenReturn(response);

        contractDetails = new ContractDetails(testEpic, 2, 3, 4, 5, 0.5, 10, 12, "-", "EUR", 1, MarketStatus.TRADEABLE);
        when(marketHandler.getContractDetails(testEpic)).thenReturn(contractDetails);

    }

    @Test
    public void testCreatePositionAccepted() throws Exception {
        getDealConfirmationV1Response.setDealStatus(DealStatus.ACCEPTED);

        assertEquals(1000, brokerBuy.createPosition(testEpic,tradeParams));
        assertEquals(1001, atomicInteger.get());
        assertEquals(105, tradeParams[2], 0);
    }

    @Test
    public void testBrokerStopDealRejected() {
        getDealConfirmationV1Response.setDealStatus(DealStatus.REJECTED);
        assertEquals(0, brokerBuy.createPosition(testEpic,tradeParams));
    }

    @Test
    public void testBrokerStopUpdatePositionHttpException() throws Exception {
        when(restApi.updateOTCPositionV2(any(), any(),any())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "testError", "This is a test response body".getBytes(), Charset.defaultCharset()));
        assertEquals(0, brokerBuy.createPosition(testEpic,tradeParams));
    }

    @Test
    public void testBrokerStopUpdatePositionOtherException() throws Exception {
        when(restApi.updateOTCPositionV2(any(), any(),any())).thenThrow(new RuntimeException("This is a runtime error"));
        assertEquals(0, brokerBuy.createPosition(testEpic,tradeParams));
    }

    @Test
    public void testBrokerStopConfirmationHttpException() throws Exception {
        when(restApi.getDealConfirmationV1(any(), any())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "testError", "This is a test response body".getBytes(), Charset.defaultCharset()));
        assertEquals(0, brokerBuy.createPosition(testEpic,tradeParams));
    }

    @Test
    public void testBrokerStopConfirmationOtherException() throws Exception {
        when(restApi.getDealConfirmationV1(any(), any())).thenThrow(new RuntimeException("RuntimeException"));
        assertEquals(0, brokerBuy.createPosition(testEpic,tradeParams));
    }
}
