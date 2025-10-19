package com.paklog.wes.task.domain.entity;

import com.paklog.wes.task.domain.valueobject.Location;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * Replenish task context for moving inventory from reserve to forward pick
 */
@Document
public class ReplenishTaskContext implements TaskContext {

    private String sku;
    private int quantity;
    private Location sourceLocation;
    private Location destinationLocation;
    private String replenishmentType; // MIN_MAX, EMPTY_LOCATION, DEMAND_BASED

    public ReplenishTaskContext() {}

    @Override
    public void validate() {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required for replenish tasks");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (sourceLocation == null || destinationLocation == null) {
            throw new IllegalArgumentException("Source and destination locations are required");
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sku", sku);
        metadata.put("quantity", quantity);
        metadata.put("sourceLocation", sourceLocation != null ? sourceLocation.getLocationCode() : null);
        metadata.put("destinationLocation", destinationLocation != null ? destinationLocation.getLocationCode() : null);
        metadata.put("replenishmentType", replenishmentType);
        return metadata;
    }

    // Getters and setters
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Location getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(Location sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public String getReplenishmentType() {
        return replenishmentType;
    }

    public void setReplenishmentType(String replenishmentType) {
        this.replenishmentType = replenishmentType;
    }
}
