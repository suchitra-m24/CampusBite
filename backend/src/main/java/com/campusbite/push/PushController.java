package com.campusbite.push;
import org.springframework.dao.DataIntegrityViolationException;
import com.campusbite.common.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PushController {

    private final PushSubscriptionRepository subscriptionRepository;
    private final String vapidPublicKey;

    public PushController(PushSubscriptionRepository subscriptionRepository,
                          @Value("${campusbite.push.vapid-public-key}") String vapidPublicKey) {
        this.subscriptionRepository = subscriptionRepository;
        this.vapidPublicKey = vapidPublicKey;
    }

    @GetMapping("/api/push/vapid-public-key")
    public ResponseEntity<String> publicKey() {
        return ResponseEntity.ok(vapidPublicKey);
    }

    @PostMapping("/api/student/push/subscribe")
    public ResponseEntity<Void> subscribe(@AuthenticationPrincipal SecurityUser principal,
                                          @Valid @RequestBody PushSubscriptionRequest request) {
        try {
            PushSubscription existing = subscriptionRepository.findByEndpoint(request.endpoint()).orElse(null);
            if (existing == null) {
                subscriptionRepository.save(new PushSubscription(
                        principal.getUser(), request.endpoint(), request.p256dh(), request.auth()));
            } else {
                existing.setUser(principal.getUser());
                subscriptionRepository.save(existing);
            }
        } catch (DataIntegrityViolationException e) {
            // Another request already saved this exact subscription (e.g. a double
            // click, or a stale client retry) -- that's fine, it exists either way.
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/student/push/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@AuthenticationPrincipal SecurityUser principal,
                                             @Valid @RequestBody UnsubscribeRequest request) {
        subscriptionRepository.deleteByEndpointAndUser(request.endpoint(), principal.getUser());
        return ResponseEntity.noContent().build();
    }
}