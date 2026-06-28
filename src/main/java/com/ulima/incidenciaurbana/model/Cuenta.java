package com.ulima.incidenciaurbana.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "cuentas")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String usuario;

    @Column(name = "contrasena_hash", nullable = false)
    private String contrasenaHash;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "persona_id", nullable = false, unique = true) // cambio
    private Persona persona;

    @OneToMany(mappedBy = "cuenta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reporte> reportes = new ArrayList<>();

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "token_verificacion")
    private String tokenVerificacion;

    @Column(name = "token_expiracion")
    private LocalDateTime tokenExpiracion;

    // Constructors
    public Cuenta() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        this.activo = true;
    }

    public Cuenta(String usuario, String contrasenaHash, Persona persona) {
        this();
        this.usuario = usuario;
        this.contrasenaHash = contrasenaHash;
        this.persona = persona;
    }

    // Business Methods
    public void crearReporte(Reporte reporte) {
        reportes.add(reporte);
        reporte.setCuenta(this);
    }

    public void cambiarContrasena(String nuevaContrasenaHash) {
        this.contrasenaHash = nuevaContrasenaHash;
        this.fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    public List<Reporte> getReportes() {
        return reportes;
    }

    public void setReportes(List<Reporte> reportes) {
        this.reportes = reportes;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getTokenVerificacion() {
        return tokenVerificacion;
    }

    public void setTokenVerificacion(String tokenVerificacion) {
        this.tokenVerificacion = tokenVerificacion;
    }

    public LocalDateTime getTokenExpiracion() {
        return tokenExpiracion;
    }

    public void setTokenExpiracion(LocalDateTime tokenExpiracion) {
        this.tokenExpiracion = tokenExpiracion;
    }

    /**
     * Asigna un token de verificación de correo con su fecha de expiración.
     */
    public void asignarTokenVerificacion(String token, LocalDateTime expiracion) {
        this.tokenVerificacion = token;
        this.tokenExpiracion = expiracion;
    }

    /**
     * Indica si el token de verificación ya expiró (o no existe).
     */
    public boolean tokenVerificacionExpirado() {
        return tokenExpiracion == null || tokenExpiracion.isBefore(LocalDateTime.now());
    }

    /**
     * Activa la cuenta tras verificar el correo y limpia el token.
     */
    public void confirmarVerificacion() {
        this.activo = true;
        this.tokenVerificacion = null;
        this.tokenExpiracion = null;
    }

    /**
     * Retorna el tipo de cuenta concreto (p. ej. CIUDADANO, TECNICO, OPERADOR_MUNICIPAL).
     * Cada subclase debe implementar este método para indicar su tipo.
     */
    public abstract String getTipoCuenta();

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Cuenta))
            return false;
        Cuenta cuenta = (Cuenta) o;
        return Objects.equals(id, cuenta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
