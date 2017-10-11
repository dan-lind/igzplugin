package com.danlind.igz.misc;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class RetryWithDelay implements Function<Observable<? extends Throwable>, Observable<?>> {
    private final static Logger LOG = LoggerFactory.getLogger(RetryWithDelay.class);
    private final int maxRetries;
    private final int retryDelayMillis;
    private int retryCount;

    public RetryWithDelay(final int maxRetries, final int retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.retryCount = 0;
    }

    @Override
    public Observable<?> apply(final Observable<? extends Throwable> attempts) {
        return attempts
            .flatMap(new Function<Throwable, Observable<?>>() {
                @Override
                public Observable<?> apply(final Throwable throwable) {
                    if (++retryCount < maxRetries) {
                        LOG.debug("Retry {} of {}", retryCount, maxRetries);
                        // When this Observable calls onNext, the original
                        // Observable will be retried (i.e. re-subscribed).
                        return Observable.timer(retryDelayMillis,
                            TimeUnit.MILLISECONDS);
                    }

                    LOG.debug("Max retries exceeded, passing error downstream");

                    // Max retries hit. Just pass the error along.
                    return Observable.error(throwable);
                }
            });
    }
}