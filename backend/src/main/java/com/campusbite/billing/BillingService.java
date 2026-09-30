package com.campusbite.billing;

import com.campusbite.billing.dto.CurrentBillingDto;
import com.campusbite.billing.dto.MonthlyStatementDto;
import com.campusbite.common.exception.ResourceNotFoundException;
import com.campusbite.order.Order;
import com.campusbite.order.OrderRepository;
import com.campusbite.order.OrderStatus;
import com.campusbite.user.PaymentMode;
import com.campusbite.user.User;
import com.campusbite.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors how real campus cafeterias bill staff (payroll deduction) rather
 * than instant payment. POSTPAID users' orders aren't debited at all when
 * placed -- their cost just accumulates, and once a calendar month closes,
 * a statement is generated summarizing what they owe. Admin marks it
 * "settled" once the actual deduction/collection happens outside the app.
 */
@Service
public class BillingService {

    private static final List<OrderStatus> BILLABLE_STATUSES = List.of(
            OrderStatus.CONFIRMED, OrderStatus.PREPARING,
            OrderStatus.READY, OrderStatus.PICKED_UP);

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MonthlyStatementRepository statementRepository;
    private final ZoneId campusZone;

    public BillingService(UserRepository userRepository, OrderRepository orderRepository,
                          MonthlyStatementRepository statementRepository,
                          @Value("${campusbite.time-zone:Asia/Kolkata}") String timeZone) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.statementRepository = statementRepository;
        this.campusZone = ZoneId.of(timeZone);
    }

    @Transactional
    public List<MonthlyStatementDto> generateStatementsFor(int year, int month) {
        ZoneId zone = campusZone;
        YearMonth ym = YearMonth.of(year, month);
        Instant from = ym.atDay(1).atStartOfDay(zone).toInstant();
        Instant to = ym.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant();

        List<User> postpaidUsers = userRepository.findAll().stream()
                .filter(u -> u.getPaymentMode() == PaymentMode.POSTPAID)
                .toList();

        List<MonthlyStatementDto> results = new ArrayList<>();
        for (User user : postpaidUsers) {
            if (statementRepository.existsByUserAndYearAndMonth(user, year, month)) {
                continue; // idempotent -- running this twice for the same month is a no-op
            }
            List<Order> orders = orderRepository.findByStudentOrderByPlacedAtDesc(user).stream()
                    .filter(o -> BILLABLE_STATUSES.contains(o.getStatus()))
                    .filter(o -> !o.getPlacedAt().isBefore(from) && o.getPlacedAt().isBefore(to))
                    .toList();

            if (orders.isEmpty()) continue;

            BigDecimal total = orders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            MonthlyStatement saved = statementRepository.save(
                    new MonthlyStatement(user, year, month, orders.size(), total));
            results.add(MonthlyStatementDto.from(saved));
        }
        return results;
    }

    public CurrentBillingDto getCurrentBillingFor(User user) {
        ZoneId zone = campusZone;
        YearMonth ym = YearMonth.now(zone);
        Instant from = ym.atDay(1).atStartOfDay(zone).toInstant();
        Instant to = Instant.now();

        List<Order> orders = orderRepository.findByStudentOrderByPlacedAtDesc(user).stream()
                .filter(o -> BILLABLE_STATUSES.contains(o.getStatus()))
                .filter(o -> !o.getPlacedAt().isBefore(from) && !o.getPlacedAt().isAfter(to))
                .toList();
        BigDecimal total = orders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CurrentBillingDto(ym.getYear(), ym.getMonthValue(), orders.size(), total);
    }

    public List<MonthlyStatementDto> getStatementsFor(User user) {
        return statementRepository.findByUserOrderByYearDescMonthDesc(user).stream()
                .map(MonthlyStatementDto::from).toList();
    }

    public List<MonthlyStatementDto> getUnsettledStatements() {
        return statementRepository.findBySettledFalseOrderByYearAscMonthAsc().stream()
                .map(MonthlyStatementDto::from).toList();
    }

    @Transactional
    public MonthlyStatementDto settle(Long statementId) {
        MonthlyStatement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new ResourceNotFoundException("Statement not found"));
        if (statement.isSettled()) {
            return MonthlyStatementDto.from(statement);
        }
        statement.setSettled(true);
        statement.setSettledAt(Instant.now());
        return MonthlyStatementDto.from(statementRepository.save(statement));
    }
}