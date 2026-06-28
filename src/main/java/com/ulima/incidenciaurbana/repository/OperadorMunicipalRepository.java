package com.ulima.incidenciaurbana.repository;

import com.ulima.incidenciaurbana.model.OperadorMunicipal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperadorMunicipalRepository extends JpaRepository<OperadorMunicipal, Long> {
}
