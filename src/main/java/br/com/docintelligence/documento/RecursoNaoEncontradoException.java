package br.com.docintelligence.documento;

/** Recurso inexistente. Mapeada para HTTP 404 por {@link TratadorDeErros}. */
public class RecursoNaoEncontradoException extends RuntimeException {

    private final String codigoMotivo;

    public RecursoNaoEncontradoException(String codigoMotivo, String mensagem) {
        super(mensagem);
        this.codigoMotivo = codigoMotivo;
    }

    public String getCodigoMotivo() {
        return codigoMotivo;
    }
}
