package com.campusbite.analytics;

import com.campusbite.analytics.dto.AnalyticsSummaryDto;
import com.campusbite.analytics.dto.BestSellerDto;
import com.campusbite.analytics.dto.PeakHourDto;
import com.campusbite.analytics.dto.SlotUtilizationDto;
import com.campusbite.kitchenload.KitchenLoadSnapshot;
import com.campusbite.kitchenload.KitchenLoadSnapshotRepository;
import com.campusbite.slot.SlotCapacityRepository;
import com.campusbite.slot.SlotCapacity;
import com.campusbite.order.Order;
import com.campusbite.order.OrderItemRepository;
import com.campusbite.order.OrderRepository;
import com.campusbite.order.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KitchenLoadSnapshotRepository kitchenLoadSnapshotRepository;
    private final ZoneId campusZone;
    private final SlotCapacityRepository slotCapacityRepository;
    private final int defaultCapacityPerWindow;

    public AnalyticsService(OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             KitchenLoadSnapshotRepository kitchenLoadSnapshotRepository,
                             SlotCapacityRepository slotCapacityRepository,
                             @Value("${campusbite.slot.default-capacity-per-window}") int defaultCapacityPerWindow,
                             @Value("${campusbite.time-zone:Asia/Kolkata}") String timeZone) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.kitchenLoadSnapshotRepository = kitchenLoadSnapshotRepository;
        this.slotCapacityRepository = slotCapacityRepository;
        this.defaultCapacityPerWindow = defaultCapacityPerWindow;
        this.campusZone = ZoneId.of(timeZone);
    }

    public List<BestSellerDto> bestSellers(int limit) {
        return orderItemRepository.findBestSellers().stream()
                .limit(limit)
                .map(row -> new BestSellerDto(row.getName(), row.getTotalQty()))
                .toList();
    }

    /** Average kitchen-load percentage by hour of day, from recorded snapshots -- powers the "peak hours" chart. */
    public List<PeakHourDto> peakHours() {
        ZoneId zone = campusZone;
        Instant from = Instant.now().minusSeconds(30L * 24 * 3600); // last 30 days of snapshots
        Instant to = Instant.now();
        List<KitchenLoadSnapshot> snapshots = kitchenLoadSnapshotRepository.findBetween(from, to);

        Map<Integer, List<Integer>> byHour = new TreeMap<>();
        for (KitchenLoadSnapshot s : snapshots) {
            int hour = s.getCreatedAt().atZone(zone).getHour();
            byHour.computeIfAbsent(hour, h -> new java.util.ArrayList<>()).add(s.getLoadPercentage());
        }

        return byHour.entrySet().stream()
                .map(e -> new PeakHourDto(
                        e.getKey(),
                        e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0),
                        e.getValue().size()))
                .sorted(Comparator.comparingInt(PeakHourDto::hourOfDay))
                .toList();
    }

    /** How full each recent pickup slot got vs its capacity -- surfaces where the admin should raise/lower capacity. */
    public List<SlotUtilizationDto> slotUtilization() {
        ZoneId zone = campusZone;
        Instant from = Instant.now().minusSeconds(24L * 3600);
        Instant to = Instant.now().plusSeconds(24L * 3600);

        List<Order> orders = orderRepository.findActiveInWindow(
                List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                        OrderStatus.READY, OrderStatus.PICKED_UP),
                from, to);

        Map<Instant, Long> bySlot = orders.stream()
                .collect(Collectors.groupingBy(Order::getPickupSlotStart, Collectors.counting()));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE HH:mm");

        return bySlot.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new SlotUtilizationDto(
                        ZonedDateTime.ofInstant(e.getKey(), zone).format(fmt),
                        e.getValue().intValue(),
                        capacityFor(e.getKey(), zone)))
                .toList();
    }

    private int capacityFor(Instant slotStart, ZoneId zone) {
        int minuteOfDay = slotStart.atZone(zone).getHour() * 60 + slotStart.atZone(zone).getMinute();
        return slotCapacityRepository.findByWindowStartMinuteOfDay(minuteOfDay)
                .map(SlotCapacity::getMaxOrders)
                .orElse(defaultCapacityPerWindow);
    }

    public AnalyticsSummaryDto summary() {
        ZoneId zone = campusZone;
        Instant startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        Instant endOfDay = Instant.now();

        List<Order> todayOrders = orderRepository.findByPlacedAtBetween(startOfDay, endOfDay);

        long total = todayOrders.size();
        BigDecimal revenue = todayOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long noShows = todayOrders.stream().filter(o -> o.getStatus() == OrderStatus.NO_SHOW).count();
        long cancelled = todayOrders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

        double avgAccuracy = todayOrders.stream()
                .filter(o -> o.getActualPrepMinutes() != null && o.getEstimatedPrepMinutes() > 0)
                .mapToDouble(o -> 100.0 - Math.min(100, Math.abs(o.getActualPrepMinutes() - o.getEstimatedPrepMinutes()) * 100.0 / o.getEstimatedPrepMinutes()))
                .average()
                .orElse(100.0);

        return new AnalyticsSummaryDto(total, revenue, avgAccuracy, noShows, cancelled);
    }
}
