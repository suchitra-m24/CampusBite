package com.campusbite.order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.campusbite.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"student", "items", "items.menuItem"})
    List<Order> findByStudentOrderByPlacedAtDesc(User student);

    Optional<Order> findByPickupToken(String pickupToken);

    @Query("select count(o) from Order o where o.pickupSlotStart = :slotStart " +
           "and o.status not in (com.campusbite.order.OrderStatus.CANCELLED, com.campusbite.order.OrderStatus.NO_SHOW)")
    long countActiveOrdersInSlot(@Param("slotStart") Instant slotStart);

    @EntityGraph(attributePaths = {"student", "items", "items.menuItem"})
    List<Order> findByStatusInOrderByPickupSlotStartAsc(List<OrderStatus> statuses);

    @Query("select o from Order o where o.status in :statuses and o.pickupSlotStart between :from and :to")
    List<Order> findActiveInWindow(@Param("statuses") List<OrderStatus> statuses,
                                    @Param("from") Instant from, @Param("to") Instant to);

    long countByStatusIn(List<OrderStatus> statuses);

    @EntityGraph(attributePaths = {"student", "items", "items.menuItem"})
    List<Order> findByStatusAndPickupSlotEndBefore(OrderStatus status, Instant cutoff);

    List<Order> findByStatusInAndPickupSlotEndBefore(List<OrderStatus> statuses, Instant cutoff);

    List<Order> findTop50ByActualPrepMinutesIsNotNullOrderByPlacedAtDesc();

    List<Order> findByPlacedAtBetween(Instant from, Instant to);

    @Query("select o from Order o where " +
           "str(o.id) = :query or lower(o.pickupToken) like lower(concat('%', :query, '%')) or " +
           "lower(o.student.fullName) like lower(concat('%', :query, '%')) or " +
           "lower(o.student.email) like lower(concat('%', :query, '%')) or " +
           "lower(o.student.loginId) like lower(concat('%', :query, '%')) " +
           "order by o.placedAt desc")
    Page<Order> searchOrders(@Param("query") String query, Pageable pageable);

    @Query("select o from Order o where o.status in :statuses and (" +
           "str(o.id) = :query or lower(o.pickupToken) like lower(concat('%', :query, '%')) or " +
           "lower(o.student.fullName) like lower(concat('%', :query, '%')) or " +
           "lower(o.student.loginId) like lower(concat('%', :query, '%'))) " +
           "order by o.placedAt desc")
    List<Order> searchByStatusesAndQuery(@Param("statuses") List<OrderStatus> statuses, @Param("query") String query);

    List<Order> findTop100ByStatusInOrderByPlacedAtDesc(List<OrderStatus> statuses);

    Page<Order> findByStudentOrderByPlacedAtDesc(User student, Pageable pageable);

    @Query("select o from Order o where o.status in :statuses and (" +
            "str(o.id) = :query or lower(o.pickupToken) like lower(concat('%', :query, '%')) or " +
            "lower(o.student.fullName) like lower(concat('%', :query, '%')) or " +
            "lower(o.student.loginId) like lower(concat('%', :query, '%')))")
    Page<Order> searchByStatusesAndQuery(@Param("statuses") List<OrderStatus> statuses,
                                         @Param("query") String query, Pageable pageable);

    Page<Order> findByStatusInOrderByPlacedAtDesc(List<OrderStatus> statuses, Pageable pageable);
}
