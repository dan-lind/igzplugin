package com.danlind.igz.brokerapi;

import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.PriceDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.handler.AssetHandler;
import com.danlind.igz.handler.LoginHandler;
import com.danlind.igz.handler.MarketHandler;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.DealStatus;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.MarketStatus;
import com.danlind.igz.ig.api.client.rest.dto.positions.getPositionByDealIdV2.GetPositionByDealIdV2Response;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.closeOTCPositionV1.CloseOTCPositionV1Response;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.Direction;
import net.openhft.chronicle.map.ChronicleMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by danlin on 2017-04-08.
 */
//@PrepareForTest(Zorro.class)
@RunWith(MockitoJUnitRunner.class)
public class BrokerLoginTest {

    @Mock
    RestAPI restApi;

    @Mock
    LoginHandler loginHandler;

    @Mock
    MarketHandler marketHandler;

    @Mock
    AssetHandler assetHandler;

    private final OrderDetails sampleOrderDetails = new OrderDetails(new Epic("IX.D.OMX.IFD.IP"), 10000, Direction.BUY, 20, new DealId("DIAAAAA9QN6L4AU"));
    ChronicleMap<Integer, OrderDetails> orderReferenceMap;

    OrderDetails longOrderDetails;
    OrderDetails shortOrderDetails;

    @InjectMocks
    RestApiAdapter restApiAdapter;

    BrokerTrade brokerTrade;
    DealId dealId;
    DealId otherDealId;
    ContractDetails contractDetails;
    Epic testEpic = new Epic("TestEpic");
    double[] orderParams;

    @Before
    public void setUp() throws Exception {
        orderReferenceMap = ChronicleMap
            .of(Integer.class, OrderDetails.class)
            .averageValue(sampleOrderDetails)
            .entries(50)
            .create();

        brokerTrade = new BrokerTrade(orderReferenceMap, assetHandler, marketHandler, restApiAdapter);

//        PowerMockito.mockStatic(Zorro.class);
//        PowerMockito.doNothing().when(Zorro.class,"indicateError");



        dealId = new DealId("TestDealId");
        otherDealId = new DealId("OtherTestDealId");
        GetDealConfirmationV1Response getDealConfirmationV1Response = new GetDealConfirmationV1Response();
//
        getDealConfirmationV1Response.setDealId(dealId.getValue());
        getDealConfirmationV1Response.setEpic(testEpic.getName());
        getDealConfirmationV1Response.setLevel(105F);
        getDealConfirmationV1Response.setSize(100F);
        getDealConfirmationV1Response.setDealStatus(DealStatus.ACCEPTED);

        orderParams = new double[4];

        longOrderDetails = new OrderDetails(testEpic, 105, Direction.BUY, 100, dealId);
        shortOrderDetails = new OrderDetails(testEpic, 105, Direction.SELL, 100, otherDealId);
        orderReferenceMap.put(1000, longOrderDetails);
        orderReferenceMap.put(1001, shortOrderDetails);

        PriceDetails priceDetails = new PriceDetails(testEpic, 100, 120);

        CloseOTCPositionV1Response response = new CloseOTCPositionV1Response();
        response.setDealReference("TestDealReference");

        when(restApi.getDealConfirmationV1(any(), any())).thenReturn(getDealConfirmationV1Response);
        when(assetHandler.getAssetDetails(testEpic)).thenReturn(priceDetails);

        contractDetails = new ContractDetails(testEpic, 2, 3, 4, 5, 0.5, 10, 12, "-", "EUR", 2, MarketStatus.TRADEABLE);
        when(marketHandler.getContractDetails(testEpic)).thenReturn(contractDetails);

    }

    @Test
    public void testGetTradeStatusExistingLong() throws Exception {
        assertEquals(100, brokerTrade.getTradeStatus(1000, orderParams));
        assertEquals(105,orderParams[0], 0);
        assertEquals(100,orderParams[1], 0);
        assertEquals(0,orderParams[2], 0);
        assertEquals(-6000,orderParams[3], 0);
    }

    @Test
    public void testGetTradeStatusExistingShort() throws Exception {
        assertEquals(100, brokerTrade.getTradeStatus(1001, orderParams));
        assertEquals(105,orderParams[0], 0);
        assertEquals(120,orderParams[1], 0);
        assertEquals(0,orderParams[2], 0);
        assertEquals(-18000,orderParams[3], 0);
    }

    @Test
    public void testGetTradeStatusMissing() {
        assertEquals(0, brokerTrade.getTradeStatus(1002, orderParams));
    }

    @Test
    public void testCheckValidPosition() throws Exception {
        when(restApi.getPositionByDealIdV2(any(), eq(dealId.getValue()))).thenReturn(new GetPositionByDealIdV2Response());
        when(restApi.getPositionByDealIdV2(any(), eq(otherDealId.getValue()))).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found","{\"errorCode\":\"error.position.notfound\"}".getBytes(), StandardCharsets.UTF_8));

        assertEquals(2, orderReferenceMap.size());
        brokerTrade.checkPositionsValid();
        assertEquals(1, orderReferenceMap.size());
    }
//
//    @Test
//    public void testClosePositionOtherException() throws Exception {
//        when(restApi.closeOTCPositionV1(any(),any())).thenThrow(new RuntimeException("This is a runtime error"));
//        assertEquals(0, brokerSell.closePosition(1000, 100));
//    }
//
//    @Test
//    public void testClosePositionConfirmationHttpException() throws Exception {
//        when(restApi.getDealConfirmationV1(any(), any())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "testError", "This is a test response body".getBytes(), Charset.defaultCharset()));
//        assertEquals(0, brokerSell.closePosition(1000, 100));
//    }
//
//    @Test
//    public void testClosePositionConfirmationOtherException() throws Exception {
//        when(restApi.getDealConfirmationV1(any(), any())).thenThrow(new RuntimeException("RuntimeException"));
//        assertEquals(0, brokerSell.closePosition(1000, 100));
//    }
}
