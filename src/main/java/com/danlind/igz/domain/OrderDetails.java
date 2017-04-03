package com.danlind.igz.domain;

import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.Direction;
import com.danlind.igz.domain.types.Epic;

/**
 * Created by danlin on 2017-03-27.
 */
public class OrderDetails {

    private final Epic epic;
    private final double entryLevel;
    private final Direction direction;
    private final int positionSize;
    private final String dealId;


    public OrderDetails(Epic epic, double entryLevel, Direction direction, int positionSize, String dealId) {
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
}
