package br.com.docintelligence.documento;

/**
 * Requisição malformada no nível de negócio — campo obrigatório ausente
 * ({@code motivo} da rejeição), valor de filtro desconhecido. Mapeada para HTTP
 * 422 por {@link TratadorDeErros}.
 */
public class RequisicaoInvalidaException extends RuntimeException {

    private final String codigoMotivo;

    public RequisicaoInvalidaException(String codigoMotivo, String mensagem) {
        super(mensagem);
        this.codigoMotivo = codigoMotivo;
    }

    public String getCodigoMotivo() {
        return codigoMotivo;
    }
}
