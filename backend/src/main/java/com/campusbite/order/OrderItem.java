package com.campusbite.order;

import com.campusbite.common.BaseEntity;
import com.campusbite.menu.MenuItem;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private String menuItemNameSnapshot;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @BatchSize(size = 50)
    @ElementCollection
    @CollectionTable(name = "order_item_customizations", joinColumns = @JoinColumn(name = "order_item_id"))
    @Column(name = "option_label")
    private List<String> selectedCustomizations = new ArrayList<>();

    public OrderItem() {
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public String getMenuItemNameSnapshot() {
        return menuItemNameSnapshot;
    }

    public void setMenuItemNameSnapshot(String menuItemNameSnapshot) {
        this.menuItemNameSnapshot = menuItemNameSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public List<String> getSelectedCustomizations() {
        return selectedCustomizations;
    }

    public void setSelectedCustomizations(List<String> selectedCustomizations) {
        this.selectedCustomizations = selectedCustomizations;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
