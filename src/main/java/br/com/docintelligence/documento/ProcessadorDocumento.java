package br.com.docintelligence.documento;

import br.com.docintelligence.documento.AdaptadorExtracao.EntradaExtracao;
import br.com.docintelligence.documento.AdaptadorExtracao.ResultadoExtracao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Processa um job já reivindicado: move o documento por {@code em_processamento},
 * chama o adaptador, aplica as regras de domínio e grava o próximo estado. Numa
 * transação própria (a reivindicação foi noutra — ver {@link FilaProcessamento}).
 */
@Service
public class ProcessadorDocumento {

    /** Limite de tentativas antes de falha_definitiva (§6; restrição a). */
    static final int MAX_TENTATIVAS = 3;

    /**
     * Backoff entre tentativas: 5s, 10s, 20s (§6). Com {@link #MAX_TENTATIVAS} = 3
     * só os dois primeiros são usados; o terceiro documenta a progressão e passa a
     * valer se o limite subir.
     */
    static final List<Duration> BACKOFF = List.of(
            Duration.ofSeconds(5), Duration.ofSeconds(10), Duration.ofSeconds(20));

    private final JobProcessamentoRepository jobs;
    private final DocumentoRepository documentos;
    private final TransicaoEstadoRepository transicoes;
    private final AdaptadorExtracao adaptador;
    private final RegrasDocumento regras;

    public ProcessadorDocumento(JobProcessamentoRepository jobs,
                                DocumentoRepository documentos,
                                TransicaoEstadoRepository transicoes,
                                AdaptadorExtracao adaptador,
                                RegrasDocumento regras) {
        this.jobs = jobs;
        this.documentos = documentos;
        this.transicoes = transicoes;
        this.adaptador = adaptador;
        this.regras = regras;
    }

    @Transactional
    public void executar(UUID jobId) {
        JobProcessamento job = jobs.buscarComDocumento(jobId)
                .orElseThrow(() -> new IllegalStateException("Job não encontrado: " + jobId));
        Documento documento = job.getDocumento();

        mover(documento, EstadoDocumento.EM_PROCESSAMENTO, null);

        try {
            ResultadoExtracao resultado = adaptador.extrair(new EntradaExtracao(
                    documento.getTipo(), documento.getTamanhoBytes(), null));

            double confianca = regras.confiancaDoDocumento(resultado, documento.getTipo());
            documento.setCamposExtraidos(regras.camposParaPersistencia(resultado));
            documento.setConfianca(confianca);
            documento.setVersaoAdaptador(resultado.versao());
            documento.setNomePadronizado(regras.nomePadronizado(documento));

            mover(documento, regras.proximoEstado(confianca), null);
            job.setEstado(EstadoJob.CONCLUIDO);

        } catch (FalhaExtracaoException falha) {
            tratarFalhaTemporaria(job, documento, falha.getMessage());
        }

        documentos.save(documento);
        jobs.save(job);
    }

    private void tratarFalhaTemporaria(JobProcessamento job, Documento documento, String motivo) {
        job.setTentativas(job.getTentativas() + 1);

        if (job.getTentativas() >= MAX_TENTATIVAS) {
            mover(documento, EstadoDocumento.FALHA_DEFINITIVA, motivo);
            job.setEstado(EstadoJob.FALHOU);
            return;
        }

        mover(documento, EstadoDocumento.FALHA_TEMPORARIA, motivo);
        job.setEstado(EstadoJob.PENDENTE);
        job.setReivindicadoPor(null);
        job.setLeaseExpiraEm(null);
        job.setProximaTentativaEm(Instant.now().plus(BACKOFF.get(job.getTentativas() - 1)));
    }

    private void mover(Documento documento, EstadoDocumento novo, String motivo) {
        EstadoDocumento anterior = documento.getEstado();
        documento.setEstado(novo);
        transicoes.save(TransicaoEstado.de(documento, anterior, novo, motivo));
    }
}
