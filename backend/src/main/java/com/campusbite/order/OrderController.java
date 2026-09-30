package com.campusbite.order;
import org.springframework.web.bind.annotation.RequestHeader;
import com.campusbite.common.security.SecurityUser;
import com.campusbite.order.dto.OrderResponse;
import com.campusbite.order.dto.CancelOrderRequest;
import com.campusbite.order.dto.PlaceOrderRequest;
import com.campusbite.order.dto.ScanTokenRequest;
import com.campusbite.order.dto.StatusUpdateRequest;
import com.campusbite.order.dto.UpdateOrderItemsRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;

    private final IdempotencyService idempotencyService;

    public OrderController(OrderService orderService, IdempotencyService idempotencyService) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
    }
    // ---- Student ----

    @PostMapping("/api/student/orders")
    public ResponseEntity<OrderResponse> placeOrder(@AuthenticationPrincipal SecurityUser principal,
                                                    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                    @Valid @RequestBody(required = false) PlaceOrderRequest request) {
        String notes = request != null ? request.specialInstructions() : null;

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            OrderService.OrderResult result = orderService.placeOrder(principal.getUser(), notes);
            return ResponseEntity.ok(orderService.getOrder(result.order().getId(), principal.getUser()));
        }
        idempotencyKey = idempotencyKey.trim();
        if (idempotencyKey.length() > 200) {
            throw new com.campusbite.common.exception.BadRequestException("Idempotency-Key must be at most 200 characters");
        }

        String requestHash = idempotencyService.hashRequest(notes);
        IdempotencyService.ClaimOutcome outcome =
                idempotencyService.claim(idempotencyKey, principal.getUser().getId(), requestHash);

        if (outcome.alreadyCompleted()) {
            // Replay of a request we already fully processed -- return the ORIGINAL order,
            // don't create a second one.
            return ResponseEntity.ok(orderService.getOrder(outcome.existingOrderId(), principal.getUser()));
        }

        OrderService.OrderResult result;
        try {
            result = orderService.placeOrder(principal.getUser(), notes);
            idempotencyService.recordResult(outcome.recordId(), result.order().getId());
        } catch (RuntimeException ex) {
            // The claim lives in a separate transaction, so release it if order creation fails;
            // otherwise a transient failure would block the same legitimate request for 24 hours.
            idempotencyService.release(outcome.recordId());
            throw ex;
        }
        // Keep response rendering outside the claim-release block: once the claim has been
        // marked complete, a response serialization/read failure must not make the same key reusable.
        return ResponseEntity.ok(orderService.getOrder(result.order().getId(), principal.getUser()));
    }

    @GetMapping("/api/student/orders")
    public ResponseEntity<List<OrderResponse>> history(@AuthenticationPrincipal SecurityUser principal) {
        // Unpaged -- the student orders page renders the full active-order tickets
        // plus history together and expects everything at once; kept as-is.
        return ResponseEntity.ok(orderService.getOrderHistory(principal.getUser()));
    }

    @GetMapping("/api/student/orders/paged")
    public ResponseEntity<Page<OrderResponse>> historyPaged(@AuthenticationPrincipal SecurityUser principal,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getOrderHistoryPaged(principal.getUser(), page, size));
    }

    @GetMapping("/api/student/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOne(@AuthenticationPrincipal SecurityUser principal,
                                                  @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId, principal.getUser()));
    }

    @PostMapping("/api/student/orders/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancel(@AuthenticationPrincipal SecurityUser principal,
                                                  @PathVariable Long orderId,
                                                  @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return ResponseEntity.ok(orderService.cancel(orderId, principal.getUser(), request != null ? request.reason() : null));
    }

    @PatchMapping("/api/student/orders/{orderId}/items")
    public ResponseEntity<OrderResponse> updateItems(@AuthenticationPrincipal SecurityUser principal,
                                                        @PathVariable Long orderId,
                                                        @Valid @RequestBody UpdateOrderItemsRequest request) {
        return ResponseEntity.ok(orderService.updateItems(orderId, principal.getUser(), request));
    }

    // ---- Staff ----

    @GetMapping("/api/staff/orders/queue")
    public ResponseEntity<List<OrderResponse>> kitchenQueue() {
        return ResponseEntity.ok(orderService.getKitchenQueue());
    }

    @GetMapping("/api/staff/orders/history")
    public ResponseEntity<Page<OrderResponse>> staffHistory(@RequestParam(required = false) String query,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.staffOrderHistory(query, page, size));
    }

    @PatchMapping("/api/staff/orders/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long orderId,
                                                         @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.advanceStatus(orderId, request.status()));
    }

    @PostMapping("/api/staff/orders/scan")
    public ResponseEntity<OrderResponse> scan(@Valid @RequestBody ScanTokenRequest request) {
        return ResponseEntity.ok(orderService.completeByToken(request.pickupToken()));
    }

    // ---- Admin ----

    @GetMapping("/api/admin/orders/search")
    public ResponseEntity<List<OrderResponse>> adminSearch(@RequestParam String query) {
        return ResponseEntity.ok(orderService.searchOrders(query));
    }
}
