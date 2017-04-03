package com.danlind.igz.domain;

import com.danlind.igz.domain.types.Epic;

import java.util.Objects;

/**
 * Created by danlin on 2017-03-12.
 */
public class PriceDetails {

    private final Epic epic;
    private final double bid;
    private final double ask;
    private final double volume;

    public PriceDetails(Epic epic, double bid, double ask, double volume) {
        Objects.requireNonNull(epic);
        this.epic = epic;
        this.bid = bid;
        this.ask = ask;
        this.volume = volume;
    }

    public double getAsk() {
        return ask;
    }

    public double getSpread() {
        return this.ask-this.bid;
    }

    public double getBid() {
        return bid;
    }

    public Epic getEpic() {
        return epic;
    }

    public double getVolume() {
        return volume;
    }
}
