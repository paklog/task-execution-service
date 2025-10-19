package com.paklog.wes.task.domain.service;

import com.paklog.wes.task.domain.aggregate.Task;
import com.paklog.wes.task.domain.valueobject.TaskPriority;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain service for calculating dynamic task priority
 * Considers multiple factors: SLA, carrier cutoff, customer tier, zone efficiency
 */
@Service
public class TaskPriorityCalculator {

    private static final Logger logger = LoggerFactory.getLogger(TaskPriorityCalculator.class);

    // Base priority weights (total = 100)
    private static final int WEIGHT_SLA_URGENCY = 35;
    private static final int WEIGHT_CARRIER_CUTOFF = 30;
    private static final int WEIGHT_CUSTOMER_TIER = 20;
    private static final int WEIGHT_ZONE_EFFICIENCY = 10;
    private static final int WEIGHT_AGE = 5;

    // Customer tier multipliers
    private static final Map<String, Double> CUSTOMER_TIER_MULTIPLIERS = Map.of(
            "PLATINUM", 2.0,
            "GOLD", 1.5,
            "SILVER", 1.2,
            "BRONZE", 1.0,
            "STANDARD", 0.8
    );

    /**
     * Calculate comprehensive priority score for a task
     * Higher score = higher priority
     */
    public int calculatePriority(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        int totalScore = 0;

        // 1. SLA Urgency Score (35%)
        int slaScore = calculateSLAScore(task);
        totalScore += (slaScore * WEIGHT_SLA_URGENCY) / 100;

        // 2. Carrier Cutoff Score (30%)
        int cutoffScore = calculateCutoffScore(task);
        totalScore += (cutoffScore * WEIGHT_CARRIER_CUTOFF) / 100;

        // 3. Customer Tier Score (20%)
        int customerScore = calculateCustomerScore(task);
        totalScore += (customerScore * WEIGHT_CUSTOMER_TIER) / 100;

        // 4. Zone Efficiency Score (10%)
        int zoneScore = calculateZoneScore(task);
        totalScore += (zoneScore * WEIGHT_ZONE_EFFICIENCY) / 100;

        // 5. Age Score (5%)
        int ageScore = calculateAgeScore(task);
        totalScore += (ageScore * WEIGHT_AGE) / 100;

        // Apply task type modifiers
        totalScore = applyTaskTypeModifier(totalScore, task.getType());

        // Apply express/rush flags
        if (task.isExpress()) {
            totalScore = (int) (totalScore * 1.5);
        }

        // Ensure score is within bounds
        totalScore = Math.max(0, Math.min(1000, totalScore));

        logger.debug("Calculated priority {} for task {} (SLA:{}, Cutoff:{}, Customer:{}, Zone:{}, Age:{})",
                totalScore, task.getTaskId(), slaScore, cutoffScore, customerScore, zoneScore, ageScore);

        return totalScore;
    }

    /**
     * Calculate priority based on TaskPriority enum and upgrade if needed
     */
    public int calculatePriority(Task task, TaskPriority basePriority) {
        int calculatedScore = calculatePriority(task);
        int baseScore = basePriority.getScore();

        // Return higher of calculated or base priority
        return Math.max(calculatedScore, baseScore);
    }

    /**
     * SLA Urgency Score - based on time remaining until deadline
     */
    private int calculateSLAScore(Task task) {
        LocalDateTime deadline = task.getRequiredCompletionTime();
        if (deadline == null) {
            return 50; // Default medium urgency
        }

        LocalDateTime now = LocalDateTime.now();
        Duration timeRemaining = Duration.between(now, deadline);
        long hoursRemaining = timeRemaining.toHours();

        if (hoursRemaining < 0) {
            return 100; // CRITICAL - past deadline
        } else if (hoursRemaining < 1) {
            return 95; // Urgent - less than 1 hour
        } else if (hoursRemaining < 2) {
            return 90; // Very high - less than 2 hours
        } else if (hoursRemaining < 4) {
            return 80; // High - less than 4 hours
        } else if (hoursRemaining < 8) {
            return 70; // Medium-high - less than 8 hours
        } else if (hoursRemaining < 24) {
            return 60; // Medium - same day
        } else if (hoursRemaining < 48) {
            return 40; // Lower - next day
        } else {
            return 20; // Low - 2+ days
        }
    }

