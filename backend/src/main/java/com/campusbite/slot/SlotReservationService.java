package com.campusbite.slot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class SlotReservationService {

    private final SlotReservationRepository repository;

    public SlotReservationService(SlotReservationRepository repository) {
        this.repository = repository;
    }

    /** Ensures the counter row exists without a duplicate-insert exception, then atomically claims one spot. */
    @Transactional
    public boolean tryReserve(Instant windowStart, int capacity) {
        if (capacity <= 0) {
            return false;
        }
        repository.ensureRow(windowStart);
        return repository.reserveIfRoom(windowStart, capacity) == 1;
    }

    @Transactional
    public void release(Instant windowStart) {
        repository.release(windowStart);
    }

    public int getReservedCount(Instant windowStart) {
        return repository.findByWindowStart(windowStart).map(SlotReservation::getReservedCount).orElse(0);
    }
}