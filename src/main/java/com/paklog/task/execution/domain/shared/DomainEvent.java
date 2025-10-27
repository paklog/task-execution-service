package com.paklog.task.execution.domain.shared;

import java.time.Instant;

/**
 * Base interface for all domain events in Task Execution bounded context
 * Copied from paklog-domain to eliminate compilation dependency
 */
public interface DomainEvent {

    /**
     * When the event occurred
     */
    Instant occurredOn();

    /**
     * Type of the event
     */
    String eventType();
}
