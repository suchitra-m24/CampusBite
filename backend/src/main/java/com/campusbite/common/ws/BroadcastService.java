package com.campusbite.common.ws;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Centralized WebSocket broadcasting. Transactional callers publish only after a successful commit. */
@Service
public class BroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public BroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastMenuUpdate() {
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/menu", "MENU_UPDATED"));
    }

    public void broadcastKitchenLoad(Object payload) {
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/kitchen-load", payload));
    }

    public void broadcastNewOrderToStaff(Long orderId, String status) {
        OrderBroadcastEvent event = new OrderBroadcastEvent(orderId, status);
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/orders/staff", event));
    }

    public void broadcastOrderUpdateToStudent(Long userId, Long orderId, String status) {
        OrderBroadcastEvent event = new OrderBroadcastEvent(orderId, status);
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/orders/user/" + userId, event));
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
