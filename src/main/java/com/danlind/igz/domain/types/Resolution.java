package com.danlind.igz.domain.types;

/**
 * Created by danlin on 2017-03-23.
 */
public enum  Resolution {
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

    DAY(1440),
    HOUR_4(240),
    HOUR_3(180),
    HOUR_2(120),
    HOUR(60),
    MINUTE_30(30),
    MINUTE_15(15),
    MINUTE_10(10),
    MINUTE_5(5),
    MINUTE_3(3),
    MINUTE_2(2),
    MINUTE(1),
    INVALID(0);

    private final int value;

    Resolution(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }
}
