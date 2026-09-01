package br.com.docintelligence.documento;

import java.time.Instant;
import java.util.UUID;

/** Item da listagem {@code GET /documentos} (secao 4) — versão enxuta, sem campos nem histórico. */
public record DocumentoResumoResponse(
        UUID id,
        String estado,
        String tipo,
        Double confianca,
        String nomePadronizado,
        Instant criadoEm) {

    static DocumentoResumoResponse de(Documento d) {
        return new DocumentoResumoResponse(
                d.getId(),
                d.getEstado().name().toLowerCase(),
                d.getTipo(),
                d.getConfianca(),
                d.getNomePadronizado(),
                d.getCriadoEm());
    }
}
