package com.danlind.igz.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientProvider {

    private final static Logger logger = LoggerFactory.getLogger(ClientProvider.class);
//
//    public static IClient get() {
//        return Observable
//            .fromCallable(() -> ClientFactory.getDefaultInstance())
//            .doOnError(err -> logger.error("Error while login! " + err.getMessage()))
//            .blockingFirst();
//    }
}
