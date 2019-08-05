package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.adapter.StreamingApiAdapter;
import com.danlind.igz.domain.AccountDetails;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.handler.LoginHandler;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.dto.getAccountsV1.AccountsItem;
import com.danlind.igz.ig.api.client.rest.dto.getAccountsV1.Balance;
import com.danlind.igz.ig.api.client.rest.dto.getAccountsV1.GetAccountsV1Response;
import com.danlind.igz.ig.api.client.rest.dto.prices.getPricesV3.*;
import com.danlind.igz.ig.api.client.rest.dto.session.getSessionV1.GetSessionV1Response;
import io.reactivex.subjects.PublishSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by danlin on 2017-04-08.
 */
@RunWith(MockitoJUnitRunner.class)
public class BrokerHistoryTest {

    @Mock
    RestAPI restApi;

    @Mock
    LoginHandler loginHandler;

    @InjectMocks
    RestApiAdapter restApiAdapter;


    BrokerHistory brokerHistory;

    @Before
    public void setUp() throws Exception {
        brokerHistory = new BrokerHistory(restApiAdapter);

        GetSessionV1Response response = new GetSessionV1Response();
        response.setTimezoneOffset(2);
        ResponseEntity<GetSessionV1Response> sessionV1ResponseResponseEntity = new ResponseEntity<>(response, HttpStatus.OK);

        GetPricesV3Response pricesV3Response = new GetPricesV3Response();
        pricesV3Response.setPrices(createPrices());

        when(restApi.getPricesV3(any(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(pricesV3Response);

        when(restApi.getSessionV1(any(),anyBoolean())).thenReturn(sessionV1ResponseResponseEntity);

    }

    @Test
    public void testGetHistory() throws Exception {
        double[] params = new double[21];
        assertEquals(3, brokerHistory.getPriceHistory(new Epic("MyTestClass"),30000,30020,1,60,params));
        assertEquals(290, params[0], 0);
        assertEquals(190, params[7], 0);
        assertEquals(200, params[8], 0);
        assertEquals(220, params[9], 0);
        assertEquals(180, params[10], 0);
        assertEquals(5, params[12], 0);
        assertEquals(15000, params[13], 0);
    }

    @Test
    public void testGetHistorySimple() throws Exception {
        List<PricesItem> pricesItems = brokerHistory.getPriceHistory(new Epic("MyTestClass"),3);
        assertEquals(3, pricesItems.size());
    }


    private List<PricesItem> createPrices() {
        PricesItem item1 = new PricesItem();
        ClosePrice closePrice = new ClosePrice();
        closePrice.setBid(BigDecimal.valueOf(95));
        closePrice.setAsk(BigDecimal.valueOf(100));
        item1.setClosePrice(closePrice);
        HighPrice highPrice = new HighPrice();
        highPrice.setAsk(BigDecimal.valueOf(120));
        item1.setHighPrice(highPrice);
        LowPrice lowPrice = new LowPrice();
        lowPrice.setAsk(BigDecimal.valueOf(80));
        item1.setLowPrice(lowPrice);
        OpenPrice openPrice = new OpenPrice();
        openPrice.setAsk(BigDecimal.valueOf(90));
        item1.setOpenPrice(openPrice);
        item1.setLastTradedVolume(10000L);
        item1.setSnapshotTimeUTC("2017-04-25T10:27:00");

        PricesItem item2 = new PricesItem();
        ClosePrice closePrice2 = new ClosePrice();
        closePrice2.setBid(BigDecimal.valueOf(195));
        closePrice2.setAsk(BigDecimal.valueOf(200));
        item2.setClosePrice(closePrice2);
        HighPrice highPrice2 = new HighPrice();
        highPrice2.setAsk(BigDecimal.valueOf(220));
        item2.setHighPrice(highPrice2);
        LowPrice lowPrice2 = new LowPrice();
        lowPrice2.setAsk(BigDecimal.valueOf(180));
        item2.setLowPrice(lowPrice2);
        OpenPrice openPrice2 = new OpenPrice();
        openPrice2.setAsk(BigDecimal.valueOf(190));
        item2.setOpenPrice(openPrice2);
        item2.setLastTradedVolume(15000L);
        item2.setSnapshotTimeUTC("2017-04-25T10:28:00");

        PricesItem item3 = new PricesItem();
        ClosePrice closePrice3 = new ClosePrice();
        closePrice3.setBid(BigDecimal.valueOf(295));
        closePrice3.setAsk(BigDecimal.valueOf(300));
        item3.setClosePrice(closePrice3);
        HighPrice highPrice3 = new HighPrice();
        highPrice3.setAsk(BigDecimal.valueOf(320));
        item3.setHighPrice(highPrice3);
        LowPrice lowPrice3 = new LowPrice();
        lowPrice3.setAsk(BigDecimal.valueOf(280));
        item3.setLowPrice(lowPrice3);
        OpenPrice openPrice3 = new OpenPrice();
        openPrice3.setAsk(BigDecimal.valueOf(290));
        item3.setOpenPrice(openPrice3);
        item3.setLastTradedVolume(25000L);
        item3.setSnapshotTimeUTC("2017-04-25T10:29:00");


        List<PricesItem> pricesItems = new ArrayList<>();
        pricesItems.add(item1);
        pricesItems.add(item2);
        pricesItems.add(item3);

        return pricesItems;
    }

}
