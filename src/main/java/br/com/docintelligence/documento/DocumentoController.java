package br.com.docintelligence.documento;

import br.com.docintelligence.documento.RequisicoesConferencia.CorrecaoRequest;
import br.com.docintelligence.documento.RequisicoesConferencia.ReivindicarRequest;
import br.com.docintelligence.documento.RequisicoesConferencia.RejeitarRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 *
 * <p>As anotações {@code @Operation}/{@code @ApiResponse} descrevem cada operação e
 * seus códigos de motivo de erro, puxados da §4 da especificação, e alimentam a
 * Swagger UI em {@code /swagger-ui.html}. Não substituem a especificação — resumem.
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

    @Operation(summary = "Recebe um documento (multipart) e o enfileira para processamento",
            description = "Valida formato (jpg, jpeg, png, pdf) e tamanho (máx. 15 MB) na fronteira, "
                    + "pelo sufixo do nome do arquivo enviado — sem inspeção de conteúdo. Responde na hora "
                    + "com id e status 'recebido'. Idempotente por hash do conteúdo: reenvio do mesmo "
                    + "arquivo devolve o registro existente (200) em vez de criar outro (201). Ver §4.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Documento criado e enfileirado"),
            @ApiResponse(responseCode = "200",
                    description = "Hash já conhecido; devolve o documento existente (idempotência)"),
            @ApiResponse(responseCode = "422",
                    description = "Arquivo recusado antes da fila. Códigos de motivo: 'arquivo_ausente', "
                            + "'formato_nao_suportado', 'arquivo_grande'.",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<DocumentoResponse> receber(@RequestParam("arquivo") MultipartFile arquivo) {
        DocumentoService.ResultadoCriacao resultado = documentos.criar(arquivo);

        // 201 quando o documento e criado agora; 200 quando o hash ja existia e
        // devolvemos o registro existente (idempotencia, secao 4).
        HttpStatus status = resultado.criado() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(DocumentoResponse.de(resultado.documento()));
    }

    @Operation(summary = "Consulta um documento: estado atual, campos extraídos e histórico de transições",
            description = "Leitura pura — não altera estado. Ver §4.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento encontrado"),
            @ApiResponse(responseCode = "404",
                    description = "Documento inexistente. Código de motivo: 'documento_nao_encontrado'.",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public DocumentoDetalhadoResponse detalhar(@PathVariable UUID id) {
        return documentos.detalhar(id);
    }

    @Operation(summary = "Lista documentos, com filtro opcional por estado e tipo, paginada",
            description = "Leitura pura. 'pagina' começa em 0; 'tamanho' padrão 20. Ver §4.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de resultados (pode vir vazia)"),
            @ApiResponse(responseCode = "422",
                    description = "Valor de 'status' desconhecido. Código de motivo: 'status_invalido'.",
                    content = @Content)
    })
    @GetMapping
    public PaginaResponse<DocumentoResumoResponse> listar(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "" + TAMANHO_PAGINA_PADRAO) int tamanho) {
        return documentos.listar(status, tipo, pagina, tamanho);
    }

    @Operation(summary = "Reivindica um documento em aguardando_conferencia para conferência humana",
            description = "Marca posse por um operador, com expiração (lease). Só documentos em "
                    + "'aguardando_conferencia' podem ser reivindicados; o mesmo operador renova o lease. Ver §4.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reivindicado; documento em em_conferencia"),
            @ApiResponse(responseCode = "404",
                    description = "Documento inexistente. Código de motivo: 'documento_nao_encontrado'.",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Reivindicação em disputa ou estado incompatível. Códigos de motivo: "
                            + "'ja_reivindicado' (lease ativo de outro operador), "
                            + "'estado_invalido' (não está em aguardando_conferencia).",
                    content = @Content),
            @ApiResponse(responseCode = "422",
                    description = "Corpo sem operador. Código de motivo: 'operador_obrigatorio'.",
                    content = @Content)
    })
    @PostMapping("/{id}/reivindicar")
    public DocumentoDetalhadoResponse reivindicar(
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
                    examples = @ExampleObject(value = "{\"operador\": \"op-1\"}")))
            @RequestBody ReivindicarRequest corpo) {
        return detalharDe(conferencia.reivindicar(id, corpo.operador()).getId());
    }

    @Operation(summary = "Grava a correção de campos feita pelo operador e conclui o documento",
            description = "Exige reivindicação ativa do mesmo operador. Move o documento para 'concluido'. Ver §4.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Correção gravada; documento em concluido"),
            @ApiResponse(responseCode = "404",
                    description = "Documento inexistente. Código de motivo: 'documento_nao_encontrado'.",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Sem reivindicação compatível. Códigos de motivo: "
                            + "'nao_esta_em_conferencia', 'operador_diferente'.",
                    content = @Content),
            @ApiResponse(responseCode = "422",
                    description = "Corpo sem operador. Código de motivo: 'operador_obrigatorio'.",
                    content = @Content)
    })
    @PatchMapping("/{id}")
    public DocumentoDetalhadoResponse corrigir(
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
                    examples = @ExampleObject(value = "{\"operador\": \"op-1\", \"campos\": "
                            + "{\"cpf\": \"000.000.000-00\", \"nome_completo\": \"FULANO DE TAL\"}}")))
            @RequestBody CorrecaoRequest corpo) {
        return detalharDe(conferencia.corrigir(id, corpo.operador(), corpo.campos()).getId());
    }

    @Operation(summary = "Rejeita um documento não corrigível (ilegível, tipo errado, dados irrecuperáveis)",
            description = "Exige reivindicação ativa do mesmo operador e um 'motivo' obrigatório. "
                    + "Move o documento para 'rejeitado'. Ver §4.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento rejeitado"),
            @ApiResponse(responseCode = "404",
                    description = "Documento inexistente. Código de motivo: 'documento_nao_encontrado'.",
                    content = @Content),
            @ApiResponse(responseCode = "409",
                    description = "Sem reivindicação compatível. Códigos de motivo: "
                            + "'nao_esta_em_conferencia', 'operador_diferente'.",
                    content = @Content),
            @ApiResponse(responseCode = "422",
                    description = "Corpo sem 'motivo' (ou sem operador). Códigos de motivo: "
                            + "'motivo_obrigatorio', 'operador_obrigatorio'.",
                    content = @Content)
    })
    @PostMapping("/{id}/rejeitar")
    public DocumentoDetalhadoResponse rejeitar(
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
                    examples = @ExampleObject(value = "{\"operador\": \"op-1\", \"motivo\": \"arquivo ilegivel\"}")))
            @RequestBody RejeitarRequest corpo) {
        return detalharDe(conferencia.rejeitar(id, corpo.operador(), corpo.motivo()).getId());
    }

    private DocumentoDetalhadoResponse detalharDe(UUID id) {
        return documentos.detalhar(id);
    }
}
