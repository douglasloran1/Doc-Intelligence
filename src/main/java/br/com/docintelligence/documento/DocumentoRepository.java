package br.com.docintelligence.documento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    /** Suporte a idempotencia por hash (secao 4, restricao c). */
    Optional<Documento> findByHashConteudo(String hashConteudo);

    /** Listagem paginada com filtros opcionais por estado e tipo (secao 4). Parametro nulo = sem filtro. */
    @Query("""
            select d from Documento d
            where (:estado is null or d.estado = :estado)
              and (:tipo is null or d.tipo = :tipo)
            """)
    Page<Documento> listar(@Param("estado") EstadoDocumento estado,
                           @Param("tipo") String tipo,
                           Pageable pageable);
}
