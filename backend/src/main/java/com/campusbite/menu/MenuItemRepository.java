package com.campusbite.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByCategoryId(Long categoryId);
    List<MenuItem> findByAvailableTrue();
    List<MenuItem> findByDailySpecialTrue();
}
