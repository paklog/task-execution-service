package com.paklog.wes.task.adapter.rest.mapper;

import com.paklog.wes.task.domain.entity.CountTaskContext;
import com.paklog.wes.task.domain.entity.MoveTaskContext;
import com.paklog.wes.task.domain.entity.PackTaskContext;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.entity.PutawayTaskContext;
import com.paklog.wes.task.domain.entity.ReplenishTaskContext;
import com.paklog.wes.task.domain.entity.ShipTaskContext;
import com.paklog.wes.task.domain.entity.TaskContext;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TaskContextMapperTest {

    private TaskContextMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TaskContextMapper(Jackson2ObjectMapperBuilder.json().build());
    }

    @ParameterizedTest(name = "{0} context is mapped to {1}")
    @MethodSource("contextMappings")
    void mapContextHandlesAllTaskTypes(TaskType type,
                                       Class<? extends TaskContext> expectedType,
                                       Map<String, Object> contextMap,
                                       Map<String, Object> expectedMetadata) {
        TaskContext context = mapper.mapContext(type, contextMap);

        assertThat(context)
                .as("mapper should create expected context type")
                .isInstanceOf(expectedType);

        context.validate();
        assertThat(context.getMetadata())
                .containsAllEntriesOf(expectedMetadata);
    }

    private static Stream<Arguments> contextMappings() {
        return Stream.of(
                Arguments.of(
                        TaskType.PICK,
                        PickTaskContext.class,
                        Map.of(
                                "type", "PICK",
                                "waveId", "WAVE-1",
                                "orderId", "ORDER-1",
                                "strategy", "DISCRETE",
                                "instructions", List.of(
                                        Map.of(
                                                "sku", "SKU-1",
                                                "quantity", 2,
                                                "lpn", "LPN-1",
                                                "location", locationMap("A")
                                        )
                                )
                        ),
                        Map.of(
                                "waveId", "WAVE-1",
                                "orderId", "ORDER-1",
                                "instructionCount", 1
                        )
                ),
                Arguments.of(
                        TaskType.PACK,
                        PackTaskContext.class,
                        Map.of(
                                "type", "PACK",
                                "orderId", "ORDER-2",
                                "shipmentId", "SHIP-2",
                                "strategy", "SINGLE_ORDER",
                                "items", List.of(
                                        Map.of("sku", "SKU-2", "quantity", 1)
                                )
                        ),
                        Map.of(
                                "orderId", "ORDER-2",
                                "itemCount", 1,
                                "strategy", "SINGLE_ORDER"
                        )
                ),
                Arguments.of(
                        TaskType.PUTAWAY,
                        PutawayTaskContext.class,
                        Map.of(
                                "type", "PUTAWAY",
                                "receiptId", "REC-1",
                                "lpn", "LPN-2",
                                "sku", "SKU-3",
                                "quantity", 5,
                                "destinationLocation", locationMap("B"),
                                "storageType", "RESERVE"
                        ),
                        Map.of(
                                "receiptId", "REC-1",
                                "destinationLocation", "B-01-01-01"
                        )
                ),
                Arguments.of(
                        TaskType.REPLENISH,
                        ReplenishTaskContext.class,
                        Map.of(
                                "type", "REPLENISH",
                                "sku", "SKU-4",
                                "quantity", 10,
                                "sourceLocation", locationMap("C"),
                                "destinationLocation", locationMap("D"),
                                "replenishmentType", "MIN_MAX"
                        ),
                        Map.of(
                                "sku", "SKU-4",
                                "sourceLocation", "C-01-01-01",
                                "destinationLocation", "D-01-01-01",
                                "replenishmentType", "MIN_MAX"
                        )
                ),
                Arguments.of(
                        TaskType.COUNT,
                        CountTaskContext.class,
                        Map.of(
                                "type", "COUNT",
                                "countId", "COUNT-1",
                                "countType", "CYCLE_COUNT",
                                "location", locationMap("E"),
                                "sku", "SKU-5",
                                "expectedQuantity", 12
                        ),
                        Map.of(
                                "countId", "COUNT-1",
                                "countType", "CYCLE_COUNT",
                                "location", "E-01-01-01"
                        )
                ),
                Arguments.of(
                        TaskType.MOVE,
                        MoveTaskContext.class,
                        Map.of(
                                "type", "MOVE",
                                "lpn", "LPN-6",
                                "sourceLocation", locationMap("F"),
                                "destinationLocation", locationMap("G"),
                                "reason", "REBALANCE"
                        ),
                        Map.of(
                                "lpn", "LPN-6",
                                "sourceLocation", "F-01-01-01",
                                "destinationLocation", "G-01-01-01"
                        )
                ),
                Arguments.of(
                        TaskType.SHIP,
                        ShipTaskContext.class,
                        Map.of(
                                "type", "SHIP",
                                "shipmentId", "SHIP-3",
                                "carrier", "CARRIER-X",
                                "trackingNumber", "TRACK-123"
                        ),
                        Map.of(
                                "carrier", "CARRIER-X",
                                "shipmentId", "SHIP-3"
                        )
                )
        );
    }

    private static Map<String, Object> locationMap(String aisle) {
        return Map.of(
                "aisle", aisle,
                "bay", "01",
                "level", "01",
                "position", "01"
        );
    }
}
