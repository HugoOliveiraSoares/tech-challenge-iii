/**
 * Camada de infraestrutura: adaptadores para mundo externo.
 * <ul>
 *   <li>{@code controller} — API REST e DTOs</li>
 *   <li>{@code config} — beans Spring (security, use cases, kafka)</li>
 *   <li>{@code gateway.db} — persistência PostgreSQL (JPA)</li>
 *   <li>{@code gateway.kafka} — mensageria com pagamento-service</li>
 *   <li>{@code gateway.security} — leitura do JWT</li>
 * </ul>
 */
package br.com.fiap.order.infra;
