package br.com.fiap.payment.core.domain;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PaymentMapperUtil {

    private PaymentMapperUtil() {}

    public static PaymentStatus mapProcpagStatus(String procpagStatus) {
        return switch (procpagStatus.toUpperCase()) {
            case "ACCEPTED" -> PaymentStatus.APPROVED;
            case "PENDING" -> PaymentStatus.PENDING;
            default -> {
                log.warn("Status desconhecido Procpag: {}, assumindo PENDING", procpagStatus);
                yield PaymentStatus.PENDING;
            }
        };
    }

    public static PaymentEvent buildPaymentEvent(Payment payment) {
        return new PaymentEvent(
                payment.getOrderId(),
                payment.getPaymentId().toString(),
                payment.getTotalAmount(),
                LocalDateTime.now());
    }
}
