package com.campusbite.push;

import com.campusbite.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.Security;
import java.util.List;
import java.util.Map;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PushService pushService;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepository,
                                   @Value("${campusbite.push.vapid-public-key}") String publicKey,
                                   @Value("${campusbite.push.vapid-private-key}") String privateKey,
                                   @Value("${campusbite.push.vapid-subject}") String subject) throws Exception {
        this.subscriptionRepository = subscriptionRepository;
        Security.addProvider(new BouncyCastleProvider());
        this.pushService = (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank())
                ? null : new PushService(publicKey, privateKey, subject);
    }

    public void notifyUserAfterCommit(User user, String title, String body, String url) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notifyUser(user, title, body, url);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notifyUser(user, title, body, url);
            }
        });
    }

    public void notifyUser(User user, String title, String body, String url) {
        if (pushService == null) {
            log.debug("Web Push is disabled because VAPID keys are not configured");
            return;
        }
        List<PushSubscription> subscriptions = subscriptionRepository.findByUser(user);
        log.debug("Push: found {} subscription(s) for user", subscriptions.size());
        if (subscriptions.isEmpty()) return;

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of("title", title, "body", body, "url", url));
        } catch (Exception e) {
            log.error("Push: failed to build payload", e);
            return;
        }

        for (PushSubscription sub : subscriptions) {
            try {
                // Explicit TTL is required -- some push services silently discard
                // messages sent without one, even while still returning 201.
                Notification notification = new Notification(
                        sub.getEndpoint(), sub.getP256dh(), sub.getAuth(), payload.getBytes(), 2419200);
                HttpResponse response = pushService.send(notification);
                int status = response.getStatusLine().getStatusCode();
                log.debug("Push delivery returned HTTP {}", status);
                if (status == 404 || status == 410) {
                    subscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                }
            } catch (Exception e) {
                log.error("Push: failed to deliver notification", e);
            }
        }
    }
}