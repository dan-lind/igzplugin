package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.adapter.StreamingApiAdapter;
import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.handler.LoginHandler;
import com.danlind.igz.misc.MarketDataProvider;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.dto.markets.getMarketDetailsV3.MarketStatus;
import com.danlind.igz.ig.api.client.rest.dto.session.encryptionKey.getEncryptionKeySessionV1.GetEncryptionKeySessionV1Response;
import com.danlind.igz.misc.TimeConvert;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by danlin on 2017-04-08.
 */
@PrepareForTest(Zorro.class)
@RunWith(PowerMockRunner.class)
public class BrokerTimeTest {

    private final static Logger logger = LoggerFactory.getLogger(BrokerTimeTest.class);

    @Mock
    LoginHandler loginHandler;

    @Mock
    RestAPI restAPI;

    @Mock
    MarketDataProvider marketDataProvider;

    @Mock
    StreamingApiAdapter streamingApiAdapter;

    @InjectMocks
    RestApiAdapter restApiAdapter;



    private PublishSubject<Long> heartbeatSubject;
    private BrokerTime brokerTime;

    @Before
    public void setUp() throws Exception {
        ThreadPoolTaskScheduler threadPoolTaskScheduler
            = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix(
            "ThreadPoolTaskScheduler");

        threadPoolTaskScheduler.initialize();

        Epic testEpic = new Epic("TestEpic");
        ContractDetails contractDetails = new ContractDetails(testEpic, 2, 3, 4, 5, 0.5, 10, 12, "-", "EUR", 1, MarketStatus.TRADEABLE);

        PowerMockito.mockStatic(Zorro.class);
        PowerMockito.doNothing().when(Zorro.class,"indicateError");

        brokerTime = new BrokerTime(streamingApiAdapter, marketDataProvider,threadPoolTaskScheduler,restApiAdapter);

        heartbeatSubject = PublishSubject.create();

        GetEncryptionKeySessionV1Response response = new GetEncryptionKeySessionV1Response();
        response.setTimeStamp(1000L);

        when(streamingApiAdapter.getHeartbeatObservable()).thenReturn(heartbeatSubject);
        when(marketDataProvider.getContractDetails(testEpic)).thenReturn(contractDetails);
        when(restAPI.getEncryptionKeySessionV1(any())).thenReturn(response);
    }

    @Test
    public void testSubscribeToTime() {
        brokerTime.subscribeToLighstreamerHeartbeat();
    }

    @Test
    public void testGetTime() {
        brokerTime.subscribeToLighstreamerHeartbeat();
        double[] time = new double[1];
        brokerTime.getBrokerTime(time);

        assertEquals(1000L, TimeConvert.millisFromOLEDate(time[0]), 0);
        heartbeatSubject.onNext(2000L);

        brokerTime.getBrokerTime(time);
        assertEquals(2000L, TimeConvert.millisFromOLEDate(time[0]), 0);
    }
}
