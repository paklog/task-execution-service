package com.paklog.wes.task.domain.valueobject;

/**
 * Enumeration of all warehouse task types
 */
public enum TaskType {
    /**
     * Picking items from inventory locations for orders
     */
    PICK,

    /**
     * Packing items into shipping containers
     */
    PACK,

    /**
     * Storing received inventory into locations
     */
    PUTAWAY,

    /**
     * Replenishing forward pick locations from reserve
     */
    REPLENISH,

    /**
     * Cycle counting inventory
     */
    COUNT,

    /**
     * Moving inventory between locations
     */
    MOVE,

    /**
     * Loading shipments for dispatch
     */
    SHIP;

    /**
     * Check if this task type requires location assignment
     */
    public boolean requiresLocation() {
        return this == PICK || this == PUTAWAY || this == REPLENISH || this == COUNT;
    }

    /**
     * Check if this task type is order-related
     */
    public boolean isOrderRelated() {
        return this == PICK || this == PACK || this == SHIP;
    }

    /**
     * Get estimated complexity multiplier for this task type
     */
    public double getComplexityMultiplier() {
        return switch (this) {
            case PICK -> 1.0;
            case PACK -> 1.2;
            case PUTAWAY -> 0.8;
            case REPLENISH -> 1.5;
            case COUNT -> 1.0;
            case MOVE -> 0.5;
            case SHIP -> 1.3;
        };
    }
}
