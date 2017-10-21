package com.danlind.igz.misc;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class RetryWithDelay implements Function<Flowable<Throwable>, Publisher<?>> {

    private final static Logger LOG = LoggerFactory.getLogger(RetryWithDelay.class);
    private final int maxRetries;
    private final long retryDelayMillis;
    private int retryCount;

    public RetryWithDelay(final int maxRetries, final int retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.retryCount = 0;
    }

    @Override
    public Publisher<?> apply(Flowable<Throwable> throwableFlowable) throws Exception {
        return throwableFlowable.flatMap((Function<Throwable, Publisher<?>>) throwable -> {
            if (++retryCount < maxRetries) {
                LOG.debug("Retry {} of {}", retryCount, maxRetries);

                // When this Observable calls onNext, the original
                // Observable will be retried (i.e. re-subscribed).
                return Flowable.timer(retryDelayMillis,
                    TimeUnit.MILLISECONDS);
            }

            LOG.debug("Max retries exceeded, passing error downstream");

            // Max retries hit. Just pass the error along.
            return Flowable.error(throwable);
        });
    }
}