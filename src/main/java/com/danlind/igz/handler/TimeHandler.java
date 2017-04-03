package com.danlind.igz.handler;

import com.danlind.igz.Zorro;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.StreamingAPI;
import com.danlind.igz.ig.api.client.streaming.HandyTableListenerAdapter;
import com.danlind.igz.time.TimeConvert;
import com.lightstreamer.ls_client.UpdateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;

@Component
public class TimeHandler {

    private static final int DAYS_SINCE_UTC_EPOCH = 25569;

    private final static Logger logger = LoggerFactory.getLogger(TimeHandler.class);

    private final ArrayList<HandyTableListenerAdapter> listeners;
    private final StreamingAPI streamingAPI;
    private final MarketHandler marketHandler;
    private final ScheduleHandler scheduleHandler;
    private final RestAPI restAPI;
    private final LoginHandler loginHandler;

    private long currentTimeMillis = 0;
    private long lastCheck = 0;
    private int lastTradableStatus = 0;

    @Autowired
    public TimeHandler(StreamingAPI streamingAPI, ArrayList<HandyTableListenerAdapter> listeners, MarketHandler marketHandler, ScheduleHandler scheduleHandler, RestAPI restAPI, LoginHandler loginHandler) {
        this.streamingAPI = streamingAPI;
        this.listeners = listeners;
        this.marketHandler = marketHandler;
        this.scheduleHandler = scheduleHandler;
        this.restAPI = restAPI;
        this.loginHandler = loginHandler;
    }

    /**
     * Subscribe to Lightstreamer heartbeat which arrives every second.
     * Heartbeat sends seconds since epoch, so multiply by 1000 for millis
     */
    public void subscribeToLighstreamerHeartbeat() {
        logger.info("Subscribing to Lightstreamer heartbeat");
        try {
            listeners.add(streamingAPI.subscribe(new HandyTableListenerAdapter() {
                @Override
                public void onUpdate(int i, String s, UpdateInfo updateInfo) {
                    currentTimeMillis = Long.parseLong(updateInfo.getNewValue(1)) * 1000;
                }
            }, new String[]{"TRADE:HB.U.HEARTBEAT.IP"}, "MERGE", new String[]{"HEARTBEAT"}));
        } catch (Exception e) {
            logger.error("Error while subscribing to Lightstreamer heartbeat", e);
            Zorro.indicateError();
        }
    }

    /**
     * Only check TRADABLE twice times per minute to avoid exhausting API limits (max 30 non-trading requests per minute)
     * @param pTimeUTC
     * @return
     */
    //TODO: Allow user to configure time refresh interval
    public int getBrokerTime(final double pTimeUTC[]) {
        if (currentTimeMillis == 0) {
            try {
                currentTimeMillis = restAPI.getEncryptionKeySessionV1(loginHandler.getConversationContext()).getTimeStamp();
            } catch (Exception e) {
                logger.error("Failed getting time from encryptionSession", e);
            }
//            indicateWait();
        }
        if (lastCheck == 0) {
            lastTradableStatus = marketHandler.isAnySubscribedEpicTradable();
        } else if (lastCheck != currentTimeMillis / 30000L) {
            lastCheck = currentTimeMillis / 30000L;
            lastTradableStatus = marketHandler.isAnySubscribedEpicTradable();
        }
        pTimeUTC[0] = TimeConvert.getOLEDateFromMillis(currentTimeMillis);
        return lastTradableStatus;
    }

    private void indicateWait() {
        ScheduledFuture future = scheduleHandler.indicateProgress();
        while (currentTimeMillis == 0) {
            try {
                logger.debug("Waiting for initial time from server");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error("Interrupted while sleeping", e);
            }
        }
        logger.debug("Initial server time received");
        future.cancel(true);
    }


}
