package com.campusbite.user;

import com.campusbite.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "login_id")
})
public class User extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * The identifier used to log in: a student's USN, or a free-text staff/
     * admin ID. Unique across all accounts regardless of role. Email is
     * kept for verification/password-reset only -- it is no longer used
     * to authenticate.
     */
    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(length = 100)
    private String department;

    @Column(name = "academic_year", length = 30)
    private String academicYear;

    @Column(length = 30)
    private String section;

    public User() {
    }

    public User(String fullName, String email, String loginId, String passwordHash, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode = PaymentMode.WALLET;

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }
}
