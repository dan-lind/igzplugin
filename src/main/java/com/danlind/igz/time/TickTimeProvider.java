package com.danlind.igz.time;

public class TickTimeProvider {
//
//    private final TickQuoteRepository tickQuoteRepository;
//    private final Clock clock;
//    private long latestTickTime;
//    private long synchTime;
//
//    private final static Logger logger = LoggerFactory.getLogger(TickTimeProvider.class);
//
//    public TickTimeProvider(final TickQuoteRepository tickQuoteRepository,
//                            final Clock clock) {
//        this.tickQuoteRepository = tickQuoteRepository;
//        this.clock = clock;
//    }
//
//    public long get() {
//        final long latestTickTimeFromRepository = fromRepository();
//
//        return latestTickTimeFromRepository == ZorroReturnValues.INVALID_SERVER_TIME.getValue()
//                ? ZorroReturnValues.INVALID_SERVER_TIME.getValue()
//                : getForValidTickTime(latestTickTimeFromRepository);
//    }
//
//    private long getForValidTickTime(final long latestTickTimeFromRepository) {
//        return latestTickTimeFromRepository > latestTickTime
//                ? setNewAndSynch(latestTickTimeFromRepository)
//                : estimateWithSynchTime();
//    }
//
//    private long fromRepository() {
//        logger.debug("Fetching tick times from repository...");
//        return tickQuoteRepository
//            .getAll()
//            .values()
//            .stream()
//            .map(TickQuote::tick)
//            .mapToLong(tick -> {
//                final long tickTime = tick.getTime();
//                logger.debug("Found latest tick time " + DateTimeUtil.formatMillis(tickTime));
//                return tickTime;
//            })
//            .max()
//            .orElseGet(() -> ZorroReturnValues.INVALID_SERVER_TIME.getValue());
//    }
//
//    private long setNewAndSynch(final long latestTickTimeFromRepository) {
//        latestTickTime = latestTickTimeFromRepository;
//        synchTime = clock.millis();
//        return latestTickTime;
//    }
//
//    private long estimateWithSynchTime() {
//        final long timeDiffToSynchTime = clock.millis() - synchTime;
//        return latestTickTime + timeDiffToSynchTime;
//    }
}
