package com.ulima.incidenciaurbana.dto;

public class TecnicoDTO {

    private Long id;
    private String usuario;
    private String nombres;
    private String apellidos;
    private String correo;

    public TecnicoDTO() {}

    public TecnicoDTO(Long id, String usuario, String nombres, String apellidos, String correo) {
        this.id = id;
        this.usuario = usuario;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
}
