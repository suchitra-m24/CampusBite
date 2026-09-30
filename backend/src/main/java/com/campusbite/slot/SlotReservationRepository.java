package com.campusbite.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SlotReservationRepository extends JpaRepository<SlotReservation, Long> {

    Optional<SlotReservation> findByWindowStart(Instant windowStart);

    /** Creates the counter row if it does not already exist without turning a concurrent
     * duplicate insert into a failed transaction. MySQL's INSERT IGNORE is used deliberately
     * because the unique window_start constraint is the concurrency primitive here. */
    @Modifying
    @Query(value = "INSERT IGNORE INTO slot_reservations (created_at, updated_at, window_start, reserved_count) VALUES (CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), :windowStart, 0)", nativeQuery = true)
    int ensureRow(@Param("windowStart") Instant windowStart);

    /**
     * Atomic reserve: succeeds (returns 1) only if room remains at the
     * instant the row lock is acquired. Two concurrent attempts against a
     * slot with exactly one spot left will serialize at the database --
     * only one can possibly return 1. See SlotReservationConcurrencyTest.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE SlotReservation s SET s.reservedCount = s.reservedCount + 1 " +
            "WHERE s.windowStart = :windowStart AND s.reservedCount < :capacity")
    int reserveIfRoom(@Param("windowStart") Instant windowStart, @Param("capacity") int capacity);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE SlotReservation s SET s.reservedCount = CASE WHEN s.reservedCount > 0 THEN s.reservedCount - 1 ELSE 0 END " +
            "WHERE s.windowStart = :windowStart")
    int release(@Param("windowStart") Instant windowStart);
}