package com.campusbite.broadcast;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BroadcastMessageRepository extends JpaRepository<BroadcastMessage, Long> {
    Optional<BroadcastMessage> findFirstByActiveTrueOrderByUpdatedAtDesc();
}
