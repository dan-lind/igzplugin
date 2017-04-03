package com.danlind.igz.handler;


import com.danlind.igz.config.ZorroReturnValues;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.domain.types.Resolution;
import com.danlind.igz.ig.api.client.RestAPI;
import com.danlind.igz.ig.api.client.rest.dto.prices.getPricesV3.GetPricesV3Response;
import com.danlind.igz.ig.api.client.rest.dto.prices.getPricesV3.PricesItem;
import com.danlind.igz.time.TimeConvert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.danlind.igz.domain.types.Resolution.*;


@Component
public class HistoryHandler {

    private final static Logger logger = LoggerFactory.getLogger(HistoryHandler.class);
    private static final String PAGE_SIZE = "0";
    private static final String PAGE_NUMBER = "1";
    private final RestAPI restAPI;
    private final LoginHandler loginHandler;

    @Autowired
    public HistoryHandler(RestAPI restAPI, LoginHandler loginHandler) {
        this.restAPI = restAPI;
        this.loginHandler = loginHandler;
    }

    public int getPriceHistory(final Epic epic,
                               final double tStart,
                               final double tEnd,
                               final int nTickMinutes,
                               final int nTicks,
                               final double tickParams[]) {

        logger.debug("Finding resolution {}", nTickMinutes);
        Resolution resolution = checkValidResolution(nTickMinutes);
        if (resolution != Resolution.INVALID) {
            try {
                int accountZoneOffset = restAPI.getSessionV1(loginHandler.getConversationContext(), false).getBody().getTimezoneOffset();
                LocalDateTime endDateTime = TimeConvert.localDateTimeFromOLEDate(tEnd, accountZoneOffset);
                LocalDateTime startDateTime = calculateStartDateTime(endDateTime, nTicks, nTickMinutes);
                logger.debug("Getting prices for {} - {}, max ticks {}, resolution {}", startDateTime, endDateTime, nTicks, resolution.name());
                GetPricesV3Response response = restAPI.getPricesV3(loginHandler.getConversationContext(),
                        PAGE_NUMBER,
                        Integer.toString(nTicks),
                        PAGE_SIZE, epic.getName(),
                        startDateTime.toString(),
                        endDateTime.toString(),
                        resolution.name());

                int tickParamsIndex = 0;
                //        Collections.reverse(bars);
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


                int size = response.getPrices().size();
                logger.info("Getting first price for endDate {}; Bid: {}, Ask: {}, snapshot time {}, UTC {}", endDateTime, response.getPrices().get(0).getClosePrice().getBid().toString(), response.getPrices().get(0).getClosePrice().getAsk().toString(), response.getPrices().get(0).getSnapshotTime(), response.getPrices().get(0).getSnapshotTimeUTC());
                logger.info("Getting last price for endDate {}; Bid: {}, Ask: {}, snapshot time {}, UTC {}", endDateTime, response.getPrices().get(size - 1).getClosePrice().getBid().toString(), response.getPrices().get(size - 1).getClosePrice().getAsk().toString(), response.getPrices().get(size - 1).getSnapshotTime(), response.getPrices().get(size - 1).getSnapshotTimeUTC());
                return size;
            } catch (Exception e) {
                logger.error("Failed when getting history for epic {}", epic.getName(), e);
            }

        } else {
            logger.info("Unable to find valid resolution for {}", resolution.name());
            return ZorroReturnValues.HISTORY_UNAVAILABLE.getValue();
        }
        return 0;
    }

    public List<PricesItem> getPriceHistory(final Epic epic,
                                final int ticks) {
        try {
            GetPricesV3Response response = restAPI.getPricesV3(loginHandler.getConversationContext(),
                    PAGE_NUMBER,
                    Integer.toString(ticks),
                    PAGE_SIZE, epic.getName(),
                    null, null, MINUTE.name());
            return response.getPrices();
        } catch (Exception e) {
            logger.error("Unable to get historical prices for {}", epic.getName(), e);
            throw new RuntimeException(e);
        }
    }

    private LocalDateTime calculateStartDateTime(LocalDateTime endDateTime, int nTicks, int nTickMinutes) {
        return endDateTime.minus(nTicks * nTickMinutes, ChronoUnit.MINUTES);
    }

    //    DAY	1 day
//    HOUR	1 hour
//    HOUR_2	2 hours
//    HOUR_3	3 hours
//    HOUR_4	4 hours
//    MINUTE	1 minute
//    MINUTE_10	10 minutes
//    MINUTE_15	15 minutes
//    MINUTE_2	2 minutes
//    MINUTE_3	3 minutes
//    MINUTE_30	30 minutes
//    MINUTE_5	5 minutes
//    MONTH	1 month
//    SECOND	1 second
//    WEEK	1 week
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
