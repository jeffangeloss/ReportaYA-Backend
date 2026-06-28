package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.dto.LoginRequest;
import com.ulima.incidenciaurbana.dto.LoginResponse;
import com.ulima.incidenciaurbana.exception.EmailNotVerifiedException;
import com.ulima.incidenciaurbana.exception.InvalidCredentialsException;
import com.ulima.incidenciaurbana.model.Cuenta;
import com.ulima.incidenciaurbana.repository.CuentaRepository;
import com.ulima.incidenciaurbana.service.AuthService;
import com.ulima.incidenciaurbana.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final CuentaRepository cuentaRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(CuentaRepository cuentaRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Cuenta c = cuentaRepository.findByUsuario(request.getUsuario())
                .orElseThrow(InvalidCredentialsException::new);

        String stored = c.getContrasenaHash();
        boolean valid;
        boolean needsMigration = false;

        if (stored != null && isBcryptHash(stored)) {
            valid = passwordEncoder.matches(request.getPassword(), stored);
        } else {
            valid = stored != null && stored.equals(request.getPassword());
            if (valid) needsMigration = true;
        }

        if (!valid) throw new InvalidCredentialsException();

        // Credenciales correctas pero la cuenta aún no verificó su correo.
        // Se comprueba DESPUÉS del password para no revelar cuentas por el usuario.
        if (!c.isActivo()) {
            throw new EmailNotVerifiedException();
        }

        if (needsMigration) {
            c.setContrasenaHash(passwordEncoder.encode(request.getPassword()));
            cuentaRepository.save(c);
        }

        String nombre = (c.getPersona() != null) ? c.getPersona().getNombreCompleto() : "";
        String token = jwtUtil.generateToken(c.getId(), c.getUsuario(), c.getTipoCuenta());
        return new LoginResponse(c.getId(), c.getUsuario(), nombre, "Login exitoso", c.getTipoCuenta(), token);
    }

    @Override
    @Transactional(readOnly = true)
    public void solicitarRecuperacion(String correo) {
        if (correo == null || correo.trim().isEmpty()) {
            throw new RuntimeException("El correo es obligatorio");
        }
        cuentaRepository.findByCorreoAndActivoTrue(correo.trim())
                .orElseThrow(() -> new RuntimeException("El correo no esta asociado a ninguna cuenta"));
    }

    @Override
    @Transactional
    public void restablecerContrasena(String correo, String nuevaContrasena) {
        if (correo == null || correo.trim().isEmpty()) {
            throw new RuntimeException("El correo es obligatorio");
        }
        if (nuevaContrasena == null || nuevaContrasena.length() < 6) {
            throw new RuntimeException("La contrasena debe tener al menos 6 caracteres");
        }

        Cuenta cuenta = cuentaRepository.findByCorreoAndActivoTrue(correo.trim())
                .orElseThrow(() -> new RuntimeException("No se encontro una cuenta asociada a este correo"));

        cuenta.cambiarContrasena(passwordEncoder.encode(nuevaContrasena));
        cuentaRepository.save(cuenta);
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
