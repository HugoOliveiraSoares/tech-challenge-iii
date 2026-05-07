package br.com.fiap.payment.infra.gateway.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProcPagHttpResponse(
    @JsonProperty("pagamento_id") String pagamentoId,
    @JsonProperty("status") String status
) {}
