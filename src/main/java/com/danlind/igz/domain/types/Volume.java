package com.danlind.igz.domain.types;

import java.util.Objects;

/**
 * Created by danlin on 2017-04-07.
 */
public class Volume {

    private final Integer volume;

    public Volume(Integer volume) {
        Objects.requireNonNull(volume);
        this.volume = volume;
    }

    public Integer getValue() {
        return volume;
    }
}
