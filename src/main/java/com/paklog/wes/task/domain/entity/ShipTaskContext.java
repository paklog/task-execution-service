package com.paklog.wes.task.domain.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Ship task context for loading shipments
 */
@Document
public class ShipTaskContext implements TaskContext {

    private String shipmentId;
    private String carrier;
    private String trackingNumber;
    private String dockDoor;
    private LocalDateTime scheduledPickupTime;
    private int totalPackages;

    public ShipTaskContext() {}

    @Override
    public void validate() {
        if (shipmentId == null || shipmentId.isBlank()) {
            throw new IllegalArgumentException("Shipment ID is required for ship tasks");
        }
        if (carrier == null || carrier.isBlank()) {
            throw new IllegalArgumentException("Carrier is required");
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("shipmentId", shipmentId);
        metadata.put("carrier", carrier);
        metadata.put("trackingNumber", trackingNumber);
        metadata.put("dockDoor", dockDoor);
        metadata.put("scheduledPickupTime", scheduledPickupTime);
        metadata.put("totalPackages", totalPackages);
        return metadata;
    }

    // Getters and setters
    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getDockDoor() {
        return dockDoor;
    }

    public void setDockDoor(String dockDoor) {
        this.dockDoor = dockDoor;
    }

    public LocalDateTime getScheduledPickupTime() {
        return scheduledPickupTime;
    }

    public void setScheduledPickupTime(LocalDateTime scheduledPickupTime) {
        this.scheduledPickupTime = scheduledPickupTime;
    }

    public int getTotalPackages() {
        return totalPackages;
    }

    public void setTotalPackages(int totalPackages) {
        this.totalPackages = totalPackages;
    }
}
