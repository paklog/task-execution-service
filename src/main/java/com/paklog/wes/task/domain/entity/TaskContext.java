package com.paklog.wes.task.domain.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;

/**
 * Polymorphic task context for type-specific data
 * Each task type can have its own context with specific fields
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PickTaskContext.class, name = "PICK"),
        @JsonSubTypes.Type(value = PackTaskContext.class, name = "PACK"),
        @JsonSubTypes.Type(value = PutawayTaskContext.class, name = "PUTAWAY"),
        @JsonSubTypes.Type(value = ReplenishTaskContext.class, name = "REPLENISH"),
        @JsonSubTypes.Type(value = CountTaskContext.class, name = "COUNT"),
        @JsonSubTypes.Type(value = MoveTaskContext.class, name = "MOVE"),
        @JsonSubTypes.Type(value = ShipTaskContext.class, name = "SHIP")
})
public interface TaskContext {

    /**
     * Validate context data
     * Throws IllegalArgumentException if validation fails
     */
    void validate();

    /**
     * Get context metadata as key-value pairs
     */
    Map<String, Object> getMetadata();

    /**
     * Get estimated complexity score for this task context
     */
    default double getComplexityScore() {
        return 1.0;
    }
}
