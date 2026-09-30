package com.campusbite.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("select oi.menuItemNameSnapshot as name, sum(oi.quantity) as totalQty " +
           "from OrderItem oi where oi.order.status <> com.campusbite.order.OrderStatus.CANCELLED " +
           "group by oi.menuItemNameSnapshot order by sum(oi.quantity) desc")
    List<BestSellerRow> findBestSellers();

    interface BestSellerRow {
        String getName();
        Long getTotalQty();
    }
}
