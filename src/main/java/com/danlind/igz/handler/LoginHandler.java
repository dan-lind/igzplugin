package com.danlind.igz.handler;

import com.danlind.igz.Zorro;
import com.danlind.igz.ZorroLogger;
import com.danlind.igz.adapter.StreamingApiAdapter;
import com.danlind.igz.brokerapi.BrokerLogin;
import com.danlind.igz.config.PluginConfig;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.types.AccountType;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.ConversationContext;
import com.danlind.igz.ig.api.client.rest.ConversationContextV3;
import com.danlind.igz.ig.api.client.rest.dto.session.createSessionV3.CreateSessionV3Request;
import com.danlind.igz.ig.api.client.rest.dto.session.refreshSessionV1.RefreshSessionV1Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

@Component
public class LoginHandler {

    private final BrokerLogin brokerLogin;

    @Autowired
    public LoginHandler(BrokerLogin brokerLogin) {
        this.brokerLogin = brokerLogin;
    }

    public int connect(String identifier, String password, String accountType)  {
        return brokerLogin.connect(identifier, password, accountType);
    }

    public int disconnect() {
        return brokerLogin.disconnect();
    }

    public ConversationContext getConversationContext() {
        return brokerLogin.getConversationContext();
    }

    public String getAccountId() {
        return brokerLogin.getAccountId();
    }

    public AccountType getZorroAccountType() {
        return brokerLogin.getZorroAccountType();
    }
}
