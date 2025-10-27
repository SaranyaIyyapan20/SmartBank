package com.smartbank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "paypal_users")
public class PayPalUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    // Getters and setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}

