package br.com.docintelligence.documento;

import java.util.Map;

/**
 * Corpos das requisições da fila de conferência (secao 4). O identificador de
 * operador vem no corpo (não em header) — auto-declarado e não autenticado nesta
 * entrega (restricao g / achado 14).
 */
final class RequisicoesConferencia {

    private RequisicoesConferencia() {
    }

    record ReivindicarRequest(String operador) {
    }

    record CorrecaoRequest(String operador, Map<String, Object> campos) {
    }

    record RejeitarRequest(String operador, String motivo) {
    }
}
