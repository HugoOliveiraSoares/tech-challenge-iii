package br.com.fiap.payment.core.usecase;

public interface ConfirmPaymentUseCase {
    void execute(String paymentId);
}