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
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.PositionStatus;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.MarketStatus;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.closeOTCPositionV1.CloseOTCPositionV1Response;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.Direction;
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
public class BrokerTradeTest {

    @Mock
    RestAPI restApi;

    @Mock
    LoginHandler loginHandler;

    @Mock
    MarketHandler marketHandler;

    @Mock
    ChronicleMap<Integer, OrderDetails> orderReferenceMap;

    OrderDetails orderDetails;

    @InjectMocks
    RestApiAdapter restApiAdapter;

    AtomicInteger atomicInteger;

    BrokerSell brokerSell;

    ContractDetails contractDetails;
    Epic testEpic = new Epic("TestEpic");
    double[] tradeParams;
    private GetDealConfirmationV1Response getDealConfirmationV1Response = new GetDealConfirmationV1Response();
//
    @Before
    public void setUp() throws Exception {
        atomicInteger = new AtomicInteger(1001);
        brokerSell = new BrokerSell(restApiAdapter, orderReferenceMap, atomicInteger);

        PowerMockito.mockStatic(Zorro.class);
        PowerMockito.doNothing().when(Zorro.class,"indicateError");

        DealId dealId = new DealId("TestDealId");

        getDealConfirmationV1Response.setDealId(dealId.getValue());
        getDealConfirmationV1Response.setEpic(testEpic.getName());
        getDealConfirmationV1Response.setLevel(105F);
        getDealConfirmationV1Response.setSize(100F);
        getDealConfirmationV1Response.setDealStatus(DealStatus.ACCEPTED);

        tradeParams = new double[3];
        tradeParams[0] = 100;
        tradeParams[1] = 10;

        orderDetails = new OrderDetails(testEpic, 105, Direction.BUY, 100, dealId);

        CloseOTCPositionV1Response response = new CloseOTCPositionV1Response();
        response.setDealReference("TestDealReference");

        when(restApi.getDealConfirmationV1(any(), any())).thenReturn(getDealConfirmationV1Response);
        when(orderReferenceMap.get(any())).thenReturn(orderDetails);
        when(restApi.closeOTCPositionV1(any(), any())).thenReturn(response);

        contractDetails = new ContractDetails(testEpic, 2, 3, 4, 5, 0.5, 10, 12, "-", "EUR", 1, MarketStatus.TRADEABLE);
        when(marketHandler.getContractDetails(testEpic)).thenReturn(contractDetails);

    }

    @Test
    public void testCloseFullPositionAccepted() throws Exception {
        getDealConfirmationV1Response.setStatus(PositionStatus.CLOSED);
        assertEquals(1000, brokerSell.closePosition(1000, 100));
    }

    @Test
    public void testClosePartialPositionAccepted() {
        getDealConfirmationV1Response.setStatus(PositionStatus.PARTIALLY_CLOSED);
        assertEquals(1001, atomicInteger.get());
        assertEquals(1001, brokerSell.closePosition(1000, 50));
        assertEquals(1002, atomicInteger.get());
    }

    @Test
    public void testClosePositionHttpException() throws Exception {
        when(restApi.closeOTCPositionV1(any(), any())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "testError", "This is a test response body".getBytes(), Charset.defaultCharset()));
        assertEquals(0, brokerSell.closePosition(1000, 100));
    }

    @Test
    public void testClosePositionOtherException() throws Exception {
        when(restApi.closeOTCPositionV1(any(),any())).thenThrow(new RuntimeException("This is a runtime error"));
        assertEquals(0, brokerSell.closePosition(1000, 100));
    }

    @Test
    public void testClosePositionConfirmationHttpException() throws Exception {
        when(restApi.getDealConfirmationV1(any(), any())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "testError", "This is a test response body".getBytes(), Charset.defaultCharset()));
        assertEquals(0, brokerSell.closePosition(1000, 100));
    }

    @Test
    public void testClosePositionConfirmationOtherException() throws Exception {
        when(restApi.getDealConfirmationV1(any(), any())).thenThrow(new RuntimeException("RuntimeException"));
        assertEquals(0, brokerSell.closePosition(1000, 100));
    }
}
