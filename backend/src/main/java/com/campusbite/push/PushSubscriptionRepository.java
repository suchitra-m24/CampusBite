package com.campusbite.push;

import com.campusbite.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUser(User user);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    @Transactional
    void deleteByEndpointAndUser(String endpoint, User user);

    @Transactional
    void deleteByEndpoint(String endpoint);
}