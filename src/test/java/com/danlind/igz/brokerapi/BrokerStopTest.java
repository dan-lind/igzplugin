package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.handler.LoginHandler;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.DealStatus;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.GetDealConfirmationV1Response;
import com.danlind.igz.ig.api.client.rest.dto.getDealConfirmationV1.Reason;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.updateOTCPositionV2.UpdateOTCPositionV2Response;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by danlin on 2017-04-08.
 */
@PrepareForTest(Zorro.class)
@RunWith(PowerMockRunner.class)
public class BrokerStopTest {

    private final static Logger logger = LoggerFactory.getLogger(BrokerStopTest.class);

    @Mock
    RestAPI restApi;

    @Mock
    LoginHandler loginHandler;

    @Mock
    ChronicleMap<Integer, OrderDetails> orderReferenceMap;

    @Mock
    OrderDetails orderDetails;

    @InjectMocks
    RestApiAdapter restApiAdapter;

    BrokerStop brokerStop;

    private GetDealConfirmationV1Response getDealConfirmationV1Response = new GetDealConfirmationV1Response();

    @Before
    public void setUp() throws Exception {
        brokerStop = new BrokerStop(restApiAdapter, orderReferenceMap);

        UpdateOTCPositionV2Response response = new UpdateOTCPositionV2Response();
        response.setDealReference("TestDealReference");

        PowerMockito.mockStatic(Zorro.class);
        PowerMockito.doNothing().when(Zorro.class,"indicateError");

        getDealConfirmationV1Response.setDealId("TestDealId");
        getDealConfirmationV1Response.setReason(Reason.CANNOT_CHANGE_STOP_TYPE);

        when(restApi.updateOTCPositionV2(any(), any(), any())).thenReturn(response);
        when(restApi.getDealConfirmationV1(any(), any())).thenReturn(getDealConfirmationV1Response);
        when(orderDetails.getDealId()).thenReturn(new DealId("TestDealId"));
        when(orderReferenceMap.get(any())).thenReturn(orderDetails);
    }

    @Test
    public void testBrokerStopDealAccepted() throws Exception {
        getDealConfirmationV1Response.setDealStatus(DealStatus.ACCEPTED);
        assertEquals(1, brokerStop.updateStop(1,10));
    }

    @Test
    public void testBrokerStopDealRejected() {
        getDealConfirmationV1Response.setDealStatus(DealStatus.REJECTED);
        assertEquals(0, brokerStop.updateStop(1,10));
    }

    @Test
    public void testBrokerStopUpdatePositionHttpException() throws Exception {
        when(restApi.updateOTCPositionV2(any(), any(),any())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "testError", "This is a test response body".getBytes(), Charset.defaultCharset()));
        assertEquals(0, brokerStop.updateStop(1,10));
    }

    @Test
    public void testBrokerStopUpdatePositionOtherException() throws Exception {
        when(restApi.updateOTCPositionV2(any(), any(),any())).thenThrow(new RuntimeException("This is a runtime error"));
        assertEquals(0, brokerStop.updateStop(1,10));
    }

    @Test
    public void testBrokerStopConfirmationHttpException() throws Exception {
        when(restApi.getDealConfirmationV1(any(), any())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "testError", "This is a test response body".getBytes(), Charset.defaultCharset()));
        assertEquals(0, brokerStop.updateStop(1,10));
    }

    @Test
    public void testBrokerStopConfirmationOtherException() throws Exception {
        when(restApi.getDealConfirmationV1(any(), any())).thenThrow(new RuntimeException("RuntimeException"));
        assertEquals(0, brokerStop.updateStop(1,10));
    }
}
