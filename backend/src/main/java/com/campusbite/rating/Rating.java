package com.campusbite.rating;

import com.campusbite.common.BaseEntity;
import com.campusbite.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** One rating per order, submitted once the order has been picked up. */
@Entity
@Table(name = "ratings")
public class Rating extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false)
    private int stars; // 1-5

    @Column(length = 500)
    private String comment;

    public Rating() {
    }

    public Rating(Order order, int stars, String comment) {
        this.order = order;
        this.stars = stars;
        this.comment = comment;
    }

    public Order getOrder() {
        return order;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
