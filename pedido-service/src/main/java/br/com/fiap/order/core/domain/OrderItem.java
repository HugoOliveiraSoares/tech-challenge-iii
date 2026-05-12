package br.com.fiap.order.core.domain;

import java.math.BigDecimal;

public class OrderItem {
    private Long productId;
    private String name;
    private Integer quantity;
    private BigDecimal price;

    public OrderItem(Long productId, String name, Integer quantity, BigDecimal price) {
        this.productId = productId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}