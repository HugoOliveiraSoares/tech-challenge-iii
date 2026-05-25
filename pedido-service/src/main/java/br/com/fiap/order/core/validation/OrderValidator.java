package br.com.fiap.order.core.validation;

import br.com.fiap.order.core.domain.Order;
import br.com.fiap.order.core.domain.OrderItem;
import br.com.fiap.order.core.domain.OrderStatus;
import br.com.fiap.order.core.exception.InvalidOrderException;
import br.com.fiap.order.core.exception.UnauthorizedCustomerException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class OrderValidator {

    public static final int MAX_ITEMS_PER_ORDER = 50;
    public static final int MAX_ITEM_QUANTITY = 999;
    public static final int MAX_TEXT_LENGTH = 255;
    public static final int MAX_PRICE_SCALE = 2;
    public static final BigDecimal MAX_UNIT_PRICE = new BigDecimal("999999.99");

    private OrderValidator() {
    }

    public static void validateCustomerId(UUID customerId) {
        if (customerId == null) {
            throw new InvalidOrderException("Customer id is required");
        }
    }

    public static void validateOrderId(UUID orderId) {
        if (orderId == null) {
            throw new InvalidOrderException("Order id is required");
        }
    }

    public static void validateCreateOrder(UUID customerId, String restaurantId, List<OrderItem> items) {
        validateCustomerId(customerId);
        validateRestaurantId(restaurantId);
        validateItems(items);
    }

    public static String normalizeRestaurantId(String restaurantId) {
        validateRestaurantId(restaurantId);
        return restaurantId.trim();
    }

    public static List<OrderItem> normalizeItems(List<OrderItem> items) {
        validateItems(items);
        return items.stream()
                .map(item -> new OrderItem(
                        item.getProductId(),
                        item.getName().trim(),
                        item.getQuantity(),
                        item.getPrice()))
                .toList();
    }

    public static void validateRestaurantId(String restaurantId) {
        if (restaurantId == null || restaurantId.isBlank()) {
            throw new InvalidOrderException("Restaurant id is required");
        }
        String trimmed = restaurantId.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new InvalidOrderException(
                    "Restaurant id must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
    }

    public static void validateItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }
        if (items.size() > MAX_ITEMS_PER_ORDER) {
            throw new InvalidOrderException(
                    "Order must not exceed " + MAX_ITEMS_PER_ORDER + " items");
        }

        Set<Long> productIds = new HashSet<>();
        for (int index = 0; index < items.size(); index++) {
            OrderItem item = items.get(index);
            if (item == null) {
                throw new InvalidOrderException("Item at position " + index + " is required");
            }
            validateItem(item, index);
            if (!productIds.add(item.getProductId())) {
                throw new InvalidOrderException(
                        "Duplicate product id in order: " + item.getProductId());
            }
        }
    }

    private static void validateItem(OrderItem item, int index) {
        String position = "Item at position " + index;

        if (item.getProductId() == null || item.getProductId() <= 0) {
            throw new InvalidOrderException(position + ": product id must be a positive number");
        }
        if (item.getName() == null || item.getName().isBlank()) {
            throw new InvalidOrderException(position + ": product name is required");
        }
        if (item.getName().trim().length() > MAX_TEXT_LENGTH) {
            throw new InvalidOrderException(
                    position + ": product name must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new InvalidOrderException(position + ": quantity must be greater than zero");
        }
        if (item.getQuantity() > MAX_ITEM_QUANTITY) {
            throw new InvalidOrderException(
                    position + ": quantity must not exceed " + MAX_ITEM_QUANTITY);
        }
        validatePrice(item.getPrice(), position);
    }

    private static void validatePrice(BigDecimal price, String context) {
        if (price == null) {
            throw new InvalidOrderException(context + ": price is required");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException(context + ": price must be greater than zero");
        }
        if (price.compareTo(MAX_UNIT_PRICE) > 0) {
            throw new InvalidOrderException(context + ": price exceeds maximum allowed value");
        }
        if (price.scale() > MAX_PRICE_SCALE) {
            throw new InvalidOrderException(context + ": price must have at most " + MAX_PRICE_SCALE + " decimal places");
        }
    }

    public static boolean canTransitionToPaid(OrderStatus currentStatus) {
        return currentStatus == OrderStatus.CREATED || currentStatus == OrderStatus.PENDING_PAYMENT;
    }

    public static boolean canTransitionToPendingPayment(OrderStatus currentStatus) {
        return currentStatus == OrderStatus.CREATED;
    }

    public static void validateOwnership(Order order, UUID customerId) {
        validateCustomerId(customerId);
        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedCustomerException();
        }
    }
}
