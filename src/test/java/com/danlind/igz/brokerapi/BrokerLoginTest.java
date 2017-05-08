package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.adapter.StreamingApiAdapter;
import com.danlind.igz.config.PluginProperties;
import com.danlind.igz.domain.types.AccountType;
import com.danlind.igz.handler.LoginHandler;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.StreamingAPI;
import com.danlind.igz.ig.api.client.rest.AuthenticationResponseAndConversationContext;
import com.danlind.igz.ig.api.client.rest.ConversationContextV3;
import com.lightstreamer.ls_client.ConnectionListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.HttpClientErrorException;

import java.util.concurrent.ScheduledFuture;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Created by danlin on 2017-04-08.
 */
@PrepareForTest(Zorro.class)
@RunWith(PowerMockRunner.class)

public class BrokerLoginTest {

    @Mock
    RestAPI restApi;

    @Mock
    LoginHandler loginHandler;

    @Mock
    StreamingAPI streamingAPI;

    @Mock
    PluginProperties pluginProperties;

    @Mock
    ConnectionListener connectionListener;

    @Mock
    ScheduledFuture scheduledFuture;

    @InjectMocks
    RestApiAdapter restApiAdapter;

    @InjectMocks
    StreamingApiAdapter streamingApiAdapter;

    BrokerLogin brokerLogin;

    @Before
    public void setUp() throws Exception {
        ThreadPoolTaskScheduler threadPoolTaskScheduler
            = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix(
            "ThreadPoolTaskScheduler");

        threadPoolTaskScheduler.initialize();

        brokerLogin = new BrokerLogin(streamingApiAdapter, restApiAdapter, threadPoolTaskScheduler, pluginProperties);
        PowerMockito.mockStatic(Zorro.class);
        PowerMockito.when(Zorro.class,"callProgress",anyInt()).thenReturn(1);
        PowerMockito.doNothing().when(Zorro.class,"indicateError");

        AuthenticationResponseAndConversationContext context = AuthenticationResponseAndConversationContext.builder()
            .accountId("TestAccountId")
            .conversationContext(mock(ConversationContextV3.class))
            .lightstreamerEndpoint("TestLightstreamerEndpoint")
            .build();



        when(restApi.createSessionV3(any(), any())).thenReturn(context);
        when(streamingAPI.connect(anyString(), any(), anyString())).thenReturn(connectionListener);
        when(pluginProperties.getRefreshTokenInterval()).thenReturn(30000);
    }

    @Test
    public void testConnectSucceeded() throws Exception {
        assertEquals(1,brokerLogin.connect("TestId", "TestPassword", "Real"));
        assertEquals("TestAccountId", brokerLogin.getAccountId());
        assertEquals(AccountType.Real, brokerLogin.getZorroAccountType());
    }

    @Test
    public void testConnectFailedWithHttpException() throws Exception {
        when(restApi.createSessionV3(any(), any())).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        assertEquals(0,brokerLogin.connect("TestId", "TestPassword", "Real"));
    }

    @Test
    public void testConnectFailedWithOtherException() throws Exception {
        when(restApi.createSessionV3(any(), any())).thenThrow(new RuntimeException());
        assertEquals(0,brokerLogin.connect("TestId", "TestPassword", "Real"));
    }

    @Test
    public void testRefreshToken() throws Exception {
        when(pluginProperties.getRefreshTokenInterval()).thenReturn(1);
        assertEquals(1,brokerLogin.connect("TestId", "TestPassword", "Real"));
        verify(restApi, atLeastOnce()).refreshSessionV1(any(), any());
    }

    @Test
    public void testDisconnect() throws Exception {
        assertEquals(1,brokerLogin.connect("TestId", "TestPassword", "Real"));
        assertEquals(1,brokerLogin.disconnect());
    }

}
