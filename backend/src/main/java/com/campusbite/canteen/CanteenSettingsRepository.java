package com.campusbite.canteen;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CanteenSettingsRepository extends JpaRepository<CanteenSettings, Long> {
    Optional<CanteenSettings> findTopByOrderByIdAsc();
}
