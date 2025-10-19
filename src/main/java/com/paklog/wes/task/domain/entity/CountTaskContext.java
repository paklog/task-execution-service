package com.paklog.wes.task.domain.entity;

import com.paklog.wes.task.domain.valueobject.Location;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * Count task context for cycle counting
 */
@Document
public class CountTaskContext implements TaskContext {

    private String countId;
    private CountType countType;
    private Location location;
    private String sku; // Optional, for SKU-specific counts
    private Integer expectedQuantity;

    public CountTaskContext() {}

    @Override
    public void validate() {
        if (countId == null || countId.isBlank()) {
            throw new IllegalArgumentException("Count ID is required for count tasks");
        }
        if (countType == null) {
            throw new IllegalArgumentException("Count type is required");
        }
        if (location == null) {
            throw new IllegalArgumentException("Location is required for count tasks");
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("countId", countId);
        metadata.put("countType", countType != null ? countType.name() : null);
        metadata.put("location", location != null ? location.getLocationCode() : null);
        metadata.put("sku", sku);
        metadata.put("expectedQuantity", expectedQuantity);
        return metadata;
    }

    public enum CountType {
        CYCLE_COUNT,
        LOCATION_AUDIT,
        SKU_AUDIT,
        BLIND_COUNT
    }

    // Getters and setters
    public String getCountId() {
        return countId;
    }

    public void setCountId(String countId) {
        this.countId = countId;
    }

    public CountType getCountType() {
        return countType;
    }

    public void setCountType(CountType countType) {
        this.countType = countType;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Integer expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }
}
