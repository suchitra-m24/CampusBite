package com.campusbite.slot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlotCapacityRepository extends JpaRepository<SlotCapacity, Long> {
    Optional<SlotCapacity> findByWindowStartMinuteOfDay(int minuteOfDay);
}
