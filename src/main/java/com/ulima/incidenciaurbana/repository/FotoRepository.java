package com.ulima.incidenciaurbana.repository;

import com.ulima.incidenciaurbana.model.Foto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FotoRepository extends JpaRepository<Foto, Long> {
}
