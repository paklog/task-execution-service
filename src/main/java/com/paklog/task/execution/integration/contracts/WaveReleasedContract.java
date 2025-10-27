package com.paklog.task.execution.integration.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Contract for WaveReleasedEvent from wave-planning-service
 * This is the consumer's view of the external event
 * Maps external event to internal domain (Anti-Corruption Layer)
 */
public record WaveReleasedContract(
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("wave_number") String waveNumber,
    @JsonProperty("priority") String priority,
    @JsonProperty("order_ids") List<String> orderIds,
    @JsonProperty("total_lines") int totalLines,
    @JsonProperty("zone_id") String zoneId,
    @JsonProperty("released_at") Instant releasedAt
) {
    /**
     * Expected CloudEvent type from wave-planning-service
     */
    public static final String EVENT_TYPE = "com.paklog.wms.wave-planning.wave.wave.released.v1";
}
