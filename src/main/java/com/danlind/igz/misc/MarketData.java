package com.danlind.igz.misc;

public class MarketData {

//    private final IDataService dataService;
//
//    private final static Logger logger = LoggerFactory.getLogger(MarketData.class);
//
//    public MarketData(final IDataService dataService) {
//        this.dataService = dataService;
//    }
//
//    public boolean isMarketOffline(final long currentServerTime) {
//        final long lookUpEnTime = currentServerTime + Period.ONE_MIN.getInterval();
//
//        return Observable
//            .fromCallable(() -> dataService.getOfflineTimeDomains(currentServerTime, lookUpEnTime))
//            .map(domains -> isServerTimeInOfflineDomains(currentServerTime, domains))
//            .onErrorResumeNext(err -> {
//                logger.error("Get market offline times  failed!" + err.getMessage());
//                return Observable.just(true);
//            })
//            .blockingFirst();
//    }
//
//    private boolean isServerTimeInOfflineDomains(final long serverTime,
//                                                 final Set<ITimeDomain> offlineDomains) {
//        return offlineDomains
//            .stream()
//            .anyMatch(timeDomain -> {
//                return serverTime >= timeDomain.getStart() && serverTime <= timeDomain.getEnd();
//            });
//    }
}
