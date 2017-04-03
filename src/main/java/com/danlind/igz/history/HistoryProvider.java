package com.danlind.igz.history;

public class HistoryProvider {

//    private final IHistory history;
//    private final PluginConfig pluginConfig;
//
//    private final static Logger logger = LoggerFactory.getLogger(HistoryProvider.class);
//
//    public HistoryProvider(final IHistory history,
//                           final PluginConfig pluginConfig) {
//        this.history = history;
//        this.pluginConfig = pluginConfig;
//    }
//
//    public Observable<List<IBar>> fetchBars(final Instrument instrument,
//                                            final Period period,
//                                            final OfferSide offerSide,
//                                            final long startTime,
//                                            final long endTime) {
//        return Observable
//            .fromCallable(() -> history.getBars(instrument,
//                                                period,
//                                                offerSide,
//                                                startTime,
//                                                endTime))
//            .doOnSubscribe(d -> logger.debug("Starting to fetch bars for " + instrument + "\n"
//                    + "period: " + period + "\n"
//                    + "offerSide: " + offerSide + "\n"
//                    + "startTime: " + DateTimeUtil.formatMillis(startTime) + "\n"
//                    + "endTime: " + DateTimeUtil.formatMillis(endTime)))
//            .doOnComplete(() -> logger.debug("Fetching bars for " + instrument + " completed."))
//            .doOnError(err -> logger.error("Fetch bars failed! " + err.getMessage()))
//            .retryWhen(this::historyRetryWhen)
//            .onErrorResumeNext(Observable.just(new ArrayList<>()));
//    }
//
//    private ObservableSource<Long> historyRetryWhen(final Observable<Throwable> errors) {
//        return errors
//            .zipWith(Observable.range(1, pluginConfig.historyDownloadRetries() + 1), Pair::of)
//            .flatMap(retryParams -> {
//                final Throwable err = retryParams.getLeft();
//                final long retryCount = retryParams.getRight();
//                return retryCount > pluginConfig.historyDownloadRetries()
//                        ? Observable.error(err)
//                        : Observable.timer(pluginConfig.historyRetryDelay(), TimeUnit.MILLISECONDS);
//            });
//    }
//
//    public long latestFormedBarTime(final Instrument instrument,
//                                    final Period period,
//                                    final OfferSide offerSide) {
//        return Observable
//            .fromCallable(() -> history.getBar(instrument, period, offerSide, 1))
//            .map(bar -> bar.getTime())
//            .doOnSubscribe(d -> logger.debug("Trying to get latest formed bar time for instrument "
//                    + instrument + " and period " + period))
//            .doOnError(err -> logger.error("Get latest formed bar time failed! " + err.getMessage()))
//            .retryWhen(this::historyRetryWhen)
//            .onErrorResumeNext(Observable.just(0L))
//            .blockingFirst();
//    }
//
//    public long previousBarStart(final Period period,
//                                 final long time) {
//        return Observable
//            .fromCallable(() -> history.getPreviousBarStart(period, time))
//            .doOnSubscribe(d -> logger.debug("Trying to get previous bar time for period "
//                    + period + " and current time " + DateTimeUtil.formatMillis(time)))
//            .doOnError(err -> logger.error("Getting previous bar time failed! " + err.getMessage()))
//            .retryWhen(this::historyRetryWhen)
//            .onErrorResumeNext(Observable.just(0L))
//            .blockingFirst();
//    }
//
//    public Observable<List<ITick>> fetchTicks(final Instrument instrument,
//                                              final long startTime,
//                                              final long endTime) {
//        return Observable
//            .fromCallable(() -> history.getTicks(instrument,
//                                                 startTime,
//                                                 endTime))
//            .doOnSubscribe(d -> logger.debug("Starting to fetch ticks for " + instrument
//                    + "startTime: " + DateTimeUtil.formatMillis(startTime)
//                    + "endTime: " + DateTimeUtil.formatMillis(endTime)))
//            .doOnComplete(() -> logger.debug("Fetching ticks for " + instrument + " completed."))
//            .doOnError(err -> logger.error("Fetch ticks failed! " + err.getMessage()))
//            .retryWhen(this::historyRetryWhen)
//            .onErrorResumeNext(Observable.just(new ArrayList<>()));
//    }
//
//    public Observable<List<IOrder>> ordersByInstrument(final Instrument instrument,
//                                                       final long from,
//                                                       final long to) {
//        return Observable
//            .fromCallable(() -> history.getOrdersHistory(instrument,
//                                                         from,
//                                                         to))
//            .doOnSubscribe(d -> logger.debug("Fetching history orders for " + instrument))
//            .retryWhen(this::historyRetryWhen)
//            .doOnError(err -> logger.error("Seeking history orders for "
//                    + instrument + " failed! " + err.getMessage()))
//            .onErrorResumeNext(Observable.just(new ArrayList<>()));
//    }
}
