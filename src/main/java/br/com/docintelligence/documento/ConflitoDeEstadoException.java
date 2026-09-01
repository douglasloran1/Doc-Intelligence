package br.com.docintelligence.documento;

/**
 * Operação incompatível com o estado atual do documento ou com a reivindicação
 * ativa — por exemplo reivindicar um documento que não está em
 * {@code aguardando_conferencia}, ou corrigir sem ser o operador dono da
 * reivindicação (secao 4). Mapeada para HTTP 409 por {@link TratadorDeErros}.
 */
public class ConflitoDeEstadoException extends RuntimeException {

    private final String codigoMotivo;

    public ConflitoDeEstadoException(String codigoMotivo, String mensagem) {
        super(mensagem);
        this.codigoMotivo = codigoMotivo;
    }

    public String getCodigoMotivo() {
        return codigoMotivo;
    }
}
