package com.danlind.igz.domain.types;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by danlin on 2017-03-21.
 */
public class DealReference implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String dealreference;

    public DealReference(String dealreference) {
        Objects.requireNonNull(dealreference);
        this.dealreference = dealreference;
    }

    public String getValue() {
        return dealreference;
    }
}
