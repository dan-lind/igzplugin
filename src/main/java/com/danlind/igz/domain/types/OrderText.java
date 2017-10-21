package com.danlind.igz.domain.types;

import java.util.Objects;

/**
 * Created by danlin on 2017-10-20.
 */
public class OrderText {
    private final String orderText;

    public OrderText(String orderText) {
        Objects.requireNonNull(orderText);
        this.orderText = orderText;
    }

    public String getValue() {
        return orderText;
    }

}
