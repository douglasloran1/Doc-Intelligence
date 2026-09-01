package br.com.docintelligence.documento;

/**
 * Falha na chamada ao adaptador de extração — timeout ou erro do fornecedor
 * (§6). O worker trata como temporária: retry com backoff até o limite de
 * tentativas, depois {@code falha_definitiva}.
 *
 * <p>O dublê desta entrega não lança esta exceção; ela existe para a
 * implementação real e para exercitar o caminho de retry nos testes.
 */
public class FalhaExtracaoException extends RuntimeException {

    public FalhaExtracaoException(String mensagem) {
        super(mensagem);
    }

    public FalhaExtracaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
