package br.com.fiap.payment.core.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcPagRequest(UUID paymentId, String clientId, BigDecimal amount) {}