    /**
     * Carrier Cutoff Score - proximity to carrier pickup time
     */
    private int calculateCutoffScore(Task task) {
        LocalDateTime carrierCutoff = task.getCarrierCutoffTime();
        if (carrierCutoff == null) {
            return 30; // Default if no cutoff specified
        }

        LocalDateTime now = LocalDateTime.now();
        Duration timeUntilCutoff = Duration.between(now, carrierCutoff);
        long minutesRemaining = timeUntilCutoff.toMinutes();

        if (minutesRemaining < 0) {
            return 100; // CRITICAL - missed cutoff
        } else if (minutesRemaining < 30) {
            return 95; // Very urgent - less than 30 minutes
        } else if (minutesRemaining < 60) {
            return 90; // Urgent - less than 1 hour
        } else if (minutesRemaining < 120) {
            return 80; // High - less than 2 hours
        } else if (minutesRemaining < 240) {
            return 70; // Medium-high - less than 4 hours
        } else if (minutesRemaining < 480) {
            return 50; // Medium - less than 8 hours
        } else {
            return 20; // Low - plenty of time
        }
    }

    /**
     * Customer Tier Score - based on customer importance
     */
    private int calculateCustomerScore(Task task) {
        String customerTier = task.getCustomerTier();
        if (customerTier == null) {
            return 50; // Default for unknown tier
        }

        return switch (customerTier.toUpperCase()) {
            case "PLATINUM" -> 100; // VIP customers
            case "GOLD" -> 85; // Premium customers
            case "SILVER" -> 70; // Valued customers
            case "BRONZE" -> 55; // Regular customers
            case "STANDARD" -> 40; // Standard customers
            default -> 50; // Unknown tier
        };
    }

    /**
     * Zone Efficiency Score - prefer tasks in active/hot zones
     */
    private int calculateZoneScore(Task task) {
        String zone = task.getZone();
        if (zone == null) {
            return 50; // Default
        }

        // In real implementation, this would query active zone metrics
        // For now, simple heuristic based on zone naming
        if (zone.startsWith("PICK-A") || zone.startsWith("ZONE-A")) {
            return 90; // Hot zone - high traffic
        } else if (zone.startsWith("PICK-B") || zone.startsWith("ZONE-B")) {
            return 70; // Warm zone
        } else if (zone.startsWith("PICK-C") || zone.startsWith("ZONE-C")) {
            return 50; // Normal zone
        } else {
            return 30; // Low traffic zone
        }
    }

    /**
     * Age Score - older tasks get priority boost
     */
    private int calculateAgeScore(Task task) {
        LocalDateTime createdAt = task.getCreatedAt();
        if (createdAt == null) {
            return 0;
        }

        Duration age = Duration.between(createdAt, LocalDateTime.now());
        long hoursOld = age.toHours();

        if (hoursOld > 24) {
            return 100; // Very old task
        } else if (hoursOld > 12) {
            return 80; // Old task
        } else if (hoursOld > 6) {
            return 60; // Moderate age
        } else if (hoursOld > 2) {
            return 40; // Recent
        } else {
            return 20; // Fresh
        }
    }

    /**
     * Apply task type specific modifiers
     */
    private int applyTaskTypeModifier(int baseScore, TaskType taskType) {
        if (taskType == null) {
            return baseScore;
        }

        return switch (taskType) {
            case CYCLE_COUNT -> (int) (baseScore * 0.6); // Lower priority
            case REPLENISHMENT -> (int) (baseScore * 0.8); // Medium-low priority
            case PICKING -> baseScore; // Normal priority
            case PUTAWAY -> (int) (baseScore * 1.1); // Slightly higher (free up receiving)
            case MOVE -> (int) (baseScore * 0.7); // Lower priority
            default -> baseScore;
        };
    }

