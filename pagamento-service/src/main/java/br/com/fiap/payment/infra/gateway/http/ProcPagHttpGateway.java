package br.com.fiap.payment.infra.gateway.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import br.com.fiap.payment.core.domain.ProcPagRequest;
import br.com.fiap.payment.core.exception.ExternalServiceUnavailableException;
import br.com.fiap.payment.core.exception.PaymentProcessingException;
import br.com.fiap.payment.core.gateway.ProcPagGateway;
import br.com.fiap.payment.infra.gateway.http.dto.ProcPagHttpRequest;
import br.com.fiap.payment.infra.gateway.http.dto.ProcPagHttpResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.net.http.HttpClient;
import java.time.Duration;

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
            @Value("${procpag.url}") String procpagUrl,
            @Value("${procpag.connect-timeout}") Duration connectTimeout,
            @Value("${procpag.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = restClientBuilder
                .baseUrl(procpagUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @CircuitBreaker(name = "procPagCircuitBreaker", fallbackMethod = "requisicaoFallback")
    @Retry(name = "procPagRetry", fallbackMethod = "requisicaoFallback")
    public String processPayment(ProcPagRequest request) {
        return postHttpRequest(request);
    }

    private String postHttpRequest(ProcPagRequest request) {
        ProcPagHttpRequest httpRequest = new ProcPagHttpRequest(
                String.valueOf(request.paymentId()),
                request.clientId(),
                request.amount().longValue());

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

        } catch (HttpServerErrorException | ResourceAccessException ex) {
            log.error("Erro na comunicação com Procpag: {}", ex.getMessage());
            throw new ExternalServiceUnavailableException("Procpag indisponível", ex);
        } catch (Exception ex) {
            log.error("Erro inesperado na chamada Procpag", ex);
            throw new PaymentProcessingException("Falha inesperada no processamento do pagamento", ex);
        }
    }

    public String requisicaoFallback(ProcPagRequest request, Exception ex) {
        log.warn("Fallback ativado: assumindo PENDING para pagamento {}", request.paymentId(), ex);
        return "PENDING";
    }
}
