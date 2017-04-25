package com.danlind.igz.misc;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.PluginConfig;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.domain.types.Volume;
import com.danlind.igz.handler.LoginHandler;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.*;
import io.reactivex.schedulers.TestScheduler;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by danlin on 2017-03-29.
 */

@PrepareForTest(Zorro.class)
@RunWith(PowerMockRunner.class)
public class MarketDataProviderTest {


    @Mock
    RestAPI restApi;

    @Mock
    LoginHandler loginHandler;

    @Mock
    PluginConfig pluginConfig;

    @InjectMocks
    RestApiAdapter restApiAdapter;

    MarketDataProvider marketDataProvider;

    Epic testEpic = new Epic("TestEpic");
    GetMarketDetailsV3Response response;
    @Before
    public void setUp() throws Exception {
        marketDataProvider = new MarketDataProvider(restApiAdapter);

        PowerMockito.mockStatic(Zorro.class);
        PowerMockito.doNothing().when(Zorro.class,"indicateError");


        response = new GetMarketDetailsV3Response();
        Snapshot snapshot = new Snapshot();
        snapshot.setScalingFactor(10000);
        snapshot.setBid(BigDecimal.valueOf(100));
        snapshot.setOffer(BigDecimal.valueOf(120));
        snapshot.setMarketStatus(MarketStatus.TRADEABLE);
        response.setSnapshot(snapshot);

        DealingRules dealingRules = new DealingRules();
        MinDealSize minDealSize = new MinDealSize();
        minDealSize.setValue(100d);
        dealingRules.setMinDealSize(minDealSize);
        response.setDealingRules(dealingRules);

        Instrument instrument = new Instrument();
        instrument.setEpic(testEpic.getName());
        instrument.setMarginFactor(BigDecimal.TEN);
        instrument.setExpiry("-");
        List<CurrenciesItem> currenciesItems = new ArrayList<>();
        CurrenciesItem currenciesItem = new CurrenciesItem();
        currenciesItem.setBaseExchangeRate(0.5f);
        currenciesItem.setCode("EUR");
        currenciesItems.add(currenciesItem);
        instrument.setCurrencies(currenciesItems);
        instrument.setValueOfOnePip("10");
        response.setInstrument(instrument);

        when(restApi.getMarketDetailsV3(any(),anyString())).thenReturn(response);
        when(pluginConfig.getRefreshMarketDataInterval()).thenReturn(50);
    }

    @Test
    public void testGetContractDetails() throws InterruptedException {
        marketDataProvider.updateMarketDetails(testEpic);
        ContractDetails contractDetails = marketDataProvider.getContractDetails(testEpic);
        assertEquals("EUR", contractDetails.getCurrencyCode());
        response.getInstrument().getCurrencies().get(0).setCode("USD");
        Thread.sleep(100);
        contractDetails = marketDataProvider.getContractDetails(testEpic);
        assertEquals("USD", contractDetails.getCurrencyCode());

    }

    @Test
    public void testMarketIsTradable() {
        assertEquals(1,marketDataProvider.isAnySubscribedEpicTradable());
        marketDataProvider.updateMarketDetails(testEpic);
        assertEquals(2,marketDataProvider.isAnySubscribedEpicTradable());
    }

    @Test
    public void testMarketIsNotTradable() {
        response.getSnapshot().setMarketStatus(MarketStatus.ON_AUCTION);
        marketDataProvider.updateMarketDetails(testEpic);
        assertEquals(1,marketDataProvider.isAnySubscribedEpicTradable());
    }

}
