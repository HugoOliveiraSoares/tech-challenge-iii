package br.com.fiap.payment.infra.gateway.db.entity;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;

import br.com.fiap.payment.core.domain.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;
    private String orderId;
    private String clientId;
    private BigDecimal totalAmount;
    @Enumerated
    private PaymentStatus paymentStatus;

}
