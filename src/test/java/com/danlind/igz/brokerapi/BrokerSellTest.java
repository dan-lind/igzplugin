package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.PluginProperties;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.handler.LoginHandler;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.DealStatus;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.PositionStatus;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.MarketStatus;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.closeOTCPositionV1.CloseOTCPositionV1Response;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.Direction;
import com.danlind.igz.misc.MarketDataProvider;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.apache.http.annotation.Contract;
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
import static org.junit.Assert.assertNotNull;
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

    ChronicleMap<Integer, OrderDetails> orderReferenceMap;

    @Mock
    MarketDataProvider marketDataProvider;

    @Mock
    PluginProperties pluginProperties;

    OrderDetails orderDetails;

    @InjectMocks
    RestApiAdapter restApiAdapter;

    BrokerSell brokerSell;

    Epic testEpic = new Epic("TestEpic");
    double[] tradeParams;
    private GetDealConfirmationV1Response getDealConfirmationV1Response = new GetDealConfirmationV1Response();

    @Before
    public void setUp() throws Exception {
        DealId dealId = new DealId("TestDealId");
        orderDetails = new OrderDetails(testEpic, 105, Direction.BUY, 100, dealId);
        orderReferenceMap = initMap();
        orderReferenceMap.put(1000,orderDetails);


        brokerSell = new BrokerSell(restApiAdapter, orderReferenceMap, marketDataProvider);

        PowerMockito.mockStatic(Zorro.class);
        PowerMockito.doNothing().when(Zorro.class,"indicateError");



        getDealConfirmationV1Response.setDealId(dealId.getValue());
        getDealConfirmationV1Response.setEpic(testEpic.getName());
        getDealConfirmationV1Response.setLevel(105F);
        getDealConfirmationV1Response.setSize(100F);
        getDealConfirmationV1Response.setDealStatus(DealStatus.ACCEPTED);

        tradeParams = new double[3];
        tradeParams[0] = 100;
        tradeParams[1] = 10;



        CloseOTCPositionV1Response response = new CloseOTCPositionV1Response();
        response.setDealReference("TestDealReference");

        ContractDetails contractDetails = new ContractDetails(testEpic, 2, 3, 4, 0.5, 10, 12, "-", "EUR", 1, MarketStatus.TRADEABLE);

        when(restApi.getDealConfirmationV1(any(), any())).thenReturn(getDealConfirmationV1Response);
        when(restApi.closeOTCPositionV1(any(), any())).thenReturn(response);
        when(marketDataProvider.getContractDetails(any())).thenReturn(contractDetails);
        when(pluginProperties.getRestApiMaxRetry()).thenReturn(3);
        when(pluginProperties.getRestApiRetryInterval()).thenReturn(100);

    }

    @Test
    public void testCloseFullPositionAccepted() throws Exception {
        getDealConfirmationV1Response.setStatus(PositionStatus.CLOSED);
        assertEquals(1000, brokerSell.closePosition(1000, 100));
    }

    @Test
    public void testClosePartialPositionAccepted() {
        getDealConfirmationV1Response.setStatus(PositionStatus.PARTIALLY_CLOSED);
        assertEquals(1001, brokerSell.closePosition(1000, 50));
        assertNotNull(orderReferenceMap.get(1001));
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

    private ChronicleMap<Integer, OrderDetails>  initMap() {
        final OrderDetails sampleOrderDetails = new OrderDetails(new Epic("IX.D.OMX.IFD.IP"), 10000, Direction.BUY, 20, new DealId("DIAAAAA9QN6L4AU"));

        return  ChronicleMapBuilder
            .of(Integer.class, OrderDetails.class)
            .averageValue(sampleOrderDetails)
            .entries(50)
            .create();
    }

}
