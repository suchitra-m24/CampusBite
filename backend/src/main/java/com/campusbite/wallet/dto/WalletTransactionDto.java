package com.campusbite.wallet.dto;

import com.campusbite.wallet.WalletTransaction;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletTransactionDto(
        Long id, String type, BigDecimal amount, BigDecimal balanceAfter, String note, Long orderId, Instant createdAt
) {
    public static WalletTransactionDto from(WalletTransaction t) {
        return new WalletTransactionDto(t.getId(), t.getType().name(), t.getAmount(),
                t.getBalanceAfter(), t.getNote(), t.getOrderId(), t.getCreatedAt());
    }
}
