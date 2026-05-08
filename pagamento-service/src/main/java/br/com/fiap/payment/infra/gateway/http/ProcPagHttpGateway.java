package br.com.fiap.payment.infra.gateway.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import br.com.fiap.payment.core.domain.ProcPagRequest;
import br.com.fiap.payment.core.exception.ExternalServiceUnavailableException;
import br.com.fiap.payment.core.exception.PaymentProcessingException;
import br.com.fiap.payment.core.gateway.ProcPagGateway;
import br.com.fiap.payment.infra.gateway.http.dto.ProcPagHttpRequest;
import br.com.fiap.payment.infra.gateway.http.dto.ProcPagHttpResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

/**
 * Gateway HTTP para integração com o serviço externo Procpag.
 * Implementa padrão de retry com fallback para tolerância a falhas.
 */
@Slf4j
@Service
public class ProcPagHttpGateway implements ProcPagGateway {

    private final RestClient restClient;

    public ProcPagHttpGateway(RestClient.Builder restClientBuilder,
            @Value("${procpag.url}") String procpagUrl) {
        this.restClient = restClientBuilder.baseUrl(procpagUrl).build();
    }

    @Override
    @Retry(name = "procPagRetry", fallbackMethod = "requisicaoFallback")
    public String processarPagamento(ProcPagRequest request) {
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

            if (response == null) {
                throw new PaymentProcessingException("Resposta nula do Procpag");
            }
            if (response.status() == null) {
                throw new PaymentProcessingException("Status nulo na resposta do Procpag: " + response);
            }
            return response.status();

        } catch (Exception ex) {
            log.error("Error calling procpag: {}", ex.getMessage());
            throw new PaymentProcessingException("Falha no processamento do pagamento", ex);
        }
    }

    public String requisicaoFallback(ProcPagRequest request, Exception ex) {
        log.error("Fallback acionado para pagamento {}: {}", request.getPaymentId(), ex.getMessage());
        throw new ExternalServiceUnavailableException("Serviço Procpag indisponível", ex);
    }
}
