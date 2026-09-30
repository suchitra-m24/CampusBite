package com.campusbite.canteen;

import com.campusbite.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(name = "canteen_closures", uniqueConstraints = @UniqueConstraint(columnNames = "closure_date"))
public class CanteenClosure extends BaseEntity {
    @Column(name = "closure_date", nullable = false, unique = true)
    private LocalDate closureDate;

    @Column(nullable = false, length = 200)
    private String reason;

    public LocalDate getClosureDate() { return closureDate; }
    public void setClosureDate(LocalDate closureDate) { this.closureDate = closureDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
