package com.ulima.incidenciaurbana.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ciudadanos")
public class Ciudadano extends Cuenta {

    public Ciudadano() {
        super();
    }

    public Ciudadano(String usuario, String contrasenaHash, Persona persona) {
        super(usuario, contrasenaHash, persona);
    }

    @Override
    public String getTipoCuenta() {
        return "CIUDADANO";
    }
}
