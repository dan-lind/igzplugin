package com.danlind.igz.brokerapi;

import com.danlind.igz.Zorro;
import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.adapter.StreamingApiAdapter;
import com.danlind.igz.misc.MarketDataProvider;
import com.danlind.igz.misc.TimeConvert;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class BrokerTime {

    private final static Logger logger = LoggerFactory.getLogger(BrokerTime.class);

    private final StreamingApiAdapter streamingApiAdapter;
    private final MarketDataProvider marketDataProvider;
    private final RestApiAdapter restApiAdapter;

    private long currentTimeMillis = 0;
    private int lastTradableStatus = 0;
    private boolean isConnected = false;

    @Autowired
    public BrokerTime(StreamingApiAdapter streamingApiAdapter, MarketDataProvider marketDataProvider, RestApiAdapter restApiAdapter) {
        this.streamingApiAdapter = streamingApiAdapter;
        this.marketDataProvider = marketDataProvider;
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
                },
                () -> {
                    logger.info("Disconnected from heartbeat stream");
                    isConnected = false;
                }
            );

        isConnected = true;
    }

    /**
     * Get current time from the broker
     * Uses IGs streaming API to receive heartbeats every second
     * If currentTimeMillis is zero it means we did not yet get the first heartbeat, so we get time from the REST API instead.
     * This typically happens on login. If the stream is disconnected, returning 0 will cause Zorro to request a new login
     * @param pTimeUTC
     * @return
     */
    public int getBrokerTime(final double pTimeUTC[]) {
        try {
            if (currentTimeMillis == 0) {
                logger.info("Getting initial time from restAPI");
                Disposable disposable = indicateProgress();
                currentTimeMillis = restApiAdapter.getServerTime().blockingGet();
                disposable.dispose();
            } else if (!isConnected) {
                return 0;
            }

            lastTradableStatus = marketDataProvider.isAnySubscribedEpicTradable();
            pTimeUTC[0] = TimeConvert.getOLEDateFromMillis(currentTimeMillis);

            return lastTradableStatus;
        } catch (Exception e) {
            logger.error("Time handler exception", e);
            throw new RuntimeException();
        }
    }

    private Disposable indicateProgress() {
        return Observable.interval(250, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribe(x -> Zorro.callProgress(1));
    }


    private void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
    }

}
