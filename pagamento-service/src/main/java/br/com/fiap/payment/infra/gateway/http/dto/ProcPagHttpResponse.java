package br.com.fiap.payment.infra.gateway.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProcPagHttpResponse(
        @JsonProperty("status") String status) {
}
