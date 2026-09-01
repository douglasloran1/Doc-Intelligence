package br.com.docintelligence.documento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    /** Suporte a idempotencia por hash (secao 4, restricao c). */
    Optional<Documento> findByHashConteudo(String hashConteudo);
}
