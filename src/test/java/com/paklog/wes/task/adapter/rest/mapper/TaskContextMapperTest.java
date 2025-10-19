package com.paklog.wes.task.adapter.rest.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wes.task.domain.entity.PackTaskContext;
import com.paklog.wes.task.domain.entity.ShipTaskContext;
import com.paklog.wes.task.domain.entity.TaskContext;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TaskContextMapperTest {

    private TaskContextMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TaskContextMapper(new ObjectMapper());
    }

    @Test
    void mapContextCreatesPackTaskContext() {
        Map<String, Object> contextMap = Map.of(
                "type", "PACK",
                "orderId", "ORDER-1",
                "shipmentId", "SHIP-1",
                "strategy", "SINGLE_ORDER",
                "items", List.of(Map.of("sku", "SKU-1", "quantity", 2))
        );

        TaskContext context = mapper.mapContext(TaskType.PACK, contextMap);

        assertThat(context).isInstanceOf(PackTaskContext.class);
        PackTaskContext packContext = (PackTaskContext) context;
        packContext.validate();
        assertThat(packContext.getMetadata())
                .containsEntry("orderId", "ORDER-1")
                .containsEntry("itemCount", 1);
    }

    @Test
    void mapContextCreatesShipTaskContext() {
        Map<String, Object> contextMap = Map.of(
                "type", "SHIP",
                "shipmentId", "SHIP-1",
                "carrier", "CARRIER",
                "trackingNumber", "TRACK"
        );

        TaskContext context = mapper.mapContext(TaskType.SHIP, contextMap);

        assertThat(context).isInstanceOf(ShipTaskContext.class);
        ShipTaskContext shipContext = (ShipTaskContext) context;
        shipContext.validate();
        assertThat(shipContext.getMetadata()).containsEntry("carrier", "CARRIER");
    }
}
