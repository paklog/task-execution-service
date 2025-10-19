package com.paklog.wes.task.domain.entity;

import com.paklog.wes.task.domain.valueobject.Location;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskContextVariantsTest {

    private final Location source = new Location("A", "01", "01", "01");
    private final Location destination = new Location("B", "02", "01", "02");

    @Test
    void putawayContextValidatesAndExposesMetadata() {
        PutawayTaskContext context = new PutawayTaskContext();
        context.setReceiptId("REC-1");
        context.setLpn("LPN-1");
        context.setSku("SKU-1");
        context.setQuantity(5);
        context.setDestinationLocation(destination);
        context.setStorageType("RESERVE");

        context.validate();
        assertThat(context.getMetadata()).containsEntry("receiptId", "REC-1");
    }

    @Test
    void putawayContextRequiresPositiveQuantity() {
        PutawayTaskContext context = new PutawayTaskContext();
        context.setReceiptId("REC-1");
        context.setLpn("LPN-1");
        context.setSku("SKU-1");
        context.setQuantity(0);
        context.setDestinationLocation(destination);

        assertThatThrownBy(context::validate).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replenishContextValidatesMetadata() {
        ReplenishTaskContext context = new ReplenishTaskContext();
        context.setSku("SKU-1");
        context.setQuantity(10);
        context.setSourceLocation(source);
        context.setDestinationLocation(destination);
        context.setReplenishmentType("DEMAND_BASED");

        context.validate();
        assertThat(context.getMetadata().get("replenishmentType")).isEqualTo("DEMAND_BASED");
    }

    @Test
    void replenishContextRequiresLocations() {
        ReplenishTaskContext context = new ReplenishTaskContext();
        context.setSku("SKU-1");
        context.setQuantity(1);

        assertThatThrownBy(context::validate).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void countContextSupportsDifferentTypes() {
        CountTaskContext context = new CountTaskContext();
        context.setCountId("COUNT-1");
        context.setCountType(CountTaskContext.CountType.CYCLE_COUNT);
        context.setLocation(source);
        context.setSku("SKU-1");
        context.setExpectedQuantity(20);

        context.validate();
        Map<String, Object> metadata = context.getMetadata();
        assertThat(metadata.get("countType")).isEqualTo("CYCLE_COUNT");
        assertThat(metadata.get("expectedQuantity")).isEqualTo(20);
    }

    @Test
    void countContextRequiresType() {
        CountTaskContext context = new CountTaskContext();
        context.setCountId("COUNT-1");
        context.setLocation(source);

        assertThatThrownBy(context::validate).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shipContextCapturesCarrierInformation() {
        ShipTaskContext context = new ShipTaskContext();
        context.setShipmentId("SHIP-1");
        context.setCarrier("CARRIER");
        context.setTrackingNumber("TRACK-1");
        context.setDockDoor("DOOR-1");
        context.setScheduledPickupTime(LocalDateTime.now().plusHours(1));
        context.setTotalPackages(3);

        context.validate();
        assertThat(context.getMetadata()).containsEntry("carrier", "CARRIER");
    }

    @Test
    void shipContextRequiresCarrier() {
        ShipTaskContext context = new ShipTaskContext();
        context.setShipmentId("SHIP-1");

        assertThatThrownBy(context::validate).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moveContextRequiresSourceAndDestination() {
        MoveTaskContext context = new MoveTaskContext();
        context.setLpn("LPN-1");
        context.setSourceLocation(source);
        context.setDestinationLocation(destination);
        context.setReason("BALANCE");

        context.validate();
        assertThat(context.getMetadata().get("reason")).isEqualTo("BALANCE");
    }

    @Test
    void moveContextRequiresDestination() {
        MoveTaskContext context = new MoveTaskContext();
        context.setLpn("LPN-1");
        context.setSourceLocation(source);

        assertThatThrownBy(context::validate).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void packContextCalculatesComplexity() {
        PackTaskContext.PackItem item = new PackTaskContext.PackItem("SKU-1", 2);
        item.setFragile(true);
        PackTaskContext context = new PackTaskContext("ORDER-1", "SHIP-1",
                PackTaskContext.PackStrategy.SINGLE_ORDER, List.of(item));
        context.setRequiresFragileHandling(true);
        context.setRequiresGiftWrap(true);

        context.validate();
        assertThat(context.getComplexityScore()).isGreaterThan(1.0);
        assertThat(context.getMetadata().get("orderId")).isEqualTo("ORDER-1");
    }

    @Test
    void packContextRequiresItems() {
        PackTaskContext context = new PackTaskContext();
        context.setOrderId("ORDER-1");

        assertThatThrownBy(context::validate).isInstanceOf(IllegalArgumentException.class);
    }
}
