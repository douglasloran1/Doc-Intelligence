package br.com.docintelligence.documento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransicaoEstadoRepository extends JpaRepository<TransicaoEstado, UUID> {

    List<TransicaoEstado> findByDocumentoIdOrderByOcorridoEmAsc(UUID documentoId);
}
