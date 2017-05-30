package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.StreamingApiAdapter;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.PriceDetails;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.domain.types.Volume;
import com.danlind.igz.handler.HistoryHandler;
import com.danlind.igz.misc.MarketDataProvider;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.MarketStatus;
import com.danlind.igz.misc.VolumeProvider;
import io.reactivex.subjects.PublishSubject;
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by danlin on 2017-04-08.
 */
@PrepareForTest(Zorro.class)
@RunWith(PowerMockRunner.class)
public class BrokerAssetTest {

    private final static Logger logger = LoggerFactory.getLogger(BrokerAssetTest.class);

    @Mock
    StreamingApiAdapter adapter;

    @Mock
    MarketDataProvider marketDataProvider;

    @Mock
    HistoryHandler historyHandler;

    @InjectMocks
    BrokerAsset brokerAsset;

    private Epic testEpic;
    private PublishSubject<Volume> volumeSubject;
    private PublishSubject<PriceDetails> priceDetailsSubject;
    private ContractDetails contractDetails;
    private VolumeProvider volumeProvider;

    @Before
    public void setUp() {
        testEpic = new Epic("TestEpic");
        volumeSubject = PublishSubject.create();
        priceDetailsSubject = PublishSubject.create();
        contractDetails = new ContractDetails(testEpic, 2, 3, 4, -200, 10, 12, "-", "EUR", 1, MarketStatus.TRADEABLE);
        volumeProvider = new VolumeProvider();

        when(adapter.getTickObservable(testEpic)).thenReturn(priceDetailsSubject);
        when(adapter.getVolumeObservable(testEpic)).thenReturn(volumeSubject);
        when(marketDataProvider.getContractDetails(testEpic)).thenReturn(contractDetails);
    }

    @Test
    public void testSubscribeToAsset() {
        assertEquals(1, brokerAsset.subscribeToLighstreamerTickUpdates(testEpic));

        //Confirm initial setup is correct
        PriceDetails details = brokerAsset.getPriceDetails(testEpic);
        assertEquals(10, details.getBid(), 0);
        assertEquals(12, details.getAsk(), 0);

        //Confirm initial setup overridden when tick arrives
        priceDetailsSubject.onNext(new PriceDetails(testEpic, 100,120));
        details = brokerAsset.getPriceDetails(testEpic);
        assertEquals(100, details.getBid(), 0);
        assertEquals(120, details.getAsk(), 0);

        //Confirm new tick overrides previous tick
        priceDetailsSubject.onNext(new PriceDetails(testEpic, 1000,1200));
        details = brokerAsset.getPriceDetails(testEpic);
        assertEquals(1000, details.getBid(), 0);
        assertEquals(1200, details.getAsk(), 0);
    }

    @Test
    public void testGetAsset() {
        brokerAsset = new BrokerAsset(marketDataProvider, volumeProvider, adapter, historyHandler);
        brokerAsset.subscribeToLighstreamerTickUpdates(testEpic);

        priceDetailsSubject.onNext(new PriceDetails(testEpic, 100,120));
        volumeSubject.onNext(new Volume(10));

        double[] assetParams = new double[9];
        brokerAsset.getLatestAssetData(testEpic, assetParams);

        //Confirms all fields are correctly filled when requested from Zorro
        assertEquals(120, assetParams[0], 0); //Ask
        assertEquals(20, assetParams[1], 0);  //Spread
        assertEquals(10, assetParams[2], 0);  //Volume
        assertEquals(2, assetParams[3], 0);  //Pip Size
        assertEquals(3, assetParams[4], 0);  //Pip Cost
        assertEquals(4, assetParams[5], 0);  //Lot Amount
        assertEquals(-200, assetParams[6], 0);  //Margin Cost, returns leverage
        assertEquals(0, assetParams[7], 0);  //Roll over
        assertEquals(0, assetParams[8], 0);  //Roll over
    }

    @Test
    public void testSubscribeToInvalidAsset() throws Exception {
        when(adapter.getTickObservable(testEpic)).thenThrow(new RuntimeException());

        PowerMockito.mockStatic(Zorro.class);
        PowerMockito.doNothing().when(Zorro.class,"indicateError");

        assertEquals(0, brokerAsset.subscribeToLighstreamerTickUpdates(testEpic));
    }
}
