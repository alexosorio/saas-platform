package com.saas.platform.user.controller;

import com.saas.platform.user.dto.LoginRequest;
import org.springframework.http.HttpStatus;
import com.saas.platform.user.dto.RegistroOrganizacionRequest;
import com.saas.platform.user.dto.AuthResponse;
import com.saas.platform.user.dto.RegisterRequest;
import com.saas.platform.user.service.AuthService;
import com.saas.platform.user.service.VerificacionEmailService;
import com.saas.platform.user.dto.MessageResponse;
import java.util.Map;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final VerificacionEmailService verificacion;
    
    /**
     * Login - Autenticación de usuario
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Registro de nuevo usuario
     * POST /api/auth/register
     */
    /** Asistente de registro: organización nueva + usuario propietario. Deja la sesión iniciada. */
    @PostMapping("/registro-organizacion")
    public ResponseEntity<AuthResponse> registrarOrganizacion(@jakarta.validation.Valid @RequestBody RegistroOrganizacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarOrganizacion(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Refrescar token JWT
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        // Remover "Bearer " del header
        String token = refreshToken.substring(7);
        AuthResponse response = authService.refreshToken(token);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Validar token JWT
     * GET /api/auth/validate
     */
    /** Verificación con el enlace del correo. body: { "token": "..." } */
    @PostMapping("/verificar-email")
    public ResponseEntity<MessageResponse> verificarEmail(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(verificacion.verificarConEnlace(body.get("token")));
    }

    /** Pide un enlace nuevo: body { "email": "..." } o { "token": "<enlace vencido>" }. Respuesta genérica. */
    @PostMapping("/reenviar-verificacion")
    public ResponseEntity<MessageResponse> reenviarVerificacion(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(verificacion.reenviarPublico(body.get("email"), body.get("token")));
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        // Remover "Bearer " del header
        String jwtToken = token.substring(7);
        boolean isValid = authService.validateToken(jwtToken);
        return ResponseEntity.ok(isValid);
    }
    
    /**
     * Logout (invalidar token - opcional, depende de la estrategia)
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        // En una implementación con lista negra de tokens o Redis
        // aquí se invalidaría el token
        return ResponseEntity.ok().build();
    }
}
