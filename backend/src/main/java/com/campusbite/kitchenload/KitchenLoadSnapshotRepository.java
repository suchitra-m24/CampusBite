package com.campusbite.kitchenload;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface KitchenLoadSnapshotRepository extends JpaRepository<KitchenLoadSnapshot, Long> {

    @Query("select k from KitchenLoadSnapshot k where k.createdAt between :from and :to order by k.createdAt asc")
    List<KitchenLoadSnapshot> findBetween(Instant from, Instant to);
}
