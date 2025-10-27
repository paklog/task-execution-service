package com.paklog.task.execution.domain.valueobject;

/**
 * Task priority levels
 * Copied from paklog-domain to eliminate compilation dependency
 */
public enum Priority {
    CRITICAL(1),  // Process immediately (critical/urgent)
    URGENT(1),    // Process immediately (alias for CRITICAL)
    HIGH(2),      // Process soon
    NORMAL(3),    // Standard processing
    LOW(4);       // Process when capacity available

    private final int value;

    Priority(int value) {
        this.value = value;
    }

    /**
     * Get numeric value of priority (lower is more urgent)
     */
    public int getValue() {
        return value;
    }

    /**
     * Check if this priority is expedited (CRITICAL/URGENT or HIGH)
     */
    public boolean isExpedited() {
        return this == CRITICAL || this == URGENT || this == HIGH;
    }

    /**
     * Parse priority from string (case-insensitive)
     */
    public static Priority fromString(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        try {
            return Priority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
