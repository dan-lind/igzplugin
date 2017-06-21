package com.danlind.igz.brokerapi;

import com.danlind.igz.adapter.RestApiAdapter;
import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.domain.types.Resolution;
import com.danlind.igz.ig.api.client.rest.dto.prices.getPricesV3.GetPricesV3Response;
import com.danlind.igz.ig.api.client.rest.dto.prices.getPricesV3.PricesItem;
import com.danlind.igz.misc.TimeConvert;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.danlind.igz.domain.types.Resolution.*;

@Component
public class BrokerHistory {

    private final static Logger logger = LoggerFactory.getLogger(BrokerHistory.class);
    private static final String PAGE_NUMBER = "1";
    private final RestApiAdapter restApiAdapter;
    private int accountZoneOffset;
    private Disposable timeZoneOffsetSubscription;

    @Autowired
    public BrokerHistory(RestApiAdapter restApiAdapter) {
        this.restApiAdapter = restApiAdapter;
    }


    public int getPriceHistory(final Epic epic,
                               final double tStart,
                               final double tEnd,
                               final int nTickMinutes,
                               final int nTicks,
                               final double tickParams[]) {

        Resolution resolution = checkValidResolution(nTickMinutes);
        if (resolution != Resolution.INVALID) {
            return fillParamsForValidResolution(epic, tStart, tEnd, nTicks, tickParams, resolution);
        } else {
            logger.info("Unable to find valid resolution for {}", resolution.name());
            return ZorroReturnValues.HISTORY_UNAVAILABLE.getValue();
        }
    }

    public void getTimeZoneOffsetObservable() {
        if (Objects.nonNull(timeZoneOffsetSubscription)) {
            logger.debug("Disposing of existing time offset subscription");
            timeZoneOffsetSubscription.dispose();
        }
        timeZoneOffsetSubscription = restApiAdapter.getTimeZoneOffset()
            .subscribe(
                timeZone -> {
                    logger.debug("Updating time zone offset");
                    accountZoneOffset = timeZone;
                }
            );
    }

    public void cancelTimeOffsetSubscription() {
        timeZoneOffsetSubscription.dispose();
    }

    private int fillParamsForValidResolution(Epic epic, double tStart, double tEnd, int nTicks, double[] tickParams, Resolution resolution) {
        LocalDateTime endDateTime = TimeConvert.localDateTimeFromOLEDate(tEnd, accountZoneOffset);
//        LocalDateTime startDateTime = TimeConvert.localDateTimeFromOLEDate(tStart, accountZoneOffset);
//        startDateTime = alignStartDateToResolutionLimit(startDateTime, endDateTime, resolution);
        LocalDateTime startDateTime = endDateTime.minusMinutes(nTicks * resolution.getValue());
        logger.debug("Getting prices for epic {}, date range {} - {}, max ticks {}, resolution {}", epic.getName(), startDateTime, endDateTime, nTicks, resolution.name());
        GetPricesV3Response response = restApiAdapter.getHistoricPrices(PAGE_NUMBER,
            String.valueOf(nTicks),
            String.valueOf(nTicks),
            epic.getName(),
            startDateTime.toString(),
            endDateTime.toString(),
            resolution.name());

        if (response.getPrices().size() == 0) {
            logger.warn("Zero ticks returned for requested date range {} - {}", startDateTime, endDateTime);
            return ZorroReturnValues.HISTORY_UNAVAILABLE.getValue();
        }

//        response.getPrices().forEach(priceItem -> {
//            logger.debug(LocalDateTime.parse(priceItem.getSnapshotTimeUTC()).toString());
//        });

        int tickParamsIndex = 0;
        Collections.reverse(response.getPrices());
        for (int i = 0; i < response.getPrices().size(); ++i) {
            final PricesItem priceItem = response.getPrices().get(i);
            tickParams[tickParamsIndex] = priceItem.getOpenPrice().getAsk().doubleValue();
            tickParams[tickParamsIndex + 1] = priceItem.getClosePrice().getAsk().doubleValue();
            tickParams[tickParamsIndex + 2] = priceItem.getHighPrice().getAsk().doubleValue();
            tickParams[tickParamsIndex + 3] = priceItem.getLowPrice().getAsk().doubleValue();
            tickParams[tickParamsIndex + 4] = TimeConvert.getOLEDateFromMillisRounded(LocalDateTime.parse(priceItem.getSnapshotTimeUTC()).toInstant(ZoneOffset.UTC).toEpochMilli());
            tickParams[tickParamsIndex + 5] = priceItem.getClosePrice().getAsk().subtract(priceItem.getClosePrice().getBid()).doubleValue();
            tickParams[tickParamsIndex + 6] = priceItem.getLastTradedVolume().doubleValue();
            tickParamsIndex += 7;
        }

        return response.getPrices().size();
    }

