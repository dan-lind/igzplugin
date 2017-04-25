package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.adapter.StreamingApiAdapter;
import com.danlind.igz.misc.MarketDataProvider;
import com.danlind.igz.misc.TimeConvert;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

@Component
public class BrokerTime {

    private final static Logger logger = LoggerFactory.getLogger(BrokerTime.class);

    private final StreamingApiAdapter streamingApiAdapter;
    private final MarketDataProvider marketDataProvider;
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;
    private final RestApiAdapter restApiAdapter;

    private long currentTimeMillis = 0;
    private long lastCheck = 0;
    private int lastTradableStatus = 0;

    @Autowired
    public BrokerTime(StreamingApiAdapter streamingApiAdapter, MarketDataProvider marketDataProvider, ThreadPoolTaskScheduler threadPoolTaskScheduler, RestApiAdapter restApiAdapter) {
        this.streamingApiAdapter = streamingApiAdapter;
        this.marketDataProvider = marketDataProvider;
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
        this.restApiAdapter = restApiAdapter;
    }

    /**
     * Subscribe to Lightstreamer heartbeat which arrives every second.
     * Heartbeat sends seconds since epoch, so multiply by 1000 for millis
     */
    public void subscribeToLighstreamerHeartbeat() {
        logger.info("Subscribing to Lightstreamer heartbeat");

        streamingApiAdapter.getHeartbeatObservable()
            .subscribeOn(Schedulers.io())
            .subscribe(
                this::setCurrentTimeMillis,
                error -> {
                    logger.error("Error while subscribing to Lightstreamer heartbeat", error);
                    Zorro.indicateError();
                });
    }

    //TODO: Allow user to configure time refresh interval
    public int getBrokerTime(final double pTimeUTC[]) {
        try {
            if (currentTimeMillis == 0) {
                logger.info("Getting initial time from restAPI");
                ScheduledFuture future = indicateProgress();
                currentTimeMillis = restApiAdapter.getServerTime();
                future.cancel(true);
            }
            if (lastCheck == 0) {
                lastTradableStatus = marketDataProvider.isAnySubscribedEpicTradable();
            } else if (lastCheck != currentTimeMillis / 30000L) {
                lastCheck = currentTimeMillis / 30000L;
                lastTradableStatus = marketDataProvider.isAnySubscribedEpicTradable();
            }
            pTimeUTC[0] = TimeConvert.getOLEDateFromMillis(currentTimeMillis);

            return lastTradableStatus;
        } catch (Exception e) {
            logger.error("Time handler exception", e);
            throw new RuntimeException();
        }
    }

    private ScheduledFuture indicateProgress() {
        return threadPoolTaskScheduler.scheduleAtFixedRate(() -> Zorro.callProgress(1), 250);
    }


    private void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
    }

}
