package com.danlind.igz.order;

public class RunningOrders {

//    private final IEngine engine;
//
//    private final static Logger logger = LogManager.getLogger(RunningOrders.class);
//
//    public RunningOrders(final IEngine engine) {
//        this.engine = engine;
//    }
//
//    public List<IOrder> get() {
//        final List<IOrder> orders = Observable
//            .fromCallable(() -> engine.getOrders())
//            .doOnSubscribe(d -> logger.debug("Fetching running orders..."))
//            .onErrorResumeNext(err -> {
//                logger.error("Error while fetching running orders!" + err.getMessage());
//                return Observable.just(new ArrayList<>());
//            })
//            .flatMap(Observable::fromIterable)
//            .toList()
//            .blockingGet();
//        logger.debug("Fetched " + orders.size() + " runnings orders.");
//
//        return orders;
//    }
}
