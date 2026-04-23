package br.com.fiap.payment.core.domain;

import java.math.BigDecimal;

public record OrderItem(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice) {
}
