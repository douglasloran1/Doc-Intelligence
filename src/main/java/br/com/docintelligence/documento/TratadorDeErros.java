package br.com.docintelligence.documento;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Toda resposta de erro carrega um codigo de motivo, nao apenas o status HTTP
 * (secao 4).
 */
@RestControllerAdvice
class TratadorDeErros {

    record ErroResponse(String codigo, String mensagem) {
    }

    @ExceptionHandler(ArquivoInvalidoException.class)
    ResponseEntity<ErroResponse> arquivoInvalido(ArquivoInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse(ex.getCodigoMotivo(), ex.getMessage()));
    }

    /**
     * O container corta o upload antes do nosso codigo quando passa do limite de
     * multipart configurado. Traduzimos para o mesmo 422 / codigo de motivo da
     * validacao de tamanho, para o cliente ver uma resposta so.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErroResponse> uploadGrandeDemais(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse("arquivo_grande", "Arquivo acima do limite de 15 MB."));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(ex.getCodigoMotivo(), ex.getMessage()));
    }

    @ExceptionHandler(ConflitoDeEstadoException.class)
    ResponseEntity<ErroResponse> conflito(ConflitoDeEstadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse(ex.getCodigoMotivo(), ex.getMessage()));
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    ResponseEntity<ErroResponse> requisicaoInvalida(RequisicaoInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse(ex.getCodigoMotivo(), ex.getMessage()));
    }
}