    /**
     * Calculate dynamic priority that adjusts over time
     */
    public int calculateDynamicPriority(Task task, Map<String, Object> context) {
        int baseScore = calculatePriority(task);

        // Apply contextual boosts
        if (context != null) {
            // Boost if operator is already in same zone
            if (Boolean.TRUE.equals(context.get("operatorInSameZone"))) {
                baseScore = (int) (baseScore * 1.15);
            }

            // Boost if part of batch pick
            if (Boolean.TRUE.equals(context.get("partOfBatch"))) {
                baseScore = (int) (baseScore * 1.1);
            }

            // Reduce if wave is not yet released
            if (Boolean.FALSE.equals(context.get("waveReleased"))) {
                baseScore = (int) (baseScore * 0.5);
            }

            // Boost for shortage/exception resolution
            if (Boolean.TRUE.equals(context.get("resolvingException"))) {
                baseScore = (int) (baseScore * 1.3);
            }
        }

        return Math.min(1000, baseScore);
    }

    /**
     * Recommend priority adjustments based on system load
     */
    public PriorityAdjustment recommendAdjustment(
            Task task,
            SystemLoadMetrics loadMetrics) {

        int currentPriority = task.getPriority();
        int calculatedPriority = calculatePriority(task);

        // Check if priority should be adjusted
        if (Math.abs(currentPriority - calculatedPriority) > 50) {
            String reason = buildAdjustmentReason(task, loadMetrics);

            return new PriorityAdjustment(
                    task.getTaskId(),
                    currentPriority,
                    calculatedPriority,
                    true,
                    reason
            );
        }

        return new PriorityAdjustment(
                task.getTaskId(),
                currentPriority,
                currentPriority,
                false,
                "Priority within acceptable range"
        );
    }

    /**
     * Build reason for priority adjustment
     */
    private String buildAdjustmentReason(Task task, SystemLoadMetrics loadMetrics) {
        StringBuilder reason = new StringBuilder("Priority adjustment recommended: ");

        LocalDateTime deadline = task.getRequiredCompletionTime();
        if (deadline != null) {
            Duration timeRemaining = Duration.between(LocalDateTime.now(), deadline);
            if (timeRemaining.toHours() < 2) {
                reason.append("Approaching SLA deadline. ");
            }
        }

        LocalDateTime cutoff = task.getCarrierCutoffTime();
        if (cutoff != null) {
            Duration timeToCutoff = Duration.between(LocalDateTime.now(), cutoff);
            if (timeToCutoff.toHours() < 1) {
                reason.append("Carrier cutoff imminent. ");
            }
        }

        if (task.isExpress()) {
            reason.append("Express shipment. ");
        }

        if ("PLATINUM".equals(task.getCustomerTier())) {
            reason.append("VIP customer. ");
        }

        return reason.toString();
    }

    /**
     * Priority adjustment recommendation
     */
    public static class PriorityAdjustment {
        private final String taskId;
        private final int currentPriority;
        private final int recommendedPriority;
        private final boolean shouldAdjust;
        private final String reason;

        public PriorityAdjustment(String taskId, int currentPriority,
                                  int recommendedPriority, boolean shouldAdjust, String reason) {
            this.taskId = taskId;
            this.currentPriority = currentPriority;
            this.recommendedPriority = recommendedPriority;
            this.shouldAdjust = shouldAdjust;
            this.reason = reason;
        }

        public String getTaskId() {
            return taskId;
        }

        public int getCurrentPriority() {
            return currentPriority;
        }

        public int getRecommendedPriority() {
            return recommendedPriority;
        }

        public boolean shouldAdjust() {
            return shouldAdjust;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * System load metrics for priority calculations
     */
    public static class SystemLoadMetrics {
        private final int queueDepth;
        private final int activeOperators;
        private final double averageTaskTime;
        private final Map<String, Integer> tasksByZone;

        public SystemLoadMetrics(int queueDepth, int activeOperators,
                                 double averageTaskTime, Map<String, Integer> tasksByZone) {
            this.queueDepth = queueDepth;
            this.activeOperators = activeOperators;
            this.averageTaskTime = averageTaskTime;
            this.tasksByZone = tasksByZone != null ? tasksByZone : new HashMap<>();
        }

        public int getQueueDepth() {
            return queueDepth;
        }

        public int getActiveOperators() {
            return activeOperators;
        }

        public double getAverageTaskTime() {
            return averageTaskTime;
        }

        public Map<String, Integer> getTasksByZone() {
            return tasksByZone;
        }
    }
}
