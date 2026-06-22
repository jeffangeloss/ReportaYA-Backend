package com.ulima.incidenciaurbana.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tecnicos")
public class Tecnico extends Cuenta {

    public Tecnico() {
        super();
    }

    public Tecnico(String usuario, String contrasenaHash, Persona persona) {
        super(usuario, contrasenaHash, persona);
    }

    @Override
    public String getTipoCuenta() {
        return "TECNICO";
    }
}
