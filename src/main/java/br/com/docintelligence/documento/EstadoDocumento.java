package br.com.docintelligence.documento;

/**
 * Estados do ciclo de vida de um documento, conforme o diagrama da secao 3 da
 * especificacao (docs/01-especificacao.md).
 *
 * <p>Terminais: {@link #PRONTO}, {@link #CONCLUIDO}, {@link #FALHA_DEFINITIVA},
 * {@link #REJEITADO}. Nesta fatia vertical so o caminho ate {@link #RECEBIDO} e
 * exercitado pelo endpoint; as demais transicoes entram nas proximas partes.
 */
public enum EstadoDocumento {

    RECEBIDO,
    EM_PROCESSAMENTO,
    FALHA_TEMPORARIA,
    FALHA_DEFINITIVA,
    PRONTO,
    AGUARDANDO_CONFERENCIA,
    EM_CONFERENCIA,
    CONCLUIDO,
    REJEITADO
}
