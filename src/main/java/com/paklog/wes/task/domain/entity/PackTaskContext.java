package com.paklog.wes.task.domain.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pack task context for packing operations
 */
@Document
public class PackTaskContext implements TaskContext {

    private String orderId;
    private String shipmentId;
    private PackStrategy strategy;
    private List<PackItem> items;
    private String packStationId;
    private boolean requiresGiftWrap;
    private boolean requiresFragileHandling;
    private int totalItems;

    public PackTaskContext() {
        this.items = new ArrayList<>();
    }

    public PackTaskContext(String orderId, String shipmentId, PackStrategy strategy, List<PackItem> items) {
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.shipmentId = shipmentId;
        this.strategy = Objects.requireNonNull(strategy, "Pack strategy cannot be null");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "Pack items cannot be null"));
        this.totalItems = items.size();
    }

    @Override
    public void validate() {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID is required for pack tasks");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Pack items cannot be empty");
        }
        items.forEach(PackItem::validate);
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderId", orderId);
        metadata.put("shipmentId", shipmentId);
        metadata.put("strategy", strategy != null ? strategy.name() : null);
        metadata.put("itemCount", items != null ? items.size() : 0);
        metadata.put("packStationId", packStationId);
        metadata.put("requiresGiftWrap", requiresGiftWrap);
        metadata.put("requiresFragileHandling", requiresFragileHandling);
        return metadata;
    }

    @Override
    public double getComplexityScore() {
        double score = 1.0;

        // More items = higher complexity
        score += (items.size() * 0.15);

        // Special handling increases complexity
        if (requiresGiftWrap) {
            score *= 1.2;
        }
        if (requiresFragileHandling) {
            score *= 1.3;
        }

        return score;
    }

    /**
     * Item to be packed
     */
    public static class PackItem {
        private String sku;
        private int quantity;
        private boolean isFragile;
        private boolean requiresBubbleWrap;
        private String containerType;

        public PackItem() {}

        public PackItem(String sku, int quantity) {
            this.sku = sku;
            this.quantity = quantity;
        }

        public void validate() {
            if (sku == null || sku.isBlank()) {
                throw new IllegalArgumentException("SKU is required");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
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

        public boolean isFragile() {
            return isFragile;
        }

        public void setFragile(boolean fragile) {
            isFragile = fragile;
        }

        public boolean isRequiresBubbleWrap() {
            return requiresBubbleWrap;
        }

        public void setRequiresBubbleWrap(boolean requiresBubbleWrap) {
            this.requiresBubbleWrap = requiresBubbleWrap;
        }

        public String getContainerType() {
            return containerType;
        }

        public void setContainerType(String containerType) {
            this.containerType = containerType;
        }
    }

    /**
     * Pack strategy enumeration
     */
    public enum PackStrategy {
        SINGLE_ORDER,      // Pack one order at a time
        MULTI_ORDER,       // Pack multiple orders together
        CONSOLIDATION,     // Consolidate shipments
        EXPRESS            // Express packing for priority orders
    }

    // Getters and setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public PackStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(PackStrategy strategy) {
        this.strategy = strategy;
    }

    public List<PackItem> getItems() {
        return new ArrayList<>(items);
    }

    public void setItems(List<PackItem> items) {
        this.items = new ArrayList<>(items);
        this.totalItems = items.size();
    }

    public String getPackStationId() {
        return packStationId;
    }

    public void setPackStationId(String packStationId) {
        this.packStationId = packStationId;
    }

    public boolean isRequiresGiftWrap() {
        return requiresGiftWrap;
    }

    public void setRequiresGiftWrap(boolean requiresGiftWrap) {
        this.requiresGiftWrap = requiresGiftWrap;
    }

    public boolean isRequiresFragileHandling() {
        return requiresFragileHandling;
    }

    public void setRequiresFragileHandling(boolean requiresFragileHandling) {
        this.requiresFragileHandling = requiresFragileHandling;
    }

    public int getTotalItems() {
        return totalItems;
    }
}
