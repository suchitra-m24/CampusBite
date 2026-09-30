package com.campusbite.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByLoginId(String loginId);
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrLoginIdContainingIgnoreCase(
            String name, String email, String loginId);
}
