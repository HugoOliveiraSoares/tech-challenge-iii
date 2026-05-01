package br.com.fiap.payment.core.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ProcPagRequest {

    private Long paymentId;
    private String clientId;
    private BigDecimal amount;
}
