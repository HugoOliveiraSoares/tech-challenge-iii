package br.com.fiap.payment.infra.gateway.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import br.com.fiap.payment.core.domain.ProcPagRequest;
import br.com.fiap.payment.core.gateway.ProcPagGateway;
import br.com.fiap.payment.infra.gateway.http.dto.ProcPagHttpRequest;
import br.com.fiap.payment.infra.gateway.http.dto.ProcPagHttpResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProcPagHttpClientGateway implements ProcPagGateway {

    private final RestClient restClient;

    public ProcPagHttpClientGateway(@Value("${procpag.url}") String procpagUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(procpagUrl)
                .build();
    }

    @Override
    @Retry(name = "procPagRetry", fallbackMethod = "requisicaoFallback")
    public String requisicao(ProcPagRequest request) {
        return postHttpRequest(request);
    }

    private String postHttpRequest(ProcPagRequest request) {
        ProcPagHttpRequest httpRequest = new ProcPagHttpRequest(
                String.valueOf(request.getPaymentId()),
                request.getClientId(),
                request.getAmount());

        try {
            log.info("Enviando requisicao para prog pag, Cliente {}, PagamentoId {}", httpRequest.clienteId(),
                    httpRequest.pagamentoId());
            ProcPagHttpResponse response = restClient.post()
                    .uri("/requisicao")
                    .body(httpRequest)
                    .retrieve()
                    .body(ProcPagHttpResponse.class);

            if (response != null && response.status() != null) {
                return response.status();
            }
            return "";
        } catch (RestClientResponseException ex) {
            log.error("Error calling procpag: {} - {}", ex.getStatusCode(), ex.getMessage());
            throw new RuntimeException("Failed to process payment: " + ex.getStatusCode(), ex);
        }
    }

    public String requisicaoFallback(ProcPagRequest request, Exception ex) {
        log.error("Fallback triggered for procpag request: {}", ex.getMessage());
        return postHttpRequest(request);
    }
}
