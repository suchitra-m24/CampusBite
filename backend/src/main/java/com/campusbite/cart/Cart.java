package com.campusbite.cart;

import com.campusbite.common.BaseEntity;
import com.campusbite.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/** One active cart per student. Cleared out once the order is placed. */
@Entity
@Table(name = "carts")
public class Cart extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private User student;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public Cart() {
    }

    public Cart(User student) {
        this.student = student;
    }

    public User getStudent() {
        return student;
    }

    public List<CartItem> getItems() {
        return items;
    }
}
