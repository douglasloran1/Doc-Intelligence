package br.com.docintelligence.documento;

/**
 * Arquivo recusado na fronteira da API, antes de entrar na fila (secao 4;
 * restricao de ambiente b). Mapeada para HTTP 422 com codigo de motivo por
 * {@link TratadorDeErros}.
 */
public class ArquivoInvalidoException extends RuntimeException {

    private final String codigoMotivo;

    public ArquivoInvalidoException(String codigoMotivo, String mensagem) {
        super(mensagem);
        this.codigoMotivo = codigoMotivo;
    }

    public String getCodigoMotivo() {
        return codigoMotivo;
    }
}
