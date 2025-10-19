package com.paklog.wes.task.domain.valueobject;

import java.util.Objects;

/**
 * Warehouse location value object
 * Represents a physical location in the warehouse
 */
public class Location {

    private final String aisle;
    private final String bay;
    private final String level;
    private final String position;

    public Location(String aisle, String bay, String level, String position) {
        Objects.requireNonNull(aisle, "Aisle cannot be null");
        Objects.requireNonNull(bay, "Bay cannot be null");
        Objects.requireNonNull(level, "Level cannot be null");

        this.aisle = aisle;
        this.bay = bay;
        this.level = level;
        this.position = position;
    }

    /**
     * Create location from location code (e.g., "A-01-02-03")
     */
    public static Location fromCode(String locationCode) {
        Objects.requireNonNull(locationCode, "Location code cannot be null");

        String[] parts = locationCode.split("-");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid location code format: " + locationCode);
        }

        String aisle = parts[0];
        String bay = parts[1];
        String level = parts[2];
        String position = parts.length > 3 ? parts[3] : null;

        return new Location(aisle, bay, level, position);
    }

    /**
     * Get full location code (e.g., "A-01-02-03")
     */
    public String getLocationCode() {
        StringBuilder code = new StringBuilder()
                .append(aisle).append("-")
                .append(bay).append("-")
                .append(level);

        if (position != null) {
            code.append("-").append(position);
        }

        return code.toString();
    }

    /**
     * Calculate distance from another location (simplified Manhattan distance)
     */
    public double distanceFrom(Location other) {
        if (other == null) {
            return Double.MAX_VALUE;
        }

        // Simplified distance calculation based on aisle and bay differences
        int aisleDiff = Math.abs(compareAlphanumeric(this.aisle, other.aisle));
        int bayDiff = Math.abs(Integer.parseInt(this.bay) - Integer.parseInt(other.bay));
        int levelDiff = Math.abs(Integer.parseInt(this.level) - Integer.parseInt(other.level));

        // Aisle changes are most expensive, then bay, then level
        return (aisleDiff * 100.0) + (bayDiff * 10.0) + (levelDiff * 1.0);
    }

    private int compareAlphanumeric(String s1, String s2) {
        // Extract numeric part if exists, otherwise use character comparison
        try {
            String num1 = s1.replaceAll("[^0-9]", "");
            String num2 = s2.replaceAll("[^0-9]", "");
            if (!num1.isEmpty() && !num2.isEmpty()) {
                return Integer.parseInt(num1) - Integer.parseInt(num2);
            }
        } catch (NumberFormatException e) {
            // Fall through to string comparison
        }
        return s1.compareTo(s2);
    }

    /**
     * Check if this location is in the same zone as another
     */
    public boolean inSameZone(Location other) {
        return this.aisle.equals(other.aisle);
    }

    public String getAisle() {
        return aisle;
    }

    public String getBay() {
        return bay;
    }

    public String getLevel() {
        return level;
    }

    public String getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Objects.equals(aisle, location.aisle) &&
                Objects.equals(bay, location.bay) &&
                Objects.equals(level, location.level) &&
                Objects.equals(position, location.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aisle, bay, level, position);
    }

    @Override
    public String toString() {
        return getLocationCode();
    }
}
