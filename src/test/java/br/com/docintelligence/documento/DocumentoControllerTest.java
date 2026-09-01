package br.com.docintelligence.documento;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentoController.class)
class DocumentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentoService service;

    @Test
    void devolve422ComCodigoDeMotivoQuandoOArquivoEhRecusado() throws Exception {
        when(service.criar(any())).thenThrow(new ArquivoInvalidoException(
                "formato_nao_suportado", "Formato nao suportado. Aceitos: jpg, jpeg, png, pdf."));

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "documento.gif", "image/gif", new byte[] {1});

        mockMvc.perform(multipart("/documentos").file(arquivo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("formato_nao_suportado"))
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void devolve201ComIdEStatusRecebidoAoCriar() throws Exception {
        Documento doc = new Documento();
        doc.setId(UUID.randomUUID());
        doc.setTipo("identidade");
        doc.setEstado(EstadoDocumento.RECEBIDO);
        doc.setHashConteudo("a".repeat(64));
        when(service.criar(any())).thenReturn(new DocumentoService.ResultadoCriacao(doc, true));

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "identidade.jpg", "image/jpeg", "x".getBytes());

        mockMvc.perform(multipart("/documentos").file(arquivo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(doc.getId().toString()))
                .andExpect(jsonPath("$.estado").value("recebido"))
                .andExpect(jsonPath("$.tipo").value("identidade"));
    }

    @Test
    void devolve200QuandoOHashJaExistiaEDevolveORegistroExistente() throws Exception {
        Documento existente = new Documento();
        existente.setId(UUID.randomUUID());
        existente.setTipo("identidade");
        existente.setEstado(EstadoDocumento.PRONTO);
        existente.setHashConteudo("b".repeat(64));
        when(service.criar(any())).thenReturn(new DocumentoService.ResultadoCriacao(existente, false));

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "identidade.jpg", "image/jpeg", "x".getBytes());

        mockMvc.perform(multipart("/documentos").file(arquivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existente.getId().toString()))
                .andExpect(jsonPath("$.estado").value("pronto"));
    }
}
