package com.danlind.igz.time;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NTPFetch {

//    private final Observable<Long> observable;

    private final static Logger logger = LoggerFactory.getLogger(NTPFetch.class);

//    public NTPFetch(final PluginConfig pluginConfig) {
//        final NTPUDPClient ntpUDPClient = new NTPUDPClient();
//        final long retryDelay = pluginConfig.ntpRetryDelay();
//
//        observable = Observable
//            .fromCallable(() -> fetchFromURL(ntpUDPClient, pluginConfig.ntpServerURL()))
//            .doOnSubscribe(d -> logger.debug("Fetching NTP now..."))
//            .doOnError(err -> logger.debug("NTP fetch task failed with error: " + err.getMessage()
//                    + ". Will retry in " + retryDelay + " milliseconds."))
//            .retryWhen(errors -> errors.flatMap(error -> Observable.timer(retryDelay, TimeUnit.MILLISECONDS)));
//    }
//
//    private long fetchFromURL(final NTPUDPClient ntpUDPClient,
//                              final String ntpServerURL) throws Exception {
//        final InetAddress inetAddress = InetAddress.getByName(ntpServerURL);
//        final TimeInfo timeInfo = ntpUDPClient.getTime(inetAddress);
//        final NtpV3Packet ntpV3Packet = timeInfo.getMessage();
//        final TimeStamp timeStamp = ntpV3Packet.getTransmitTimeStamp();
//        return timeStamp.getTime();
//    }

//    public Observable<Long> observable() {
//        return observable;
//    }
}
