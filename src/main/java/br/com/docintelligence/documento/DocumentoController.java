package br.com.docintelligence.documento;

import br.com.docintelligence.documento.RequisicoesConferencia.CorrecaoRequest;
import br.com.docintelligence.documento.RequisicoesConferencia.ReivindicarRequest;
import br.com.docintelligence.documento.RequisicoesConferencia.RejeitarRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * API HTTP do serviço (secao 4). Traduz HTTP ↔ domínio; não conhece extração,
 * fila nem persistência por dentro.
 */
@RestController
@RequestMapping("/documentos")
public class DocumentoController {

    /** Tamanho de página padrão da listagem (secao 4 não fixa o valor). */
    static final int TAMANHO_PAGINA_PADRAO = 20;

    private final DocumentoService documentos;
    private final ConferenciaService conferencia;

    public DocumentoController(DocumentoService documentos, ConferenciaService conferencia) {
        this.documentos = documentos;
        this.conferencia = conferencia;
    }

    @PostMapping
    public ResponseEntity<DocumentoResponse> receber(@RequestParam("arquivo") MultipartFile arquivo) {
        DocumentoService.ResultadoCriacao resultado = documentos.criar(arquivo);

        // 201 quando o documento e criado agora; 200 quando o hash ja existia e
        // devolvemos o registro existente (idempotencia, secao 4).
        HttpStatus status = resultado.criado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(DocumentoResponse.de(resultado.documento()));
    }

    @GetMapping("/{id}")
    public DocumentoDetalhadoResponse detalhar(@PathVariable UUID id) {
        return documentos.detalhar(id);
    }

    @GetMapping
    public PaginaResponse<DocumentoResumoResponse> listar(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "" + TAMANHO_PAGINA_PADRAO) int tamanho) {
        return documentos.listar(status, tipo, pagina, tamanho);
    }

    @PostMapping("/{id}/reivindicar")
    public DocumentoDetalhadoResponse reivindicar(@PathVariable UUID id,
                                                  @RequestBody ReivindicarRequest corpo) {
        return detalharDe(conferencia.reivindicar(id, corpo.operador()).getId());
    }

    @PatchMapping("/{id}")
    public DocumentoDetalhadoResponse corrigir(@PathVariable UUID id,
                                               @RequestBody CorrecaoRequest corpo) {
        return detalharDe(conferencia.corrigir(id, corpo.operador(), corpo.campos()).getId());
    }

    @PostMapping("/{id}/rejeitar")
    public DocumentoDetalhadoResponse rejeitar(@PathVariable UUID id,
                                               @RequestBody RejeitarRequest corpo) {
        return detalharDe(conferencia.rejeitar(id, corpo.operador(), corpo.motivo()).getId());
    }

    private DocumentoDetalhadoResponse detalharDe(UUID id) {
        return documentos.detalhar(id);
    }
}
