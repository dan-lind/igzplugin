package com.danlind.igz.handler;

import com.danlind.igz.brokerapi.BrokerLogin;
import com.danlind.igz.domain.types.AccountType;
import com.danlind.igz.ig.api.client.rest.ConversationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
