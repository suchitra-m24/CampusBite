package com.campusbite.order;

import com.campusbite.menu.MenuItem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Predicts how long a cart will take the kitchen to prepare.
 *
 * Two ingredients feed the estimate:
 *  1. A per-cart base time derived from each item's baseline prep time
 *     (kitchen items are usually cooked in parallel, so we take the max
 *     of the distinct items rather than summing them, plus a small
 *     per-extra-unit buffer for volume).
 *  2. A global "how accurate have we been lately" adjustment factor,
 *     computed from actual-vs-predicted prep time on the most recent
 *     completed orders (see recordActualAndGetAdjustmentFactor). This is
 *     the feedback loop that lets slot allocation improve over time
 *     instead of relying on static assumptions forever.
 */
@Service
public class PrepTimePredictionService {

    private final OrderRepository orderRepository;

    private static final double MIN_FACTOR = 0.85;
    private static final double MAX_FACTOR = 1.5;

    public PrepTimePredictionService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public int estimateForCart(Map<MenuItem, Integer> itemsWithQuantity) {
        int baseMinutes = itemsWithQuantity.keySet().stream()
                .mapToInt(MenuItem::getBaselinePrepTimeMinutes)
                .max()
                .orElse(8);

        int totalUnits = itemsWithQuantity.values().stream().mapToInt(Integer::intValue).sum();
        int volumeBuffer = Math.max(0, totalUnits - 1) * 2;

        double adjustmentFactor = currentAdjustmentFactor();

        int estimate = (int) Math.round((baseMinutes + volumeBuffer) * adjustmentFactor);
        return Math.max(3, estimate);
    }

    /**
     * Average ratio of actual-to-predicted prep time across the most
     * recent completed orders, clamped to a sane range. 1.0 means the
     * kitchen has been running right on the baseline; > 1.0 means it's
     * consistently taking longer than predicted (so future estimates,
     * and therefore slot allocation, lean more conservative).
     */
    public double currentAdjustmentFactor() {
        List<Order> recent = orderRepository.findTop50ByActualPrepMinutesIsNotNullOrderByPlacedAtDesc();
        if (recent.size() < 5) {
            return 1.0; // not enough history yet -- trust the static baselines
        }

        double avgRatio = recent.stream()
                .filter(o -> o.getEstimatedPrepMinutes() > 0 && o.getActualPrepMinutes() != null)
                .mapToDouble(o -> (double) o.getActualPrepMinutes() / o.getEstimatedPrepMinutes())
                .average()
                .orElse(1.0);

        return Math.min(MAX_FACTOR, Math.max(MIN_FACTOR, avgRatio));
    }
}
