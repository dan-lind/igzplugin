package com.danlind.igz.domain.types;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by danlin on 2017-03-21.
 */
public class DealId implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String dealreference;

    public DealId(String dealreference) {
        Objects.requireNonNull(dealreference);
        this.dealreference = dealreference;
    }

    public String getValue() {
        return dealreference;
    }
}
