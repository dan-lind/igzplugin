package com.danlind.igz.history;

public class BarFetcher {

//    private final HistoryProvider historyProvider;
//    private final StrategyUtil strategyUtil;
//    private List<IBar> fetchedBars;
//
//    private final static Logger logger = LoggerFactory.getLogger(BarFetcher.class);
//
//    public BarFetcher(final HistoryProvider historyProvider,
//                      final StrategyUtil strategyUtil) {
//        this.historyProvider = historyProvider;
//        this.strategyUtil = strategyUtil;
//    }
//
//    public int fetch(final Instrument instrument,
//                     final double startDate,
//                     final double endDate,
//                     final int tickMinutes,
//                     final int nTicks,
//                     final double tickParams[]) {
//        final long startMillis = TimeConvert.millisFromOLEDateRoundMinutes(startDate);
//        long endMillis = TimeConvert.millisFromOLEDateRoundMinutes(endDate);
//
//        logger.debug("Requested bars for instrument " + instrument + ": \n "
//                + "startDateUTCRaw: " + startDate + ": \n "
//                + "endDateUTCRaw: " + endDate + ": \n "
//                + "startDate: " + DateTimeUtil.formatMillis(startMillis)
//                + ": \n "
//                + "endDate: " + DateTimeUtil.formatMillis(endMillis)
//                + ": \n "
//                + "tickMinutes: " + tickMinutes + ": \n "
//                + "nTicks: " + nTicks);
//
//        final Period period = TimeConvert.getPeriodFromMinutes(tickMinutes);
//        final long latestTickTime = strategyUtil
//            .instrumentUtil(instrument)
//            .tickQuote()
//            .getTime();
//        if (endMillis > latestTickTime - period.getInterval()) {
//            endMillis = historyProvider.previousBarStart(period, latestTickTime);
//            logger.debug("Adapted endMillis for " + instrument + "are " + DateTimeUtil.formatMillis(endMillis));
//        }
//
//        final long startMillisAdapted = endMillis - (nTicks - 1) * period.getInterval();
//        fetchedBars = null;
//        historyProvider
//            .fetchBars(instrument,
//                       period,
//                       OfferSide.ASK,
//                       startMillisAdapted,
//                       endMillis)
//            .subscribeOn(Schedulers.io())
//            .subscribe(bars -> fetchedBars = bars);
//
//        while (fetchedBars == null) {
//            Zorro.callProgress(1);
//            Observable
//                .interval(0L,
//                          250L,
//                          TimeUnit.MILLISECONDS,
//                          Schedulers.io())
//                .blockingFirst();
//        }
//
//        logger.debug("Fetched " + fetchedBars.size() + " bars for " + instrument + " with nTicks " + nTicks);
//
//        return fetchedBars.isEmpty()
//                ? ZorroReturnValues.HISTORY_UNAVAILABLE.getValue()
//                : fillBars(fetchedBars, tickParams);
//    }
//
//    private int fillBars(final List<IBar> bars,
//                         final double tickParams[]) {
//        int tickParamsIndex = 0;
//        Collections.reverse(bars);
//        for (int i = 0; i < bars.size(); ++i) {
//            final IBar bar = bars.get(i);
//            tickParams[tickParamsIndex] = bar.getOpen();
//            tickParams[tickParamsIndex + 1] = bar.getClose();
//            tickParams[tickParamsIndex + 2] = bar.getHigh();
//            tickParams[tickParamsIndex + 3] = bar.getLow();
//            tickParams[tickParamsIndex + 4] = TimeConvert.getUTCTimeFromBar(bar);
//            // tickParams[tickParamsIndex + 5] = spread not available for bars
//            tickParams[tickParamsIndex + 6] = bar.getVolume();
//
//            tickParamsIndex += 7;
//        }
//        return bars.size();
//    }
}
