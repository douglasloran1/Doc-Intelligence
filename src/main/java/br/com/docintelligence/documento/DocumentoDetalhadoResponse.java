package br.com.docintelligence.documento;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Corpo de resposta do {@code GET /documentos/{id}} (secao 4): estado, campos, confiança, nome e histórico. */
public record DocumentoDetalhadoResponse(
        UUID id,
        String estado,
        String tipo,
        Double confianca,
        Map<String, Object> camposExtraidos,
        Map<String, Object> correcaoAplicada,
        String nomePadronizado,
        String versaoAdaptador,
        String reivindicadoPor,
        Instant reivindicadoEm,
        Instant reivindicacaoExpiraEm,
        Instant criadoEm,
        Instant atualizadoEm,
        List<TransicaoResponse> historico) {

    public record TransicaoResponse(
            String estadoAnterior, String estadoNovo, Instant ocorridoEm, String motivo) {
    }

    static DocumentoDetalhadoResponse de(Documento d, List<TransicaoEstado> historico) {
        List<TransicaoResponse> transicoes = historico.stream()
                .map(t -> new TransicaoResponse(
                        t.getEstadoAnterior() == null ? null : nome(t.getEstadoAnterior()),
                        nome(t.getEstadoNovo()),
                        t.getOcorridoEm(),
                        t.getMotivo()))
                .toList();

        return new DocumentoDetalhadoResponse(
                d.getId(),
                nome(d.getEstado()),
                d.getTipo(),
                d.getConfianca(),
                d.getCamposExtraidos(),
                d.getCorrecaoAplicada(),
                d.getNomePadronizado(),
                d.getVersaoAdaptador(),
                d.getReivindicadoPor(),
                d.getReivindicadoEm(),
                d.getReivindicacaoExpiraEm(),
                d.getCriadoEm(),
                d.getAtualizadoEm(),
                transicoes);
    }

    private static String nome(EstadoDocumento estado) {
        return estado.name().toLowerCase();
    }
}
