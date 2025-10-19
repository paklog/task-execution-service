package com.paklog.wes.task.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationTest {

    @Test
    void fromCodeParsesAllSegments() {
        Location location = Location.fromCode("A-01-02-03");

        assertThat(location.getAisle()).isEqualTo("A");
        assertThat(location.getBay()).isEqualTo("01");
        assertThat(location.getLevel()).isEqualTo("02");
        assertThat(location.getPosition()).isEqualTo("03");
        assertThat(location.getLocationCode()).isEqualTo("A-01-02-03");
    }

    @Test
    void fromCodeThrowsWhenFormatInvalid() {
        assertThatThrownBy(() -> Location.fromCode("A-01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid location code format");
    }

    @Test
    void distanceFromReturnsLargeValueWhenOtherIsNull() {
        Location location = Location.fromCode("A-01-01");
        assertThat(location.distanceFrom(null)).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    void distanceFromCalculatesManhattanDistance() {
        Location current = Location.fromCode("A-01-01-01");
        Location target = Location.fromCode("B-03-02-01");

        double distance = current.distanceFrom(target);

        assertThat(distance).isGreaterThan(0);
        assertThat(distance).isEqualTo( (Math.abs("A".compareTo("B")) * 100.0) + (2 * 10.0) + (1 * 1.0) );
    }

    @Test
    void inSameZoneComparesAisle() {
        Location one = Location.fromCode("A-01-01");
        Location two = Location.fromCode("A-05-02");
        Location three = Location.fromCode("B-01-01");

        assertThat(one.inSameZone(two)).isTrue();
        assertThat(one.inSameZone(three)).isFalse();
    }
}
