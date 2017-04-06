package com.danlind.igz.domain;

import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.Direction;
import com.danlind.igz.domain.types.Epic;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by danlin on 2017-03-27.
 */
public class OrderDetails implements Serializable{
    private final Epic epic;
    private final double entryLevel;
    private final Direction direction;
    private final int positionSize;
    private final String dealId;
    private static final long serialVersionUID = 1L;

    public OrderDetails(Epic epic, double entryLevel, Direction direction, int positionSize, String dealId) {
        Objects.requireNonNull(epic);
        Objects.requireNonNull(direction);
        Objects.requireNonNull(dealId);
        this.epic = epic;
        this.entryLevel = entryLevel;
        this.direction = direction;
        this.positionSize = positionSize;
        this.dealId = dealId;
    }

    public Epic getEpic() {
        return epic;
    }

    public double getEntryLevel() {
        return entryLevel;
    }

    public String getDealId() {
        return dealId;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getPositionSize() {
        return positionSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrderDetails that = (OrderDetails) o;

        if (Double.compare(that.entryLevel, entryLevel) != 0) return false;
        if (positionSize != that.positionSize) return false;
        if (!epic.equals(that.epic)) return false;
        if (direction != that.direction) return false;
        return dealId.equals(that.dealId);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = epic.hashCode();
        temp = Double.doubleToLongBits(entryLevel);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + direction.hashCode();
        result = 31 * result + positionSize;
        result = 31 * result + dealId.hashCode();
        return result;
    }
}
