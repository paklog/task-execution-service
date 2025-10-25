package com.paklog.wes.task.domain.service;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.TaskContext;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Domain service for calculating dynamic task priority.
 * Uses a weighted scoring model that blends SLA urgency, carrier cut-off,
 * customer importance, zone efficiency and task age.
 */
@Service
public class TaskPriorityCalculator {

    private static final Logger logger = LoggerFactory.getLogger(TaskPriorityCalculator.class);

    private static final int WEIGHT_SLA_URGENCY = 35;
    private static final int WEIGHT_CARRIER_CUTOFF = 30;
    private static final int WEIGHT_CUSTOMER_TIER = 20;
    private static final int WEIGHT_ZONE_EFFICIENCY = 10;
    private static final int WEIGHT_AGE = 5;

    private static final Map<String, Double> CUSTOMER_TIER_MULTIPLIERS = Map.of(
            "PLATINUM", 2.0,
            "GOLD", 1.5,
            "SILVER", 1.2,
            "BRONZE", 1.0,
            "STANDARD", 0.8
    );

    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Calculate comprehensive priority score for a task. Higher score = higher priority.
     */
    public int calculatePriority(WorkTask task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        int totalScore = 0;

        int slaScore = calculateSLAScore(task);
        totalScore += (slaScore * WEIGHT_SLA_URGENCY) / 100;

        int cutoffScore = calculateCutoffScore(task);
        totalScore += (cutoffScore * WEIGHT_CARRIER_CUTOFF) / 100;

        int customerScore = calculateCustomerScore(task);
        totalScore += (customerScore * WEIGHT_CUSTOMER_TIER) / 100;

        int zoneScore = calculateZoneScore(task);
        totalScore += (zoneScore * WEIGHT_ZONE_EFFICIENCY) / 100;

        int ageScore = calculateAgeScore(task);
        totalScore += (ageScore * WEIGHT_AGE) / 100;

        totalScore = applyTaskTypeModifier(totalScore, task.getType());

        if (isExpress(task)) {
            totalScore = (int) (totalScore * 1.5);
        }

        totalScore = totalScore * 5;
        totalScore = Math.max(0, Math.min(1000, totalScore));

        logger.debug(
                "Calculated priority {} for task {} (SLA:{}, Cutoff:{}, Customer:{}, Zone:{}, Age:{})",
                totalScore,
                task.getTaskId(),
                slaScore,
                cutoffScore,
                customerScore,
                zoneScore,
                ageScore
        );

        return totalScore;
    }

    /**
     * Calculate priority ensuring it is never lower than a baseline Priority.
     */
    public int calculatePriority(WorkTask task, Priority basePriority) {
        int calculatedScore = calculatePriority(task);
        int baseScore = priorityToScore(basePriority);
        return Math.max(calculatedScore, baseScore);
    }

    private int calculateSLAScore(WorkTask task) {
        LocalDateTime deadline = task.getDeadline();
        if (deadline == null) {
            return 50;
        }

        LocalDateTime now = LocalDateTime.now();
        long hoursRemaining = Duration.between(now, deadline).toHours();

        if (hoursRemaining < 0) {
            return 100;
        } else if (hoursRemaining < 1) {
            return 95;
        } else if (hoursRemaining < 2) {
            return 90;
        } else if (hoursRemaining < 4) {
            return 80;
        } else if (hoursRemaining < 8) {
            return 70;
        } else if (hoursRemaining < 24) {
            return 60;
        } else if (hoursRemaining < 48) {
            return 40;
        } else {
            return 20;
        }
    }

    private int calculateCutoffScore(WorkTask task) {
        LocalDateTime carrierCutoff = getCarrierCutoffTime(task);
        if (carrierCutoff == null) {
            return 30;
        }

        LocalDateTime now = LocalDateTime.now();
        long minutesRemaining = Duration.between(now, carrierCutoff).toMinutes();

        if (minutesRemaining < 0) {
            return 100;
        } else if (minutesRemaining < 30) {
            return 95;
        } else if (minutesRemaining < 60) {
            return 90;
        } else if (minutesRemaining < 120) {
            return 80;
        } else if (minutesRemaining < 240) {
            return 70;
        } else if (minutesRemaining < 480) {
            return 50;
        } else {
            return 20;
        }
    }

