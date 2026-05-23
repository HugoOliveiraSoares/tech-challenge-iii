package br.com.fiap.payment.infra.gateway.db.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import br.com.fiap.payment.core.domain.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
public class PaymentEntity implements Persistable<UUID> {

    @Id
    private UUID paymentId;
    private String orderId;
    private String clientId;
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public UUID getId() {
        return paymentId;
    }

    @Override
    @Transient
    public boolean isNew() {
        return paymentId == null;
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
