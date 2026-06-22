package com.ulima.incidenciaurbana.service;

import com.ulima.incidenciaurbana.dto.LoginRequest;
import com.ulima.incidenciaurbana.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    void solicitarRecuperacion(String correo);

    void restablecerContrasena(String correo, String nuevaContrasena);
}