    private int calculateCustomerScore(WorkTask task) {
        String customerTier = getCustomerTier(task);
        if (customerTier == null) {
            return 50;
        }

        String normalized = customerTier.trim().toUpperCase(Locale.ENGLISH);
        return switch (normalized) {
            case "PLATINUM" -> 100;
            case "GOLD" -> 85;
            case "SILVER" -> 70;
            case "BRONZE" -> 55;
            case "STANDARD" -> 40;
            default -> {
                double multiplier = CUSTOMER_TIER_MULTIPLIERS.getOrDefault(normalized, 1.0);
                yield (int) Math.round(50 * multiplier);
            }
        };
    }

    private int calculateZoneScore(WorkTask task) {
        String zone = task.getZone();
        if (zone == null || zone.isBlank()) {
            return 50;
        }

        String normalized = zone.toUpperCase(Locale.ENGLISH);
        if (normalized.startsWith("PICK-A") || normalized.startsWith("ZONE-A")) {
            return 90;
        } else if (normalized.startsWith("PICK-B") || normalized.startsWith("ZONE-B")) {
            return 70;
        } else if (normalized.startsWith("PICK-C") || normalized.startsWith("ZONE-C")) {
            return 50;
        } else {
            return 30;
        }
    }

    private int calculateAgeScore(WorkTask task) {
        LocalDateTime createdAt = task.getCreatedAt();
        if (createdAt == null) {
            return 0;
        }

        long hoursOld = Duration.between(createdAt, LocalDateTime.now()).toHours();

        if (hoursOld > 24) {
            return 100;
        } else if (hoursOld > 12) {
            return 80;
        } else if (hoursOld > 6) {
            return 60;
        } else if (hoursOld > 2) {
            return 40;
        } else {
            return 20;
        }
    }

    private int applyTaskTypeModifier(int baseScore, TaskType taskType) {
        if (taskType == null) {
            return baseScore;
        }

        return switch (taskType) {
            case COUNT -> (int) (baseScore * 0.6);
            case REPLENISH -> (int) (baseScore * 0.8);
            case PICK -> baseScore;
            case PACK -> (int) (baseScore * 0.9);
            case PUTAWAY -> (int) (baseScore * 1.1);
            case MOVE -> (int) (baseScore * 0.7);
            case SHIP -> (int) (baseScore * 1.2);
        };
    }

    /**
     * Calculate dynamic priority that adjusts over time based on contextual signals.
     */
    public int calculateDynamicPriority(WorkTask task, Map<String, Object> context) {
        int baseScore = calculatePriority(task);

        if (context != null) {
            if (Boolean.TRUE.equals(context.get("operatorInSameZone"))) {
                baseScore = (int) (baseScore * 1.15);
            }
            if (Boolean.TRUE.equals(context.get("partOfBatch"))) {
                baseScore = (int) (baseScore * 1.1);
            }
            if (Boolean.FALSE.equals(context.get("waveReleased"))) {
                baseScore = (int) (baseScore * 0.5);
            }
            if (Boolean.TRUE.equals(context.get("resolvingException"))) {
                baseScore = (int) (baseScore * 1.3);
            }

            Object surgeLevel = context.get("systemSurgeLevel");
            if (surgeLevel instanceof Number number) {
                baseScore = (int) (baseScore * (1 + Math.min(number.doubleValue(), 3.0) * 0.05));
            }
        }

        return Math.min(1000, baseScore);
    }

