package com.danlind.igz.domain.types;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by danlin on 2017-03-21.
 */
public class DealId implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String dealId;

    public DealId(String dealId) {
        Objects.requireNonNull(dealId);
        this.dealId = dealId;
    }

    public String getValue() {
        return dealId;
    }
}
