package com.ulima.incidenciaurbana.repository;

import com.ulima.incidenciaurbana.model.EstadoReporte;
import com.ulima.incidenciaurbana.model.Reporte;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Long>, JpaSpecificationExecutor<Reporte> {

    @EntityGraph(attributePaths = { "ubicacion", "cuenta", "cuenta.persona" })
    Page<Reporte> findByCuentaId(Long cuentaId, Pageable pageable);

    @EntityGraph(attributePaths = { "ubicacion", "cuenta", "cuenta.persona" })
    Page<Reporte> findByEstado(EstadoReporte estado, Pageable pageable);

    @EntityGraph(attributePaths = { "ubicacion", "cuenta", "cuenta.persona" })
    Page<Reporte> findByTecnicoIdAndEstado(Long tecnicoId, EstadoReporte estado, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "ubicacion", "cuenta", "cuenta.persona" })
    Page<Reporte> findAll(Pageable pageable);
}