    /**
     * Recommend priority adjustments based on current score and system load.
     */
    public PriorityAdjustment recommendAdjustment(WorkTask task, SystemLoadMetrics loadMetrics) {
        int currentPriority = priorityToScore(task.getPriority());
        int calculatedPriority = calculatePriority(task);

        if (Math.abs(currentPriority - calculatedPriority) > 50) {
            String reason = buildAdjustmentReason(task, loadMetrics, calculatedPriority);
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

    private String buildAdjustmentReason(WorkTask task, SystemLoadMetrics loadMetrics, int calculatedPriority) {
        StringBuilder reason = new StringBuilder("Priority adjustment recommended: ");

        LocalDateTime deadline = task.getDeadline();
        if (deadline != null) {
            long hoursRemaining = Duration.between(LocalDateTime.now(), deadline).toHours();
            if (hoursRemaining < 2) {
                reason.append("Approaching SLA deadline. ");
            }
        }

        LocalDateTime cutoff = getCarrierCutoffTime(task);
        if (cutoff != null) {
            long minutesRemaining = Duration.between(LocalDateTime.now(), cutoff).toMinutes();
            if (minutesRemaining < 60) {
                reason.append("Carrier cutoff imminent. ");
            }
        }

        if (isExpress(task)) {
            reason.append("Express handling required. ");
        }

        String customerTier = getCustomerTier(task);
        if ("PLATINUM".equalsIgnoreCase(customerTier) || "GOLD".equalsIgnoreCase(customerTier)) {
            reason.append("High value customer. ");
        }

        if (loadMetrics != null) {
            if (loadMetrics.getQueueDepth() > 10) {
                reason.append("High queue depth. ");
            }
            if (loadMetrics.getActiveOperators() < 2) {
                reason.append("Operator shortage. ");
            }
            if (calculatedPriority > currentSystemBaseline(loadMetrics)) {
                reason.append("Calculated priority above system baseline. ");
            }
        }

        return reason.toString().trim();
    }

    private int currentSystemBaseline(SystemLoadMetrics loadMetrics) {
        if (loadMetrics == null) {
            return 500;
        }
        int base = 500;
        base += Math.min(loadMetrics.getQueueDepth(), 20) * 5;
        base -= Math.min(loadMetrics.getActiveOperators(), 10) * 10;
        base += (int) Math.min(loadMetrics.getAverageTaskTime() * 2, 100);
        return Math.max(200, Math.min(900, base));
    }

    private boolean isExpress(WorkTask task) {
        Map<String, Object> metadata = metadata(task);

        Object expressFlag = metadata.get("express");
        if (expressFlag == null) {
            expressFlag = metadata.get("isExpress");
        }

        if (expressFlag instanceof Boolean bool) {
            return bool;
        }
        if (expressFlag instanceof String string) {
            return Boolean.parseBoolean(string);
        }

        Priority priority = task.getPriority();
        return priority != null && priority.isExpedited();
    }

    private LocalDateTime getCarrierCutoffTime(WorkTask task) {
        Object value = metadata(task).get("carrierCutoffTime");
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return LocalDateTime.parse(str, ISO_DATE_TIME);
            } catch (DateTimeParseException ignored) {
                logger.debug("Unable to parse carrierCutoffTime '{}' for task {}", str, task.getTaskId());
            }
        }
        return null;
    }

    private String getCustomerTier(WorkTask task) {
        Object value = metadata(task).get("customerTier");
        if (value instanceof String str && !str.isBlank()) {
            return str;
        }

        Priority priority = task.getPriority();
        if (priority != null && priority.isExpedited()) {
            return "PLATINUM";
        }
        return null;
    }

    private int priorityToScore(Priority priority) {
        if (priority == null) {
            return 450;
        }

        return switch (priority) {
            case CRITICAL -> 900;
            case URGENT -> 750;
            case HIGH -> 600;
            case NORMAL -> 450;
            case LOW -> 300;
        };
    }

    private Map<String, Object> metadata(WorkTask task) {
        if (task == null) {
            return Collections.emptyMap();
        }
        TaskContext context = task.getContext();
        if (context == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> metadata = context.getMetadata();
        return metadata != null ? metadata : Collections.emptyMap();
    }

    /**
     * Priority adjustment recommendation DTO.
     */
    public static class PriorityAdjustment {
        private final String taskId;
        private final int currentPriority;
        private final int recommendedPriority;
        private final boolean shouldAdjust;
        private final String reason;

        public PriorityAdjustment(String taskId,
                                  int currentPriority,
                                  int recommendedPriority,
                                  boolean shouldAdjust,
                                  String reason) {
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
     * Snapshot of system load metrics used when making priority recommendations.
     */
    public static class SystemLoadMetrics {
        private final int queueDepth;
        private final int activeOperators;
        private final double averageTaskTime;
        private final Map<String, Integer> tasksByZone;

        public SystemLoadMetrics(int queueDepth,
                                 int activeOperators,
                                 double averageTaskTime,
                                 Map<String, Integer> tasksByZone) {
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
