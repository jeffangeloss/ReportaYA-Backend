package com.ulima.incidenciaurbana.repository;

import com.ulima.incidenciaurbana.model.Ciudadano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CiudadanoRepository extends JpaRepository<Ciudadano, Long> {
}
