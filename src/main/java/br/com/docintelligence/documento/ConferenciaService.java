package br.com.docintelligence.documento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Fila de conferência humana (secao 4; módulo "Fila de conferência" do §5).
 * Reivindicação com expiração, correção e rejeição, com posse por operador.
 *
 * <p><b>Expiração:</b> verificada sob demanda, no início de {@link #reivindicar}
 * ({@link #liberarSeExpirado}). Não há worker dedicado: um documento cuja
 * reivindicação expirou continua {@code em_conferencia} na base até a próxima
 * tentativa de reivindicação, que o devolve a {@code aguardando_conferencia} e o
 * repega. As leituras ({@code GET}) permanecem puras (secao 3) — não disparam a
 * liberação. Uma varredura periódica é a evolução natural.
 */
@Service
public class ConferenciaService {

    private final DocumentoRepository documentos;
    private final TransicaoEstadoRepository transicoes;
    private final Duration lease;

    public ConferenciaService(
            DocumentoRepository documentos,
            TransicaoEstadoRepository transicoes,
            @Value("${conferencia.lease-segundos:300}") long leaseSegundos) {
        this.documentos = documentos;
        this.transicoes = transicoes;
        this.lease = Duration.ofSeconds(leaseSegundos);
    }

    @Transactional
    public Documento reivindicar(UUID id, String operador) {
        exigirOperador(operador);
        Documento documento = carregar(id);
        liberarSeExpirado(documento);

        if (documento.getEstado() == EstadoDocumento.EM_CONFERENCIA) {
            if (Objects.equals(documento.getReivindicadoPor(), operador)) {
                // Mesmo operador: renova o lease em vez de recusar.
                documento.setReivindicacaoExpiraEm(Instant.now().plus(lease));
                return documentos.save(documento);
            }
            throw new ConflitoDeEstadoException("ja_reivindicado",
                    "Documento reivindicado por %s ate %s.".formatted(
                            documento.getReivindicadoPor(), documento.getReivindicacaoExpiraEm()));
        }

        if (documento.getEstado() != EstadoDocumento.AGUARDANDO_CONFERENCIA) {
            throw new ConflitoDeEstadoException("estado_invalido",
                    "So documentos em aguardando_conferencia podem ser reivindicados; estado atual: "
                            + documento.getEstado().name().toLowerCase() + ".");
        }

        Instant agora = Instant.now();
        documento.setReivindicadoPor(operador);
        documento.setReivindicadoEm(agora);
        documento.setReivindicacaoExpiraEm(agora.plus(lease));
        mover(documento, EstadoDocumento.EM_CONFERENCIA, null);
        return documentos.save(documento);
    }

    @Transactional
    public Documento corrigir(UUID id, String operador, Map<String, Object> campos) {
        Documento documento = carregarEmConferenciaDoOperador(id, operador);
        documento.setCorrecaoAplicada(campos);
        documento.setReivindicacaoExpiraEm(null);
        mover(documento, EstadoDocumento.CONCLUIDO, null);
        return documentos.save(documento);
    }

    @Transactional
    public Documento rejeitar(UUID id, String operador, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new RequisicaoInvalidaException("motivo_obrigatorio",
                    "O campo 'motivo' e obrigatorio para rejeitar um documento.");
        }
        Documento documento = carregarEmConferenciaDoOperador(id, operador);
        documento.setReivindicacaoExpiraEm(null);
        mover(documento, EstadoDocumento.REJEITADO, motivo);
        return documentos.save(documento);
    }

    private Documento carregarEmConferenciaDoOperador(UUID id, String operador) {
        exigirOperador(operador);
        Documento documento = carregar(id);
        liberarSeExpirado(documento);

        if (documento.getEstado() != EstadoDocumento.EM_CONFERENCIA) {
            throw new ConflitoDeEstadoException("nao_esta_em_conferencia",
                    "O documento nao esta em_conferencia; estado atual: "
                            + documento.getEstado().name().toLowerCase() + ".");
        }
        if (!Objects.equals(documento.getReivindicadoPor(), operador)) {
            throw new ConflitoDeEstadoException("operador_diferente",
                    "A reivindicacao ativa e de outro operador.");
        }
        return documento;
    }

    private void liberarSeExpirado(Documento documento) {
        if (documento.getEstado() == EstadoDocumento.EM_CONFERENCIA
                && documento.getReivindicacaoExpiraEm() != null
                && Instant.now().isAfter(documento.getReivindicacaoExpiraEm())) {
            mover(documento, EstadoDocumento.AGUARDANDO_CONFERENCIA, "reivindicacao expirada");
            documento.setReivindicadoPor(null);
            documento.setReivindicadoEm(null);
            documento.setReivindicacaoExpiraEm(null);
        }
    }

    private Documento carregar(UUID id) {
        return documentos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException(
                "documento_nao_encontrado", "Documento " + id + " nao existe."));
    }

    private static void exigirOperador(String operador) {
        if (operador == null || operador.isBlank()) {
            throw new RequisicaoInvalidaException("operador_obrigatorio",
                    "O campo 'operador' e obrigatorio.");
        }
    }

    private void mover(Documento documento, EstadoDocumento novo, String motivo) {
        EstadoDocumento anterior = documento.getEstado();
        documento.setEstado(novo);
        transicoes.save(TransicaoEstado.de(documento, anterior, novo, motivo));
    }
}
