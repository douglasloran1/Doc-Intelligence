package br.com.docintelligence.documento;

import java.time.Instant;
import java.util.UUID;

/**
 * Corpo de resposta do endpoint. O passo 1 da secao 2 pede identificador e
 * status; os demais campos ajudam quem consulta sem forcar uma segunda chamada.
 */
public record DocumentoResponse(
        UUID id,
        String estado,
        String tipo,
        String hashConteudo,
        Double confianca,
        String nomePadronizado,
        Instant criadoEm
) {

    static DocumentoResponse de(Documento d) {
        return new DocumentoResponse(
                d.getId(),
                d.getEstado().name().toLowerCase(),
                d.getTipo(),
                d.getHashConteudo(),
                d.getConfianca(),
                d.getNomePadronizado(),
                d.getCriadoEm()
        );
    }
}
