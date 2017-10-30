package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.PluginProperties;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.domain.types.OrderText;
import com.danlind.igz.handler.LoginHandler;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.Reason;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.CreateOTCPositionV2Request;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.Direction;
import com.danlind.igz.misc.MarketDataProvider;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.DealStatus;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.MarketStatus;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.CreateOTCPositionV2Response;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by danlin on 2017-04-08.
 */
@PrepareForTest(Zorro.class)
@RunWith(PowerMockRunner.class)
public class BrokerBuyTest {

    @Mock
    RestAPI restApi;

    @Mock
    MarketDataProvider marketDataProvider;

    @Mock
    LoginHandler loginHandler;

    ChronicleMap<Integer, OrderDetails> orderReferenceMap;

    @Mock
    PluginProperties pluginProperties;

    @Mock
    OrderDetails orderDetails;

    @InjectMocks
    RestApiAdapter restApiAdapter;

    BrokerBuy brokerBuy;

    ContractDetails contractDetails;
    Epic testEpic = new Epic("TestEpic");
    double[] tradeParams;
    private GetDealConfirmationV1Response getDealConfirmationV1Response = new GetDealConfirmationV1Response();

    @Before
    public void setUp() throws Exception {
        orderReferenceMap = initMap();
        brokerBuy = new BrokerBuy(restApiAdapter, marketDataProvider, orderReferenceMap);

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
        when(restApi.createOTCPositionV2(any(), any())).thenReturn(response);
        when(pluginProperties.getRestApiMaxRetry()).thenReturn(3);
        when(pluginProperties.getRestApiRetryInterval()).thenReturn(100);

        contractDetails = new ContractDetails(testEpic, 2, 3, 4, 0.5, 10, 12, "-", "EUR", 1, MarketStatus.TRADEABLE);
        when(marketDataProvider.getContractDetails(testEpic)).thenReturn(contractDetails);
        getDealConfirmationV1Response.setDealStatus(DealStatus.ACCEPTED);
        getDealConfirmationV1Response.setReason(Reason.ATTACHED_ORDER_LEVEL_ERROR);
    }

    @Test
    public void testCreatePositionAccepted() throws Exception {
        assertEquals(1001, brokerBuy.createPosition(testEpic,tradeParams));
        assertNotNull(orderReferenceMap.get(1001));
        assertEquals("TestDealId",orderReferenceMap.get(1001).getDealId().getValue());
        assertEquals(105, tradeParams[2], 0);
    }

    @Test
    public void testBrokerBuyRejected() {
        getDealConfirmationV1Response.setDealStatus(DealStatus.REJECTED);
        assertEquals(0, brokerBuy.createPosition(testEpic,tradeParams));
    }

    @Test
    public void testBrokerBuyCreatePositionHttpException() throws Exception {
        when(restApi.createOTCPositionV2(any(),any())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "testError", "This is a test response body".getBytes(), Charset.defaultCharset()));
        assertEquals(0, brokerBuy.createPosition(testEpic,tradeParams));
    }

    @Test
    public void testBrokerBuyCreatePositionOtherException() throws Exception {
        when(restApi.createOTCPositionV2(any(),any())).thenThrow(new RuntimeException("This is a runtime error"));
        assertEquals(0, brokerBuy.createPosition(testEpic,tradeParams));
    }

    @Test
    public void testBrokerBuyConfirmationHttpException() throws Exception {
        when(restApi.getDealConfirmationV1(any(), any())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "testError", "This is a test response body".getBytes(), Charset.defaultCharset()));
        assertEquals(0, brokerBuy.createPosition(testEpic,tradeParams));
    }

    @Test
    public void testBrokerBuyConfirmationOtherException() throws Exception {
        when(restApi.getDealConfirmationV1(any(), any())).thenThrow(new RuntimeException("RuntimeException"));
        assertEquals(0, brokerBuy.createPosition(testEpic,tradeParams));
    }

    @Test
    public void testWithoutOrderText() throws Exception {
        brokerBuy.createPosition(testEpic,tradeParams);
        ArgumentCaptor<CreateOTCPositionV2Request> argumentWithoutOrderText = ArgumentCaptor.forClass(CreateOTCPositionV2Request.class);
        verify(restApi).createOTCPositionV2(any(), argumentWithoutOrderText.capture());
        assertEquals(10, argumentWithoutOrderText.getValue().getDealReference().length());
    }

    @Test
    public void testWithOrderText() throws Exception {
        assertEquals(1, brokerBuy.setOrderText(new OrderText("MyTestText")));
        brokerBuy.createPosition(testEpic,tradeParams);
        ArgumentCaptor<CreateOTCPositionV2Request> argumentWithOrderText = ArgumentCaptor.forClass(CreateOTCPositionV2Request.class);
        verify(restApi).createOTCPositionV2(any(), argumentWithOrderText.capture());
        assertEquals(21, argumentWithOrderText.getValue().getDealReference().length());
        assertEquals("MyTestText-", argumentWithOrderText.getValue().getDealReference().substring(0,11));

    }

    @Test
    public void testWithTooLongOrderText() throws Exception {
        assertEquals(0, brokerBuy.setOrderText(new OrderText("MyTestTextAndItIsVeryLong")));
    }

    @Test
    public void testWithMatchingOrderText() throws Exception {
        assertEquals(1, brokerBuy.setOrderText(new OrderText("VeryLong0-_")));
    }

    @Test
    public void testWithNonMatchingOrderText() throws Exception {
        assertEquals(0, brokerBuy.setOrderText(new OrderText("VeryLong0£")));
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
