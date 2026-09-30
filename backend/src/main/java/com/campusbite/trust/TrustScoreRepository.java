package com.campusbite.trust;

import com.campusbite.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrustScoreRepository extends JpaRepository<TrustScore, Long> {
    Optional<TrustScore> findByUser(User user);
    Optional<TrustScore> findByUserId(Long userId);
}
