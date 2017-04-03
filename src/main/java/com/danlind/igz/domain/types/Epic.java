package com.danlind.igz.domain.types;

import java.util.Objects;

/**
 * Created by danlin on 2017-03-21.
 */
public class Epic {

    private final String name;

    public Epic(String name) {
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

        Epic epic = (Epic) o;

        return name.equals(epic.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
