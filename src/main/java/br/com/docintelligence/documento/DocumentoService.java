package br.com.docintelligence.documento;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Recebimento de documento: valida na fronteira, deduplica por hash e cria o
 * registro em RECEBIDO com a primeira transicao gravada. O processamento
 * (fila/worker) e as proximas partes desta fatia.
 */
@Service
public class DocumentoService {

    /** Teto de 15 MB da regra de negocio (secao 4; restricao b). */
    static final long TAMANHO_MAXIMO_BYTES = 15L * 1024 * 1024;

    /** Formatos aceitos (secao 4). Derivados do sufixo do nome do arquivo enviado. */
    static final Set<String> FORMATOS_ACEITOS = Set.of("jpg", "jpeg", "png", "pdf");

    /** Tipo unico desta fatia vertical (secao 2). */
    static final String TIPO_PADRAO = "identidade";

    private final DocumentoRepository documentoRepository;
    private final TransicaoEstadoRepository transicaoRepository;

    public DocumentoService(DocumentoRepository documentoRepository,
                            TransicaoEstadoRepository transicaoRepository) {
        this.documentoRepository = documentoRepository;
        this.transicaoRepository = transicaoRepository;
    }

    @Transactional
    public ResultadoCriacao criar(MultipartFile arquivo) {
        validar(arquivo);

        String hash = sha256(arquivo);

        Optional<Documento> existente = documentoRepository.findByHashConteudo(hash);
        if (existente.isPresent()) {
            // Idempotencia: reenvio do mesmo arquivo devolve o registro existente,
            // sem criar outro nem reprocessar (secao 4; restricao c).
            return new ResultadoCriacao(existente.get(), false);
        }

        Documento documento = new Documento();
        documento.setHashConteudo(hash);
        documento.setTipo(TIPO_PADRAO);
        documento.setEstado(EstadoDocumento.RECEBIDO);
        documento = documentoRepository.save(documento);

        transicaoRepository.save(
                TransicaoEstado.de(documento, null, EstadoDocumento.RECEBIDO, null));

        return new ResultadoCriacao(documento, true);
    }

    private void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("arquivo_ausente", "Nenhum arquivo recebido.");
        }
        String extensao = extensao(arquivo.getOriginalFilename());
        if (!FORMATOS_ACEITOS.contains(extensao)) {
            throw new ArquivoInvalidoException(
                    "formato_nao_suportado",
                    "Formato nao suportado. Aceitos: jpg, jpeg, png, pdf.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new ArquivoInvalidoException(
                    "arquivo_grande", "Arquivo acima do limite de 15 MB.");
        }
    }

    private static String extensao(String nomeArquivo) {
        if (nomeArquivo == null) {
            return "";
        }
        int ponto = nomeArquivo.lastIndexOf('.');
        if (ponto < 0 || ponto == nomeArquivo.length() - 1) {
            return "";
        }
        return nomeArquivo.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    private static String sha256(MultipartFile arquivo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(arquivo.getBytes()));
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException("Falha ao calcular o hash do arquivo.", e);
        }
    }

    /** Resultado da criacao: o documento e se ele foi criado agora ({@code true}) ou ja existia. */
    public record ResultadoCriacao(Documento documento, boolean criado) {
    }
}
