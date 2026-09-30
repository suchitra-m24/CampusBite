package com.campusbite.order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.campusbite.push.PushNotificationService;
import com.campusbite.cart.Cart;
import com.campusbite.cart.CartItem;
import com.campusbite.cart.CartRepository;
import com.campusbite.canteen.CanteenService;
import com.campusbite.common.exception.BadRequestException;
import com.campusbite.common.exception.ResourceNotFoundException;
import com.campusbite.common.util.QrCodeUtil;
import com.campusbite.common.util.TokenGenerator;
import com.campusbite.common.ws.BroadcastService;
import com.campusbite.kitchenload.KitchenLoadService;
import com.campusbite.menu.MenuItem;
import com.campusbite.order.dto.OrderResponse;
import com.campusbite.slot.SlotAllocationService;
import com.campusbite.slot.dto.SlotAllocationResult;
import com.campusbite.trust.TrustScoreService;
import com.campusbite.user.User;
import com.campusbite.wallet.WalletService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.campusbite.user.PaymentMode;
@Service
public class OrderService {

    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final SlotAllocationService slotAllocationService;
    private final PrepTimePredictionService prepTimePredictionService;
    private final KitchenLoadService kitchenLoadService;
    private final TrustScoreService trustScoreService;
    private final WalletService walletService;
    private final BroadcastService broadcastService;
    private final PushNotificationService pushNotificationService;
    private final int cancellationWindowMinutes;
    private final com.campusbite.slot.SlotReservationService slotReservationService;
    private final CanteenService canteenService;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        SlotAllocationService slotAllocationService,
                        PrepTimePredictionService prepTimePredictionService,
                        KitchenLoadService kitchenLoadService,
                        TrustScoreService trustScoreService,
                        WalletService walletService,
                        BroadcastService broadcastService,
                        PushNotificationService pushNotificationService,
                        com.campusbite.slot.SlotReservationService slotReservationService,
                        @Value("${campusbite.order.cancellation-window-minutes}") int cancellationWindowMinutes,
                        CanteenService canteenService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.slotAllocationService = slotAllocationService;
        this.prepTimePredictionService = prepTimePredictionService;
        this.kitchenLoadService = kitchenLoadService;
        this.trustScoreService = trustScoreService;
        this.walletService = walletService;
        this.broadcastService = broadcastService;
        this.pushNotificationService = pushNotificationService;
        this.cancellationWindowMinutes = cancellationWindowMinutes;
        this.slotReservationService = slotReservationService;
        this.canteenService = canteenService;
    }

    @Transactional
    public OrderResult placeOrder(User student, String specialInstructions) {
        if (student.getRole() != com.campusbite.user.Role.STUDENT && student.getRole() != com.campusbite.user.Role.TEACHER) {
            throw new BadRequestException("Only student and teacher accounts can place canteen orders");
        }
        Cart cart = cartRepository.findByStudent(student)
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        if (!canteenService.getSettings().isOrderingEnabled()) {
            throw new BadRequestException("The canteen is currently closed for online ordering.");
        }

        for (CartItem ci : cart.getItems()) {
            if (!ci.getMenuItem().isAvailable()) {
                throw new BadRequestException(ci.getMenuItem().getName() + " just sold out. Please update your cart.");
            }
        }

        // Trust-score gate: students below the restricted threshold may only
        // hold one active order at a time, so they can't hoard slots.
        if (trustScoreService.isRestricted(student)) {
            long activeCount = orderRepository.findByStudentOrderByPlacedAtDesc(student).stream()
                    .filter(o -> ACTIVE_STATUSES.contains(o.getStatus()))
                    .count();
            if (activeCount >= 1) {
                throw new BadRequestException(
                        "Your account currently has limited slot access due to recent no-shows. " +
                        "Complete pickup of your current order before placing a new one.");
            }
        }

        Map<MenuItem, Integer> quantities = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cart.getItems()) {
            quantities.merge(ci.getMenuItem(), ci.getQuantity(), Integer::sum);
            total = total.add(ci.getMenuItem().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        int estimatedPrepMinutes = prepTimePredictionService.estimateForCart(quantities);
        int backlogMinutes = kitchenLoadService.getCurrentBacklogMinutes();
        SlotAllocationResult slot = slotAllocationService.allocate(estimatedPrepMinutes, backlogMinutes);

        // Build the order before charging so the wallet ledger can retain the
        // exact order id. Everything is still inside this single transaction:
        // a failed debit rolls back both the order and the slot reservation.
        Order order = new Order();
        order.setStudent(student);
        order.setStatus(OrderStatus.PLACED);
        order.setPlacedAt(Instant.now());
        order.setPickupSlotStart(slot.slotStart());
        order.setPickupSlotEnd(slot.slotEnd());
        order.setEstimatedPrepMinutes(estimatedPrepMinutes);
        order.setPickupToken(TokenGenerator.generatePickupToken());
        order.setSpecialInstructions(specialInstructions);
        order.setTotalAmount(total);

        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setMenuItem(ci.getMenuItem());
            oi.setMenuItemNameSnapshot(ci.getMenuItem().getName());
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(ci.getMenuItem().getPrice());
            oi.setSelectedCustomizations(ci.getSelectedCustomizations());
            order.getItems().add(oi);
        }

        Order saved = orderRepository.save(order);

        if (student.getPaymentMode() == PaymentMode.WALLET) {
            walletService.debitForOrder(student, total, "Order payment", saved.getId());
        }
        // POSTPAID users (e.g. teachers) aren't charged now -- BillingService
        // rolls their order totals into a monthly statement instead.

        cart.getItems().clear();
        cartRepository.save(cart);

        broadcastService.broadcastNewOrderToStaff(saved.getId(), saved.getStatus().name());

        return new OrderResult(saved, true);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrderHistory(User student) {
        return orderRepository.findByStudentOrderByPlacedAtDesc(student).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Paginated version -- prefer this for anything user-facing; the unpaged one above stays for internal checks. */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrderHistoryPaged(User student, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return orderRepository.findByStudentOrderByPlacedAtDesc(student, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, User requester) {
        Order order = findOrder(orderId);
        assertOwnerOrStaff(order, requester);
        return toResponse(order);
    }

    /** Staff live board: everything not yet picked up / closed out, earliest slot first. */
    @Transactional(readOnly = true)
    public List<OrderResponse> getKitchenQueue() {
        return orderRepository.findByStatusInOrderByPickupSlotStartAsc(
                        List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY))
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse advanceStatus(Long orderId, OrderStatus newStatus) {
        Order order = findOrder(orderId);
        validateTransition(order.getStatus(), newStatus);

        Instant now = Instant.now();
        switch (newStatus) {
            case CONFIRMED -> order.setConfirmedAt(now);
            case PREPARING -> order.setPreparingAt(now);
            case READY -> {
                order.setReadyAt(now);
                Instant started = order.getPreparingAt() != null ? order.getPreparingAt() : order.getConfirmedAt();
                if (started != null) {
                    order.setActualPrepMinutes((int) ChronoUnit.MINUTES.between(started, now));
                }
                pushNotificationService.notifyUserAfterCommit(
                        order.getStudent(),
                        "Order ready for pickup!",
                        "Order #" + order.getId() + " is ready -- show your code " + order.getPickupToken() + " at the counter.",
                        "/student/orders.html");
            }
            case PICKED_UP -> {
                order.setPickedUpAt(now);
                trustScoreService.recordSuccessfulPickup(order.getStudent());
            }
            default -> { /* no timestamp for other states here */ }
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        OrderResponse response = toResponse(saved);
        broadcastService.broadcastOrderUpdateToStudent(saved.getStudent().getId(), saved.getId(), saved.getStatus().name());
        broadcastService.broadcastNewOrderToStaff(saved.getId(), saved.getStatus().name()); // refresh staff board too
        return response;
    }

    /** Staff scans the student's QR / types the pickup token to close out the order. */
    @Transactional
    public OrderResponse completeByToken(String pickupToken) {
        Order order = orderRepository.findByPickupToken(pickupToken)
                .orElseThrow(() -> new ResourceNotFoundException("No order found for this pickup code"));
        if (order.getStatus() != OrderStatus.READY) {
            throw new BadRequestException("This order is not ready for pickup yet (current status: " + order.getStatus() + ")");
        }
        return advanceStatus(order.getId(), OrderStatus.PICKED_UP);
    }

    /**
     * Lets a student adjust quantities on an order's EXISTING items (or
     * remove one entirely by setting quantity to 0) while it's still
     * PLACED -- once staff confirms it, the kitchen has committed to it and
     * it can no longer be edited, only cancelled. The pickup slot and prep
     * estimate are intentionally left as-is (a known simplification): a
     * small quantity tweak doesn't retrigger slot re-allocation.
     */
    @Transactional
    public OrderResponse updateItems(Long orderId, User requester, com.campusbite.order.dto.UpdateOrderItemsRequest request) {
        Order order = findOrder(orderId);
        assertOwnerOrStaff(order, requester);

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BadRequestException("This order can no longer be edited (current status: " + order.getStatus() + ")");
        }

        BigDecimal oldTotal = order.getTotalAmount();

        for (com.campusbite.order.dto.OrderItemUpdateRequest update : request.items()) {
            OrderItem item = order.getItems().stream()
                    .filter(i -> i.getId().equals(update.orderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("That item isn't part of this order"));
            if (update.quantity() <= 0) {
                order.getItems().remove(item);
            } else {
                item.setQuantity(update.quantity());
            }
        }

        if (order.getItems().isEmpty()) {
            throw new BadRequestException("An order needs at least one item -- cancel it instead if you want to remove everything");
        }

        BigDecimal newTotal = order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(newTotal);
        Order saved = orderRepository.save(order);
        // Build the response before wallet bulk updates clear the persistence context.
        OrderResponse response = toResponse(saved);
        Long studentId = saved.getStudent().getId();

        if (saved.getStudent().getPaymentMode() == PaymentMode.WALLET) {
            int comparison = newTotal.compareTo(oldTotal);
            if (comparison > 0) {
                walletService.debitForOrder(saved.getStudent(), newTotal.subtract(oldTotal), "Order #" + saved.getId() + " updated -- additional charge");
            } else if (comparison < 0) {
                walletService.refund(saved.getStudent(), oldTotal.subtract(newTotal), "Order #" + saved.getId() + " updated -- partial refund", saved.getId());
            }
        }

        broadcastService.broadcastOrderUpdateToStudent(studentId, saved.getId(), saved.getStatus().name());
        broadcastService.broadcastNewOrderToStaff(saved.getId(), saved.getStatus().name());
        return response;
    }

    /** Admin: find any order regardless of status, by ID, pickup token, or student name/email/login ID. */
    @Transactional(readOnly = true)
    public List<OrderResponse> searchOrders(String query) {
        Pageable pageable = PageRequest.of(0, 100);
        return orderRepository.searchOrders(query, pageable).map(this::toResponse).getContent();
    }

    /** Staff: search completed/cancelled orders (the live queue only shows active ones). */
    @Transactional(readOnly = true)
    public Page<OrderResponse> staffOrderHistory(String query, int page, int size) {
        List<OrderStatus> statuses = List.of(OrderStatus.PICKED_UP, OrderStatus.CANCELLED, OrderStatus.NO_SHOW);
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        if (query == null || query.isBlank()) {
            return orderRepository.findByStatusInOrderByPlacedAtDesc(statuses, pageable).map(this::toResponse);
        }
        return orderRepository.searchByStatusesAndQuery(statuses, query, pageable).map(this::toResponse);
    }

    @Transactional
    public OrderResponse cancel(Long orderId, User requester) {
        return cancel(orderId, requester, null);
    }

    @Transactional
    public OrderResponse cancel(Long orderId, User requester, String reason) {
        Order order = findOrder(orderId);
        assertOwnerOrStaff(order, requester);

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("This order can no longer be cancelled (current status: " + order.getStatus() + ")");
        }

        long minutesUntilSlot = ChronoUnit.MINUTES.between(Instant.now(), order.getPickupSlotStart());
        boolean isLate = minutesUntilSlot < cancellationWindowMinutes;
        User student = order.getStudent();
        PaymentMode paymentMode = student.getPaymentMode();
        BigDecimal total = order.getTotalAmount();
        Long studentId = student.getId();

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setCancelReason(reason == null || reason.isBlank() ? "Cancelled by user." : reason.trim());
        Order saved = orderRepository.save(order);
        // Materialize the response before bulk wallet/slot updates clear the persistence context.
        OrderResponse response = toResponse(saved);
        slotReservationService.release(order.getPickupSlotStart());

        if (paymentMode == PaymentMode.WALLET) {
            walletService.refund(student, total, "Refund for cancelled order #" + order.getId(), order.getId());
        }

        if (isLate) {
            trustScoreService.recordLateCancel(student);
        }

        broadcastService.broadcastOrderUpdateToStudent(studentId, saved.getId(), saved.getStatus().name());
        broadcastService.broadcastNewOrderToStaff(saved.getId(), saved.getStatus().name());
        return response;
    }

    /** Marks one delayed order in its own transaction so a single failure cannot poison the scheduler batch. */
    @Transactional
    public void markDelayNotified(Long orderId) {
        Order order = findOrder(orderId);
        if (order.isDelayNotified() || !WATCHED_STATUSES_FOR_DELAY.contains(order.getStatus())) {
            return;
        }
        order.setDelayNotified(true);
        orderRepository.save(order);
        pushNotificationService.notifyUserAfterCommit(
                order.getStudent(),
                "Running a bit behind",
                "Order #" + order.getId() + " is delayed due to high kitchen demand -- we're on it, thanks for your patience!",
                "/student/orders.html");
    }

    private static final List<OrderStatus> WATCHED_STATUSES_FOR_DELAY =
            List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PREPARING);

    /** Marks one READY order as NO_SHOW in its own transaction so one bad row cannot poison the whole scheduler batch. */
    @Transactional
    public OrderResponse markNoShow(Long orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.READY) {
            throw new BadRequestException("Only READY orders can be marked as no-show");
        }
        order.setStatus(OrderStatus.NO_SHOW);
        Order saved = orderRepository.save(order);
        OrderResponse response = toResponse(saved);
        trustScoreService.recordNoShow(saved.getStudent());
        pushNotificationService.notifyUserAfterCommit(
                saved.getStudent(),
                "Order marked as no-show",
                "Order #" + saved.getId() + " was not collected within the pickup window.",
                "/student/orders.html");
        broadcastService.broadcastOrderUpdateToStudent(saved.getStudent().getId(), saved.getId(), saved.getStatus().name());
        broadcastService.broadcastNewOrderToStaff(saved.getId(), saved.getStatus().name());
        return response;
    }

    /** Automatically expires a stale PLACED/CONFIRMED order after its slot or closure. */
    @Transactional
    public OrderResponse expireOrder(Long orderId, String reason) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Only PLACED or CONFIRMED orders can be automatically expired");
        }

        User student = order.getStudent();
        PaymentMode paymentMode = student.getPaymentMode();
        BigDecimal total = order.getTotalAmount();
        Long studentId = student.getId();

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setCancelReason(reason);
        Order saved = orderRepository.save(order);
        // Materialize before bulk wallet/slot operations clear the persistence context.
        OrderResponse response = toResponse(saved);
        slotReservationService.release(order.getPickupSlotStart());

        if (paymentMode == PaymentMode.WALLET) {
            walletService.refund(student, total, reason + " Refund for order #" + order.getId(), order.getId());
        }

        broadcastService.broadcastOrderUpdateToStudent(studentId, saved.getId(), saved.getStatus().name());
        broadcastService.broadcastNewOrderToStaff(saved.getId(), saved.getStatus().name());
        pushNotificationService.notifyUserAfterCommit(student, "Order cancelled", reason, "/student/orders.html");
        return response;
    }

    // ---- helpers ----

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private void assertOwnerOrStaff(Order order, User requester) {
        boolean isOwner = order.getStudent().getId().equals(requester.getId());
        boolean isStaffOrAdmin = requester.getRole().name().equals("STAFF") || requester.getRole().name().equals("ADMIN");
        if (!isOwner && !isStaffOrAdmin) {
            throw new BadRequestException("You do not have access to this order");
        }
    }

    private void validateTransition(OrderStatus from, OrderStatus to) {
        boolean valid = switch (from) {
            case PLACED -> to == OrderStatus.CONFIRMED;
            case CONFIRMED -> to == OrderStatus.PREPARING;
            case PREPARING -> to == OrderStatus.READY;
            case READY -> to == OrderStatus.PICKED_UP;
            default -> false;
        };
        if (!valid) {
            throw new BadRequestException("Cannot move an order from " + from + " to " + to);
        }
    }

    private OrderResponse toResponse(Order order) {
        boolean needsQr = order.getStatus() == OrderStatus.PLACED
                || order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.PREPARING
                || order.getStatus() == OrderStatus.READY;
        String qr = needsQr ? QrCodeUtil.generateBase64Png(order.getPickupToken(), 240) : null;
        return OrderResponse.from(order, qr);
    }

    public record OrderResult(Order order, boolean success) {
    }

}
