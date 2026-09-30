package com.campusbite.canteen;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CanteenClosureRepository extends JpaRepository<CanteenClosure, Long> {
    boolean existsByClosureDate(LocalDate date);
    List<CanteenClosure> findAllByClosureDateGreaterThanEqualOrderByClosureDateAsc(LocalDate date);
}
