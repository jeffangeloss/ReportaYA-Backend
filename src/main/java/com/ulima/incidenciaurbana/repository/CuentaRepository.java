package com.ulima.incidenciaurbana.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.ulima.incidenciaurbana.model.Cuenta;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    Optional<Cuenta> findByUsuarioAndActivoTrue(String usuario);

    Optional<Cuenta> findByUsuario(String usuario);

    Optional<Cuenta> findByTokenVerificacion(String token);

    Optional<Cuenta> findByTokenRecuperacion(String token);

    @Query("SELECT c FROM Cuenta c JOIN c.persona p WHERE LOWER(p.correo) = LOWER(:correo) AND c.activo = true")
    Optional<Cuenta> findByCorreoAndActivoTrue(@Param("correo") String correo);

    @Query("SELECT c FROM Cuenta c JOIN c.persona p WHERE LOWER(p.correo) = LOWER(:correo)")
    Optional<Cuenta> findByCorreo(@Param("correo") String correo);
}
