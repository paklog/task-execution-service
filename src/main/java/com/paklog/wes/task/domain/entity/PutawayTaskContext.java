package com.paklog.wes.task.domain.entity;

import com.paklog.wes.task.domain.valueobject.Location;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Putaway task context for storing received inventory
 */
@Document
public class PutawayTaskContext implements TaskContext {

    private String receiptId;
    private String lpn;  // License Plate Number
    private String sku;
    private int quantity;
    private Location destinationLocation;
    private String storageType;

    public PutawayTaskContext() {}

    public PutawayTaskContext(String receiptId, String lpn, String sku, int quantity, Location destinationLocation) {
        this.receiptId = Objects.requireNonNull(receiptId, "Receipt ID cannot be null");
        this.lpn = Objects.requireNonNull(lpn, "LPN cannot be null");
        this.sku = Objects.requireNonNull(sku, "SKU cannot be null");
        this.quantity = quantity;
        this.destinationLocation = Objects.requireNonNull(destinationLocation, "Destination location cannot be null");
    }

    @Override
    public void validate() {
        if (receiptId == null || receiptId.isBlank()) {
            throw new IllegalArgumentException("Receipt ID is required for putaway tasks");
        }
        if (lpn == null || lpn.isBlank()) {
            throw new IllegalArgumentException("LPN is required");
        }
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (destinationLocation == null) {
            throw new IllegalArgumentException("Destination location is required");
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("receiptId", receiptId);
        metadata.put("lpn", lpn);
        metadata.put("sku", sku);
        metadata.put("quantity", quantity);
        metadata.put("destinationLocation", destinationLocation != null ? destinationLocation.getLocationCode() : null);
        metadata.put("storageType", storageType);
        return metadata;
    }

    // Getters and setters
    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

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

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }
}
