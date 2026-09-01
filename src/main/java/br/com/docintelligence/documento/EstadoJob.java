package br.com.docintelligence.documento;

/**
 * Estado de um item da fila de processamento (ADR 0003 — tabela de jobs no
 * próprio Postgres).
 *
 * <ul>
 *   <li>{@code PENDENTE} — à espera de um worker; elegível quando
 *       {@code proxima_tentativa_em} é nulo ou já passou.</li>
 *   <li>{@code EM_EXECUCAO} — reivindicado por um worker, com lease; elegível de
 *       novo se o lease expirar (worker morreu no meio, §6).</li>
 *   <li>{@code CONCLUIDO} — documento chegou a {@code pronto} ou
 *       {@code aguardando_conferencia}.</li>
 *   <li>{@code FALHOU} — documento em {@code falha_definitiva} após esgotar as
 *       tentativas.</li>
 * </ul>
 */
public enum EstadoJob {
    PENDENTE,
    EM_EXECUCAO,
    CONCLUIDO,
    FALHOU
}
