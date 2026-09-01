package br.com.docintelligence.documento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransicaoEstadoRepository extends JpaRepository<TransicaoEstado, UUID> {

    @Query("select t from TransicaoEstado t where t.documento.id = :documentoId order by t.ocorridoEm asc")
    List<TransicaoEstado> historicoDo(@Param("documentoId") UUID documentoId);
}
