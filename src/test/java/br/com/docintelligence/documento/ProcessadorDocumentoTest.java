package br.com.docintelligence.documento;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessadorDocumentoTest {

    private final JobProcessamentoRepository jobs = mock(JobProcessamentoRepository.class);
    private final DocumentoRepository documentos = mock(DocumentoRepository.class);
    private final TransicaoEstadoRepository transicoes = mock(TransicaoEstadoRepository.class);
    private final RegrasDocumento regras = new RegrasDocumento();

    private Documento documentoRecebido(long tamanhoBytes) {
        Documento doc = new Documento();
        doc.setId(UUID.randomUUID());
        doc.setTipo("identidade");
        doc.setEstado(EstadoDocumento.RECEBIDO);
        doc.setHashConteudo("a".repeat(64));
        doc.setTamanhoBytes(tamanhoBytes);
        doc.setExtensaoOriginal("jpg");
        return doc;
    }

    private JobProcessamento jobEmExecucaoPara(Documento doc) {
        JobProcessamento job = JobProcessamento.pendentePara(doc);
        job.setId(UUID.randomUUID());
        job.setEstado(EstadoJob.EM_EXECUCAO);
        when(jobs.buscarComDocumento(job.getId())).thenReturn(Optional.of(job));
        return job;
    }

    private ProcessadorDocumento processadorCom(AdaptadorExtracao adaptador) {
        return new ProcessadorDocumento(jobs, documentos, transicoes, adaptador, regras);
    }

    @Test
    void arquivoDe500KBouMaisChegaEmProntoEOJobEhConcluido() {
        Documento doc = documentoRecebido(600 * 1024);
        JobProcessamento job = jobEmExecucaoPara(doc);

        processadorCom(new DubleExtracao()).executar(job.getId());

        assertThat(doc.getEstado()).isEqualTo(EstadoDocumento.PRONTO);
        assertThat(doc.getConfianca()).isEqualTo(0.95);
        assertThat(doc.getVersaoAdaptador()).isEqualTo("duble-identidade-1");
        assertThat(doc.getNomePadronizado())
                .isEqualTo("identidade_" + doc.getId() + ".jpg");
        assertThat(doc.getCamposExtraidos()).containsKey("nome_completo");
        assertThat(job.getEstado()).isEqualTo(EstadoJob.CONCLUIDO);

        ArgumentCaptor<TransicaoEstado> t = ArgumentCaptor.forClass(TransicaoEstado.class);
        verify(transicoes, times(2)).save(t.capture());
        assertThat(t.getAllValues()).extracting(TransicaoEstado::getEstadoNovo)
                .containsExactly(EstadoDocumento.EM_PROCESSAMENTO, EstadoDocumento.PRONTO);
    }

    @Test
    void arquivoAbaixoDe500KBchegaEmAguardandoConferencia() {
        Documento doc = documentoRecebido(100 * 1024);
        JobProcessamento job = jobEmExecucaoPara(doc);

        processadorCom(new DubleExtracao()).executar(job.getId());

        assertThat(doc.getEstado()).isEqualTo(EstadoDocumento.AGUARDANDO_CONFERENCIA);
        assertThat(doc.getConfianca()).isEqualTo(0.60);
        assertThat(doc.getNomePadronizado()).isNotBlank(); // proposto antes da conferencia humana
        assertThat(job.getEstado()).isEqualTo(EstadoJob.CONCLUIDO);
    }

    @Test
    void falhaNoAdaptadorAgendaRetryComBackoffAteFalhaDefinitiva() {
        AdaptadorExtracao sempreFalha = new AdaptadorExtracao() {
            @Override
            public ResultadoExtracao extrair(EntradaExtracao entrada) {
                throw new FalhaExtracaoException("timeout");
            }

            @Override
            public String versao() {
                return "stub";
            }
        };
        Documento doc = documentoRecebido(600 * 1024);
        JobProcessamento job = jobEmExecucaoPara(doc);
        ProcessadorDocumento processador = processadorCom(sempreFalha);

        processador.executar(job.getId());
        assertThat(job.getTentativas()).isEqualTo(1);
        assertThat(job.getEstado()).isEqualTo(EstadoJob.PENDENTE);
        assertThat(doc.getEstado()).isEqualTo(EstadoDocumento.FALHA_TEMPORARIA);
        assertThat(job.getProximaTentativaEm()).isAfter(Instant.now());
        Instant aposPrimeira = job.getProximaTentativaEm();

        processador.executar(job.getId());
        assertThat(job.getTentativas()).isEqualTo(2);
        assertThat(job.getEstado()).isEqualTo(EstadoJob.PENDENTE);
        // backoff maior na segunda falha (10s vs 5s)
        assertThat(job.getProximaTentativaEm()).isAfter(aposPrimeira);

        processador.executar(job.getId());
        assertThat(job.getTentativas()).isEqualTo(3);
        assertThat(job.getEstado()).isEqualTo(EstadoJob.FALHOU);
        assertThat(doc.getEstado()).isEqualTo(EstadoDocumento.FALHA_DEFINITIVA);

        ArgumentCaptor<TransicaoEstado> t = ArgumentCaptor.forClass(TransicaoEstado.class);
        verify(transicoes, times(6)).save(t.capture());
        List<TransicaoEstado> todas = t.getAllValues();
        assertThat(todas.get(5).getEstadoNovo()).isEqualTo(EstadoDocumento.FALHA_DEFINITIVA);
        assertThat(todas.get(5).getMotivo()).isEqualTo("timeout");
    }
}
