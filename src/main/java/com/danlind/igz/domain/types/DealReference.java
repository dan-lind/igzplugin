package com.danlind.igz.domain.types;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by danlin on 2017-03-21.
 */
public class DealReference implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String name;

    public DealReference(String name) {
        Objects.requireNonNull(name);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DealReference epic = (DealReference) o;

        return name.equals(epic.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
