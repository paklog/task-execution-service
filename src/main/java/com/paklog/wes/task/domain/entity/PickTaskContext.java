package com.paklog.wes.task.domain.entity;

import com.paklog.wes.task.domain.valueobject.Location;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pick task context with pick instructions
 */
@Document
public class PickTaskContext implements TaskContext {

    private String waveId;
    private String orderId;
    private PickStrategy strategy;
    private List<PickInstruction> instructions;
    private boolean isMultiOrder;
    private int totalQuantity;

    public PickTaskContext() {
        this.instructions = new ArrayList<>();
    }

    public PickTaskContext(String waveId, String orderId, PickStrategy strategy, List<PickInstruction> instructions) {
        this.waveId = Objects.requireNonNull(waveId, "Wave ID cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.strategy = Objects.requireNonNull(strategy, "Pick strategy cannot be null");
        this.instructions = new ArrayList<>(Objects.requireNonNull(instructions, "Instructions cannot be null"));
        this.isMultiOrder = false;
        this.totalQuantity = instructions.stream().mapToInt(PickInstruction::getQuantity).sum();
    }

    @Override
    public void validate() {
        if (waveId == null || waveId.isBlank()) {
            throw new IllegalArgumentException("Wave ID is required for pick tasks");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID is required for pick tasks");
        }
        if (instructions == null || instructions.isEmpty()) {
            throw new IllegalArgumentException("Pick instructions cannot be empty");
        }
        instructions.forEach(PickInstruction::validate);
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("waveId", waveId);
        metadata.put("orderId", orderId);
        metadata.put("strategy", strategy != null ? strategy.name() : null);
        metadata.put("instructionCount", instructions != null ? instructions.size() : 0);
        metadata.put("totalQuantity", totalQuantity);
        metadata.put("isMultiOrder", isMultiOrder);
        return metadata;
    }

    @Override
    public double getComplexityScore() {
        // Base complexity
        double score = 1.0;

        // More instructions = higher complexity
        score += (instructions.size() * 0.1);

        // Total quantity affects complexity
        score += (totalQuantity * 0.05);

        // Multi-order picking is more complex
        if (isMultiOrder) {
            score *= 1.3;
        }

        return score;
    }

    /**
     * Single pick instruction
     */
    public static class PickInstruction {
        private String sku;
        private int quantity;
        private Location location;
        private String lpn;  // License Plate Number

        public PickInstruction() {}

        public PickInstruction(String sku, int quantity, Location location, String lpn) {
            this.sku = sku;
            this.quantity = quantity;
            this.location = location;
            this.lpn = lpn;
        }

        public void validate() {
            if (sku == null || sku.isBlank()) {
                throw new IllegalArgumentException("SKU is required");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (location == null) {
                throw new IllegalArgumentException("Pick location is required");
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

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public String getLpn() {
            return lpn;
        }

        public void setLpn(String lpn) {
            this.lpn = lpn;
        }
    }

    /**
     * Pick strategy enumeration
     */
    public enum PickStrategy {
        DISCRETE,      // One order at a time
        BATCH,         // Multiple orders together
        ZONE,          // Pick by zone
        WAVE,          // Pick entire wave
        CLUSTER        // Pick and sort into clusters
    }

    // Getters and setters
    public String getWaveId() {
        return waveId;
    }

    public void setWaveId(String waveId) {
        this.waveId = waveId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public PickStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(PickStrategy strategy) {
        this.strategy = strategy;
    }

    public List<PickInstruction> getInstructions() {
        return new ArrayList<>(instructions);
    }

    public void setInstructions(List<PickInstruction> instructions) {
        this.instructions = new ArrayList<>(instructions);
        this.totalQuantity = instructions.stream().mapToInt(PickInstruction::getQuantity).sum();
    }

    public boolean isMultiOrder() {
        return isMultiOrder;
    }

    public void setMultiOrder(boolean multiOrder) {
        isMultiOrder = multiOrder;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }
}
