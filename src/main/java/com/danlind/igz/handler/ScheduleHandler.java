package com.danlind.igz.handler;

import com.danlind.igz.Zorro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

/**
 * Created by danlin on 2017-03-29.
 */
@Component
public class ScheduleHandler {

    private final static Logger logger = LoggerFactory.getLogger(ScheduleHandler.class);
    private final ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    public ScheduleHandler(ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        this.threadPoolTaskScheduler = threadPoolTaskScheduler;
    }

    public ScheduledFuture indicateProgress() {
        logger.debug("Indicating progress to Zorro");
        ScheduledFuture future = threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
            Zorro.callProgress(1);
        }, 250);

        return future;
    }

}
