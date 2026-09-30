package com.campusbite.kitchenload;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Recalculates the crowd meter on a fixed cadence so it reflects reality
 * even when nobody's order status just changed (e.g. prep is simply
 * taking longer than predicted).
 */
@Component
public class KitchenLoadScheduler {

    private final KitchenLoadService kitchenLoadService;

    public KitchenLoadScheduler(KitchenLoadService kitchenLoadService) {
        this.kitchenLoadService = kitchenLoadService;
    }

    @Scheduled(initialDelay = 1000, fixedRateString = "${campusbite.kitchen-load.recalculation-fixed-rate-ms}")
    public void recalculate() {
        kitchenLoadService.recalculate();
    }
}
