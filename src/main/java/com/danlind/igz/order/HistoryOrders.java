package com.danlind.igz.order;

public class HistoryOrders {

//    private final HistoryProvider historyProvider;
//    private final BrokerSubscribe brokerSubscribe;
//    private final PluginConfig pluginConfig;
//    private final ServerTimeProvider serverTimeProvider;
//
//    private final static Logger logger = LogManager.getLogger(HistoryOrders.class);
//
//    public HistoryOrders(final HistoryProvider historyProvider,
//                         final BrokerSubscribe brokerSubscribe,
//                         final PluginConfig pluginConfig,
//                         final ServerTimeProvider serverTimeProvider) {
//        this.historyProvider = historyProvider;
//        this.brokerSubscribe = brokerSubscribe;
//        this.pluginConfig = pluginConfig;
//        this.serverTimeProvider = serverTimeProvider;
//    }
//
//    public List<IOrder> get() {
//        final LocalDateTime toDate = DateTimeUtil.dateTimeFromMillis(serverTimeProvider.get());
//        final LocalDateTime fromDate = toDate.minusDays(pluginConfig.historyOrderInDays());
//        final long to = DateTimeUtil.millisFromDateTime(toDate);
//        final long from = DateTimeUtil.millisFromDateTime(fromDate);
//        logger.debug("Fetching history orders for " + brokerSubscribe.subscribedInstruments()
//                + " from " + DateTimeUtil.formatMillis(from)
//                + " to " + DateTimeUtil.formatMillis(to));
//
//        final List<IOrder> orders = brokerSubscribe
//            .subscribedInstruments()
//            .stream()
//            .map(instrument -> getForInstrument(instrument,
//                                                from,
//                                                to))
//            .flatMap(List::stream)
//            .collect(Collectors.toList());
//        logger.debug("Fetched " + orders.size() + " history orders for " + brokerSubscribe.subscribedInstruments());
//
//        return orders;
//    }
//
//    private List<IOrder> getForInstrument(final Instrument instrument,
//                                          final long from,
//                                          final long to) {
//        final List<IOrder> orders = historyProvider
//            .ordersByInstrument(instrument,
//                                from,
//                                to)
//            .blockingFirst();
//
//        logger.debug("Fetched " + orders.size() + " history orders for " + instrument);
//
//        return orders;
//    }
}
