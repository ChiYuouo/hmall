package com.hmall.trade.constant;

public interface MQConstant {

    String DELAY_EXCHANGE_NAME="trade.delay.direct";
    String DELAY_ORDER_QUEUE_NAME="trade.delay.order.queue";
    String DELAY_ORDER_KEY="dalay.order.query";
    Integer ORDER_DELAY_TIME = 30 * 60 * 1000;
}
