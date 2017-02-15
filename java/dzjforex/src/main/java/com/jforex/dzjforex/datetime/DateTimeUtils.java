package com.jforex.dzjforex.datetime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dukascopy.api.IBar;
import com.dukascopy.api.IDataService;
import com.dukascopy.api.ITimeDomain;
import com.dukascopy.api.JFException;
import com.dukascopy.api.Period;
import com.jforex.dzjforex.ZorroLogger;
import com.jforex.dzjforex.config.ReturnCodes;

public class DateTimeUtils {

    private static final HashMap<Integer, Period> minuteToPeriodMap;
    private static final int DAYS_SINCE_UTC_EPOCH = 25569;
    private static SimpleDateFormat simpleUTCormat;
    private final static Logger logger = LogManager.getLogger(DateTimeUtils.class);

    static {
        minuteToPeriodMap = new HashMap<Integer, Period>();

        minuteToPeriodMap.put(1440, Period.DAILY);
        minuteToPeriodMap.put(60, Period.ONE_HOUR);
        minuteToPeriodMap.put(30, Period.THIRTY_MINS);
        minuteToPeriodMap.put(15, Period.FIFTEEN_MINS);
        minuteToPeriodMap.put(5, Period.FIVE_MINS);
        minuteToPeriodMap.put(1, Period.ONE_MIN);
        minuteToPeriodMap.put(0, Period.TICK);

        simpleUTCormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        simpleUTCormat.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));
    }

    private final IDataService dataService;
    private final ServerTime serverTime;

    public DateTimeUtils(final IDataService dataService,
                         final ServerTime serverTime) {
        this.dataService = dataService;
        this.serverTime = serverTime;
    }

    public int doBrokerTime(final double serverTimeData[]) {
        final long currentServerTime = serverTime.get();
        serverTimeData[0] = DateTimeUtils.getOLEDateFromMillis(currentServerTime);

        final boolean isMarketOffline = isMarketOffline(currentServerTime);
        if (isMarketOffline)
            logger.debug("Market is offline");

        return isMarketOffline
                ? ReturnCodes.CONNECTION_OK_BUT_MARKET_CLOSED
                : ReturnCodes.CONNECTION_OK;
    }

    private boolean isMarketOffline(final long currentServerTime) {
        final Set<ITimeDomain> offlines = getOfflineTimes(currentServerTime,
                                                          currentServerTime + Period.ONE_MIN.getInterval());
        return offlines == null
                ? true
                : isServerTimeInOfflineDomains(currentServerTime, offlines);
    }

    public static double getOLEDateFromMillis(final long millis) {
        return DAYS_SINCE_UTC_EPOCH + (double) millis / (1000f * 3600f * 24f);
    }

    public static double getOLEDateFromMillisRounded(final long millis) {
        return getOLEDateFromMillis(millis) + 1e-8;
    }

    public static long getMillisFromOLEDate(final double oleDate) {
        final Date date = new Date();
        date.setTime((long) ((oleDate - DAYS_SINCE_UTC_EPOCH) * 24 * 3600 * 1000));
        return date.getTime();
    }

    private boolean isServerTimeInOfflineDomains(final long serverTime,
                                                 final Set<ITimeDomain> offlines) {
        for (final ITimeDomain offline : offlines)
            if (serverTime >= offline.getStart() && serverTime <= offline.getEnd())
                return true;
        return false;
    }

    private Set<ITimeDomain> getOfflineTimes(final long startTime,
                                             final long endTime) {
        Set<ITimeDomain> offlineTimes = null;
        try {
            offlineTimes = dataService.getOfflineTimeDomains(startTime, endTime);
        } catch (final JFException e) {
            logger.error("getOfflineTimes exc: " + e.getMessage());
            ZorroLogger.indicateError();
        }
        return offlineTimes;
    }

    public static String formatDateTime(final long dateTime) {
        return simpleUTCormat.format(new Date(dateTime));
    }

    public static String formatOLETime(final double oleTime) {
        final long dateTime = getMillisFromOLEDate(oleTime);
        return formatDateTime(dateTime);
    }

    public static Period getPeriodFromMinutes(final int minutes) {
        return minuteToPeriodMap.get(minutes);
    }

    public static long getUTCYearStartTime(final int year) {
        return getUTCTime(year, 0, 1, 0, 0, 0);
    }

    public static long getUTCYearEndTime(final int year) {
        return getUTCTime(year, 11, 31, 23, 59, 0);
    }

    public static long getUTCTime(final int year,
                                  final int month,
                                  final int day,
                                  final int hour,
                                  final int min,
                                  final int sec) {
        final GregorianCalendar calendar = new GregorianCalendar(year, month, day, hour, min, sec);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        return calendar.getTimeInMillis();
    }

    public static double getUTCTimeFromBar(final IBar bar) {
        return getOLEDateFromMillisRounded(bar.getTime());
    }
}