    public List<PricesItem> getPriceHistory(final Epic epic,
                                            final int ticks) {
        return restApiAdapter.getHistoricPrices(PAGE_NUMBER,
            Integer.toString(ticks),
            Integer.toString(ticks),
            epic.getName(),
            null,
            null,
            MINUTE.name()).getPrices();
    }

    /*
    These are indications of maximum number of days of historic data available for different time resolutions
    This method makes sure we don't look further back than these dates to avoid io errors from IGs API.
        Resolution	Days
        1 Min	40
        2 Min	40
        3 Min	40
        5 Min	360
        10 Min	360
        15 Min	360
        30 Min	360
        1 Hour	360
        2 Hour	360
        3 Hour	360
        4 Hour	360
        1 Day	15 years
    */
    private LocalDateTime alignStartDateToResolutionLimit(LocalDateTime startTime, LocalDateTime endTime, Resolution resolution) {
        switch (resolution) {
            case MINUTE:
            case MINUTE_2:
            case MINUTE_3: {
                if (ChronoUnit.DAYS.between(startTime, endTime) > 40) {
                    LocalDateTime newStartDate = endTime.minusDays(40);
                    logger.info("Time resolution {} exceeded maxumum of 40 days, adjusting start date to {}", resolution, newStartDate);
                    return newStartDate;
                } else {
                    return endTime;
                }
            }
            case MINUTE_5:
            case MINUTE_10:
            case MINUTE_15:
            case MINUTE_30:
            case HOUR:
            case HOUR_2:
            case HOUR_3:
            case HOUR_4: {
                if (ChronoUnit.DAYS.between(startTime, endTime) > 360) {
                    LocalDateTime newStartDate = endTime.minusDays(360);
                    logger.info("Time resolution {} exceeded maxumum of 360 days, adjusting start date to {}", resolution, newStartDate);
                    return newStartDate;
                } else {
                    return endTime;
                }
            }
            case DAY: {
                if (ChronoUnit.YEARS.between(startTime, endTime) > 15) {
                    LocalDateTime newStartDate = endTime.minusYears(15);
                    logger.info("Time resolution {} exceeded maxumum of 15 years, adjusting start date to {}", resolution, newStartDate);
                    return newStartDate;
                } else {
                    return endTime;
                }
            }
            default: {
                throw new RuntimeException("Invalid resolution passed to function, this should never occur");
            }
        }
    }

    private Resolution checkValidResolution(int nTickMinutes) {
        switch (nTickMinutes) {
            case 1:
                return MINUTE;
            case 2:
                return MINUTE_2;
            case 3:
                return MINUTE_3;
            case 5:
                return MINUTE_5;
            case 10:
                return MINUTE_10;
            case 15:
                return MINUTE_15;
            case 30:
                return MINUTE_30;
            case 60:
                return HOUR;
            case 120:
                return HOUR_2;
            case 180:
                return HOUR_3;
            case 240:
                return HOUR_4;
            case 1440:
                return DAY;
            default:
                return INVALID;
        }
    }
}
