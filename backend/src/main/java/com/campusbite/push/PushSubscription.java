package com.campusbite.push;

import com.campusbite.common.BaseEntity;
import com.campusbite.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "push_subscriptions")
public class PushSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500, unique = true)
    private String endpoint;

    @Column(nullable = false, length = 200)
    private String p256dh;

    @Column(nullable = false, length = 200)
    private String auth;

    public PushSubscription() {
    }

    public PushSubscription(User user, String endpoint, String p256dh, String auth) {
        this.user = user;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getEndpoint() { return endpoint; }
    public String getP256dh() { return p256dh; }
    public String getAuth() { return auth; }
}