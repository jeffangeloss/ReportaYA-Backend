package com.ulima.incidenciaurbana.repository;

import com.ulima.incidenciaurbana.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    Optional<Cuenta> findByUsuarioAndActivoTrue(String usuario);

    @org.springframework.data.jpa.repository.Query(
            "SELECT c FROM Cuenta c JOIN c.persona p WHERE LOWER(p.correo) = LOWER(:correo) AND c.activo = true")
    Optional<Cuenta> findByCorreoAndActivoTrue(@org.springframework.data.repository.query.Param("correo") String correo);
}
