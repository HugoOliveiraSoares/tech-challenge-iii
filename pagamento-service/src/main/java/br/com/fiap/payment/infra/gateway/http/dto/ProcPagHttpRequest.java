package br.com.fiap.payment.infra.gateway.http.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProcPagHttpRequest(
    @JsonProperty("pagamento_id") String pagamentoId,
    @JsonProperty("cliente_id") String clienteId,
    @JsonProperty("valor") BigDecimal valor
) {}
