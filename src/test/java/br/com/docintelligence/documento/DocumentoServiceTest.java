package br.com.docintelligence.documento;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocumentoServiceTest {

    private final DocumentoRepository documentoRepository = mock(DocumentoRepository.class);
    private final TransicaoEstadoRepository transicaoRepository = mock(TransicaoEstadoRepository.class);
    private final DocumentoService service =
            new DocumentoService(documentoRepository, transicaoRepository);

    @Test
    void recusaFormatoNaoSuportado() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "documento.gif", "image/gif", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> service.criar(arquivo))
                .asInstanceOf(throwable(ArquivoInvalidoException.class))
                .extracting(ArquivoInvalidoException::getCodigoMotivo)
                .isEqualTo("formato_nao_suportado");

        verifyNoInteractions(documentoRepository, transicaoRepository);
    }

    @Test
    void recusaArquivoAcimaDoLimiteDe15MB() {
        byte[] conteudo = new byte[16 * 1024 * 1024]; // 16 MB
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "identidade.pdf", "application/pdf", conteudo);

        assertThatThrownBy(() -> service.criar(arquivo))
                .asInstanceOf(throwable(ArquivoInvalidoException.class))
                .extracting(ArquivoInvalidoException::getCodigoMotivo)
                .isEqualTo("arquivo_grande");

        verifyNoInteractions(documentoRepository, transicaoRepository);
    }

    @Test
    void criaDocumentoEmRecebidoComPrimeiraTransicaoQuandoHashEhNovo() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "identidade.jpg", "image/jpeg", "conteudo-ficticio".getBytes());
        when(documentoRepository.findByHashConteudo(any())).thenReturn(Optional.empty());
        when(documentoRepository.save(any(Documento.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentoService.ResultadoCriacao resultado = service.criar(arquivo);

        assertThat(resultado.criado()).isTrue();
        assertThat(resultado.documento().getEstado()).isEqualTo(EstadoDocumento.RECEBIDO);
        assertThat(resultado.documento().getTipo()).isEqualTo("identidade");
        assertThat(resultado.documento().getHashConteudo()).hasSize(64); // SHA-256 em hex

        ArgumentCaptor<TransicaoEstado> transicao = ArgumentCaptor.forClass(TransicaoEstado.class);
        verify(transicaoRepository).save(transicao.capture());
        assertThat(transicao.getValue().getEstadoAnterior()).isNull();
        assertThat(transicao.getValue().getEstadoNovo()).isEqualTo(EstadoDocumento.RECEBIDO);
    }

    @Test
    void naoCriaSegundoRegistroQuandoOHashJaExiste() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "identidade.jpg", "image/jpeg", "conteudo-ficticio".getBytes());
        Documento existente = new Documento();
        existente.setEstado(EstadoDocumento.PRONTO);
        when(documentoRepository.findByHashConteudo(any())).thenReturn(Optional.of(existente));

        DocumentoService.ResultadoCriacao resultado = service.criar(arquivo);

        assertThat(resultado.criado()).isFalse();
        assertThat(resultado.documento()).isSameAs(existente);
        verify(documentoRepository, never()).save(any());
        verify(transicaoRepository, never()).save(any());
    }

    @Test
    void mesmoConteudoProduzOMesmoHash() {
        byte[] bytes = "identidade-joao".getBytes();
        MockMultipartFile a = new MockMultipartFile("arquivo", "a.jpg", "image/jpeg", bytes);
        MockMultipartFile b = new MockMultipartFile("arquivo", "b.png", "image/png", bytes);
        when(documentoRepository.findByHashConteudo(any())).thenReturn(Optional.empty());
        when(documentoRepository.save(any(Documento.class))).thenAnswer(inv -> inv.getArgument(0));

        String hashA = service.criar(a).documento().getHashConteudo();
        String hashB = service.criar(b).documento().getHashConteudo();

        assertThat(hashA).isEqualTo(hashB);
    }
}
