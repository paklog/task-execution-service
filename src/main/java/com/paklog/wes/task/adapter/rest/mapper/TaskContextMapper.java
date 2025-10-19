package com.paklog.wes.task.adapter.rest.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wes.task.domain.entity.*;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mapper to convert context maps to TaskContext objects
 */
@Component
public class TaskContextMapper {

    private final ObjectMapper objectMapper;

    public TaskContextMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TaskContext mapContext(TaskType type, Map<String, Object> contextMap) {
        return switch (type) {
            case PICK -> objectMapper.convertValue(contextMap, PickTaskContext.class);
            case PACK -> objectMapper.convertValue(contextMap, PackTaskContext.class);
            case PUTAWAY -> objectMapper.convertValue(contextMap, PutawayTaskContext.class);
            case REPLENISH -> objectMapper.convertValue(contextMap, ReplenishTaskContext.class);
            case COUNT -> objectMapper.convertValue(contextMap, CountTaskContext.class);
            case MOVE -> objectMapper.convertValue(contextMap, MoveTaskContext.class);
            case SHIP -> objectMapper.convertValue(contextMap, ShipTaskContext.class);
        };
    }
}
