package br.com.fiap.payment.core.domain;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ProcPagRequest {

    private UUID paymentId;
    private String clientId;
    private BigDecimal amount;
}
