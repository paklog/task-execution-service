package com.paklog.wes.task.domain.entity;

import com.paklog.wes.task.domain.valueobject.Location;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * Move task context for moving inventory between locations
 */
@Document
public class MoveTaskContext implements TaskContext {

    private String lpn;
    private Location sourceLocation;
    private Location destinationLocation;
    private String reason;

    public MoveTaskContext() {}

    @Override
    public void validate() {
        if (lpn == null || lpn.isBlank()) {
            throw new IllegalArgumentException("LPN is required for move tasks");
        }
        if (sourceLocation == null || destinationLocation == null) {
            throw new IllegalArgumentException("Source and destination locations are required");
        }
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("lpn", lpn);
        metadata.put("sourceLocation", sourceLocation != null ? sourceLocation.getLocationCode() : null);
        metadata.put("destinationLocation", destinationLocation != null ? destinationLocation.getLocationCode() : null);
        metadata.put("reason", reason);
        return metadata;
    }

    // Getters and setters
    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
