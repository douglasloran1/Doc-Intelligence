package br.com.docintelligence.documento;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConferenciaServiceTest {

    private static final long LEASE_SEGUNDOS = 300;

    private final DocumentoRepository documentos = mock(DocumentoRepository.class);
    private final TransicaoEstadoRepository transicoes = mock(TransicaoEstadoRepository.class);
    private final ConferenciaService service =
            new ConferenciaService(documentos, transicoes, LEASE_SEGUNDOS);

    private Documento documento(EstadoDocumento estado) {
        Documento d = new Documento();
        d.setId(UUID.randomUUID());
        d.setTipo("identidade");
        d.setEstado(estado);
        d.setHashConteudo("a".repeat(64));
        when(documentos.findById(d.getId())).thenReturn(Optional.of(d));
        when(documentos.save(any(Documento.class))).thenAnswer(inv -> inv.getArgument(0));
        return d;
    }

    @Test
    void reivindicarMoveParaEmConferenciaERegistraOperadorELease() {
        Documento d = documento(EstadoDocumento.AGUARDANDO_CONFERENCIA);

        service.reivindicar(d.getId(), "op-1");

        assertThat(d.getEstado()).isEqualTo(EstadoDocumento.EM_CONFERENCIA);
        assertThat(d.getReivindicadoPor()).isEqualTo("op-1");
        assertThat(d.getReivindicacaoExpiraEm()).isAfter(Instant.now());

        ArgumentCaptor<TransicaoEstado> t = ArgumentCaptor.forClass(TransicaoEstado.class);
        org.mockito.Mockito.verify(transicoes).save(t.capture());
        assertThat(t.getValue().getEstadoNovo()).isEqualTo(EstadoDocumento.EM_CONFERENCIA);
    }

    @Test
    void reivindicarPorOutroOperadorComReivindicacaoAtivaDa409() {
        Documento d = documento(EstadoDocumento.EM_CONFERENCIA);
        d.setReivindicadoPor("op-1");
        d.setReivindicacaoExpiraEm(Instant.now().plusSeconds(120));

        assertThatThrownBy(() -> service.reivindicar(d.getId(), "op-2"))
                .asInstanceOf(throwable(ConflitoDeEstadoException.class))
                .extracting(ConflitoDeEstadoException::getCodigoMotivo)
                .isEqualTo("ja_reivindicado");
    }

    @Test
    void corrigirComOperadorDonoMoveParaConcluidoEGuardaACorrecao() {
        Documento d = documento(EstadoDocumento.EM_CONFERENCIA);
        d.setReivindicadoPor("op-1");
        d.setReivindicacaoExpiraEm(Instant.now().plusSeconds(120));

        service.corrigir(d.getId(), "op-1", Map.of("cpf", "000.000.000-00"));

        assertThat(d.getEstado()).isEqualTo(EstadoDocumento.CONCLUIDO);
        assertThat(d.getCorrecaoAplicada()).isEqualTo(Map.of("cpf", "000.000.000-00"));
    }

    @Test
    void corrigirPorOutroOperadorDa409() {
        Documento d = documento(EstadoDocumento.EM_CONFERENCIA);
        d.setReivindicadoPor("op-1");
        d.setReivindicacaoExpiraEm(Instant.now().plusSeconds(120));

        assertThatThrownBy(() -> service.corrigir(d.getId(), "op-2", Map.of()))
                .asInstanceOf(throwable(ConflitoDeEstadoException.class))
                .extracting(ConflitoDeEstadoException::getCodigoMotivo)
                .isEqualTo("operador_diferente");
    }

    @Test
    void rejeitarSemMotivoDa422() {
        Documento d = documento(EstadoDocumento.EM_CONFERENCIA);
        d.setReivindicadoPor("op-1");
        d.setReivindicacaoExpiraEm(Instant.now().plusSeconds(120));

        assertThatThrownBy(() -> service.rejeitar(d.getId(), "op-1", "  "))
                .asInstanceOf(throwable(RequisicaoInvalidaException.class))
                .extracting(RequisicaoInvalidaException::getCodigoMotivo)
                .isEqualTo("motivo_obrigatorio");
    }

    @Test
    void rejeitarComMotivoMoveParaRejeitadoEGravaOMotivoNaTransicao() {
        Documento d = documento(EstadoDocumento.EM_CONFERENCIA);
        d.setReivindicadoPor("op-1");
        d.setReivindicacaoExpiraEm(Instant.now().plusSeconds(120));

        service.rejeitar(d.getId(), "op-1", "arquivo ilegivel");

        assertThat(d.getEstado()).isEqualTo(EstadoDocumento.REJEITADO);

        ArgumentCaptor<TransicaoEstado> t = ArgumentCaptor.forClass(TransicaoEstado.class);
        org.mockito.Mockito.verify(transicoes).save(t.capture());
        assertThat(t.getValue().getEstadoNovo()).isEqualTo(EstadoDocumento.REJEITADO);
        assertThat(t.getValue().getMotivo()).isEqualTo("arquivo ilegivel");
    }

    @Test
    void reivindicacaoExpiradaLiberaODocumentoParaNovaReivindicacao() {
        Documento d = documento(EstadoDocumento.EM_CONFERENCIA);
        d.setReivindicadoPor("op-1");
        d.setReivindicacaoExpiraEm(Instant.now().minusSeconds(1)); // ja expirou

        service.reivindicar(d.getId(), "op-2");

        assertThat(d.getEstado()).isEqualTo(EstadoDocumento.EM_CONFERENCIA);
        assertThat(d.getReivindicadoPor()).isEqualTo("op-2");

        ArgumentCaptor<TransicaoEstado> t = ArgumentCaptor.forClass(TransicaoEstado.class);
        org.mockito.Mockito.verify(transicoes, org.mockito.Mockito.times(2)).save(t.capture());
        List<TransicaoEstado> transicoesSalvas = t.getAllValues();
        assertThat(transicoesSalvas.get(0).getEstadoNovo())
                .isEqualTo(EstadoDocumento.AGUARDANDO_CONFERENCIA);
        assertThat(transicoesSalvas.get(0).getMotivo()).isEqualTo("reivindicacao expirada");
        assertThat(transicoesSalvas.get(1).getEstadoNovo())
                .isEqualTo(EstadoDocumento.EM_CONFERENCIA);
    }

    @Test
    void reivindicarDocumentoQueNaoExisteDa404() {
        UUID id = UUID.randomUUID();
        when(documentos.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reivindicar(id, "op-1"))
                .asInstanceOf(throwable(RecursoNaoEncontradoException.class))
                .extracting(RecursoNaoEncontradoException::getCodigoMotivo)
                .isEqualTo("documento_nao_encontrado");
    }
}
