package br.com.fiap.payment.core.gateway;

import br.com.fiap.payment.core.domain.ProcPagRequest;

public interface ProcPagGateway {

    String processarPagamento(ProcPagRequest request);

}
