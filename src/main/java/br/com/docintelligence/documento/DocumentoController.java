package br.com.docintelligence.documento;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoint de recebimento de documentos (secao 4). Responde na hora; o
 * processamento acontece fora do ciclo da requisicao nas proximas partes.
 */
@RestController
@RequestMapping("/documentos")
public class DocumentoController {

    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DocumentoResponse> receber(@RequestParam("arquivo") MultipartFile arquivo) {
        DocumentoService.ResultadoCriacao resultado = service.criar(arquivo);

        // 201 quando o documento e criado agora; 200 quando o hash ja existia e
        // devolvemos o registro existente (idempotencia, secao 4).
        HttpStatus status = resultado.criado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(DocumentoResponse.de(resultado.documento()));
    }
}
