package com.paklog.wes.task.adapter.rest.dto;

import com.paklog.wes.task.domain.valueobject.Location;

/**
 * Location DTO for REST API
 */
public record LocationDto(
        String aisle,
        String bay,
        String level,
        String position
) {
    public static LocationDto fromDomain(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationDto(
                location.getAisle(),
                location.getBay(),
                location.getLevel(),
                location.getPosition()
        );
    }

    public Location toDomain() {
        return new Location(aisle, bay, level, position);
    }
}
