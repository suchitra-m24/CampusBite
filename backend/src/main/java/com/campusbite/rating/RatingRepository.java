package com.campusbite.rating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    @Query("select avg(r.stars) from Rating r")
    Double averageStars();

    @Query("select count(r) from Rating r")
    long countAll();
}