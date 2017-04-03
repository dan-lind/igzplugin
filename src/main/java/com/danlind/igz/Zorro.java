package com.danlind.igz;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Zorro {

//    private final PluginConfig pluginConfig;
    private int taskResult;
    private final Observable<Integer> heartBeat;

    private final static int running = 10;
    private final static int heartBeatIndication = 1;
    private final static Logger logger = LoggerFactory.getLogger(Zorro.class);

    public Zorro() {
        heartBeat = Observable
            .interval(0L,
                      250,
                      TimeUnit.MILLISECONDS)
            .map(i -> running);
    }

    public int progressWait(final Observable<Integer> task) {
        Observable
            .merge(heartBeat, task)
            .subscribeOn(Schedulers.io())
            .takeUntil(i -> i != running)
            .blockingSubscribe(i -> {
                if (i != running)
                    taskResult = i;
                else
                    callProgress(heartBeatIndication);
            });

        return taskResult;
    }

    public static int callProgress(final int progress) {
        return jcallback_BrokerProgress(progress);
    }

    public static int logError(final String errorMsg,
                               final Logger logger) {
        logger.error(errorMsg);
        return logError(errorMsg);
    }

    public static int logError(final String errorMsg) {
        return jcallback_BrokerError(errorMsg);
    }

    public static void logDiagnose(final String errorMsg) {
        logError("#" + errorMsg);
    }

    public static void logPopUp(final String errorMsg) {
        logError("!" + errorMsg);
    }

    public static void indicateError() {
        logError("Severe error occured, check igzplugin.log logfile!");
    }

    public static void showError(final String errorMsg) {
        logError(errorMsg);
    }

    private static native int jcallback_BrokerError(String errorMsg);

    private static native int jcallback_BrokerProgress(int progress);
}
