package com.paklog.wes.task.infrastructure.assignment;

import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskType;

import java.util.Set;

/**
 * Worker information for task assignment
 */
public record Worker(
        String workerId,
        String warehouseId,
        String currentZone,
        Location currentLocation,
        Set<TaskType> capabilities,
        Set<TaskType> specializations,
        int activeTaskCount,
        double performanceRating // 0.0 to 1.0
) {
    /**
     * Check if worker can perform a task type
     */
    public boolean canPerform(TaskType taskType) {
        return capabilities.contains(taskType);
    }

    /**
     * Check if worker has specialization in a task type
     */
    public boolean hasSpecialization(TaskType taskType) {
        return specializations.contains(taskType);
    }

    /**
     * Check if worker is available (not overloaded)
     */
    public boolean isAvailable() {
        return activeTaskCount < 3; // Configurable max concurrent tasks
    }

    /**
     * Builder for Worker
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String workerId;
        private String warehouseId;
        private String currentZone;
        private Location currentLocation;
        private Set<TaskType> capabilities = Set.of();
        private Set<TaskType> specializations = Set.of();
        private int activeTaskCount = 0;
        private double performanceRating = 1.0;

        public Builder workerId(String workerId) {
            this.workerId = workerId;
            return this;
        }

        public Builder warehouseId(String warehouseId) {
            this.warehouseId = warehouseId;
            return this;
        }

        public Builder currentZone(String currentZone) {
            this.currentZone = currentZone;
            return this;
        }

        public Builder currentLocation(Location currentLocation) {
            this.currentLocation = currentLocation;
            return this;
        }

        public Builder capabilities(Set<TaskType> capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder specializations(Set<TaskType> specializations) {
            this.specializations = specializations;
            return this;
        }

        public Builder activeTaskCount(int activeTaskCount) {
            this.activeTaskCount = activeTaskCount;
            return this;
        }

        public Builder performanceRating(double performanceRating) {
            this.performanceRating = performanceRating;
            return this;
        }

        public Worker build() {
            return new Worker(
                    workerId,
                    warehouseId,
                    currentZone,
                    currentLocation,
                    capabilities,
                    specializations,
                    activeTaskCount,
                    performanceRating
            );
        }
    }
}
