package com.danlind.igz.brokerapi;

public class BrokerStop {

//    private final OrderSetSL orderSetSL;
//    private final TradeUtil tradeUtil;
//
//    private final static Logger logger = LoggerFactory.getLogger(BrokerStop.class);
//
//    public BrokerStop(final OrderSetSL orderSetSL,
//                      final TradeUtil tradeUtil) {
//        this.orderSetSL = orderSetSL;
//        this.tradeUtil = tradeUtil;
//    }
//
//    public int setSL(final int nTradeID,
//                     final double dStop) {
//        final IOrder order = tradeUtil.orderByID(nTradeID);
//        if (order == null)
//            return ZorroReturnValues.ADJUST_SL_FAIL.getValue();
//        if (!tradeUtil.isTradingAllowed())
//            return ZorroReturnValues.ADJUST_SL_FAIL.getValue();
//
//        logger.info("Trying to set stop loss for order ID " + nTradeID
//                + " and dStop " + dStop);
//        return setSLForValidOrderID(order, dStop);
//    }
//
//    private int setSLForValidOrderID(final IOrder order,
//                                     final double dStop) {
//        final double slPrice = MathUtil.roundPrice(dStop, order.getInstrument());
//        if (tradeUtil.isSLPriceDistanceOK(order.getInstrument(), slPrice))
//            orderSetSL.run(order, slPrice);
//
//        return ZorroReturnValues.ADJUST_SL_OK.getValue();
//    }
}
