package com.saas.platform.user.controller;

import com.saas.platform.user.dto.*;
import com.saas.platform.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final com.saas.platform.user.security.AuthorizationHelper authz;
    
    /**
     * Crear nuevo usuario
     * POST /api/users
     */
    @PostMapping
    @PreAuthorize("@authz.can('usuarios','crear')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request,
                                                   Authentication authentication) {
        Long adminId = ((com.saas.platform.user.security.UserPrincipal) authentication.getPrincipal()).getId();
        UserResponse response = userService.createUser(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Obtener usuario actual
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        Long userId = ((com.saas.platform.user.security.UserPrincipal) authentication.getPrincipal()).getId();
        UserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtener usuario por ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authz.can('usuarios','leer') or #id == authentication.principal.id")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUser(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Listar todos los usuarios
     * GET /api/users
     */
    @GetMapping
    @PreAuthorize("@authz.can('usuarios','leer')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    /**
     * Actualizar usuario
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authz.can('usuarios','actualizar') or #id == authentication.principal.id")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        // Quien edita su propio perfil sin permiso de administración NO puede cambiar su rol ni su estado
        if (!authz.can("usuarios", "actualizar")) {
            request.setRolId(null);
            request.setActivo(null);
        }
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Desactivar usuario
     * PUT /api/users/{id}/deactivate
     */
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("@authz.can('usuarios','actualizar')")
    public ResponseEntity<MessageResponse> deactivateUser(@PathVariable Long id) {
        MessageResponse response = userService.deactivateUser(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Activar usuario
     * PUT /api/users/{id}/activate
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("@authz.can('usuarios','actualizar')")
    public ResponseEntity<MessageResponse> activateUser(@PathVariable Long id) {
        MessageResponse response = userService.activateUser(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Eliminar usuario permanentemente
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.can('usuarios','eliminar')")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
        MessageResponse response = userService.deleteUser(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cambiar contraseña
     * POST /api/users/change-password
     */
    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        Long userId = ((com.saas.platform.user.security.UserPrincipal) authentication.getPrincipal()).getId();
        MessageResponse response = userService.changePassword(userId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Solicitar recuperación de contraseña (código por email)
     * POST /api/users/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        MessageResponse response = userService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Solicitar enlace de recuperación (token por email)
     * POST /api/users/request-password-reset
     */
    @PostMapping("/request-password-reset")
    public ResponseEntity<MessageResponse> requestPasswordReset(@RequestParam String email) {
        MessageResponse response = userService.requestPasswordResetLink(email);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Resetear contraseña con código
     * POST /api/users/reset-password-code
     */
    @PostMapping("/reset-password-code")
    public ResponseEntity<MessageResponse> resetPasswordWithCode(
            @Valid @RequestBody ResetPasswordWithCodeRequest request) {
        MessageResponse response = userService.resetPasswordWithCode(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Resetear contraseña con token (desde enlace)
     * POST /api/users/reset-password-token
     */
    @PostMapping("/reset-password-token")
    public ResponseEntity<MessageResponse> resetPasswordWithToken(
            @Valid @RequestBody ResetPasswordWithTokenRequest request) {
        MessageResponse response = userService.resetPasswordWithToken(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verificar email
     * POST /api/users/verify-email
     */
    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        MessageResponse response = userService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Reenviar código de verificación
     * POST /api/users/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerificationCode(@RequestParam String email) {
        MessageResponse response = userService.resendVerificationCode(email);
        return ResponseEntity.ok(response);
    }

    /** El usuario con sesión iniciada pide un nuevo enlace de verificación. */
    @PostMapping("/me/reenviar-verificacion")
    public ResponseEntity<MessageResponse> reenviarVerificacionPropia() {
        return ResponseEntity.ok(userService.reenviarVerificacionPropia());
    }

    /** Un administrador reenvía el enlace de verificación a un usuario de su organización. */
    @PostMapping("/{id}/reenviar-verificacion")
    @PreAuthorize("@authz.can('usuarios','actualizar')")
    public ResponseEntity<MessageResponse> reenviarVerificacion(@PathVariable Long id) {
        return ResponseEntity.ok(userService.reenviarVerificacion(id));
    }

    /** Un administrador valida manualmente el correo (cuando no llega). Queda en auditoría. */
    @PutMapping("/{id}/verificar-email")
    @PreAuthorize("@authz.can('usuarios','actualizar')")
    public ResponseEntity<MessageResponse> verificarManualmente(@PathVariable Long id) {
        return ResponseEntity.ok(userService.verificarManualmente(id));
    }
    
    /**
     * Obtener módulos accesibles según plan de suscripción
     * GET /api/users/{userId}/modules
     */
    @GetMapping("/{userId}/modules")
    public ResponseEntity<List<ModuloDTO>> getAccessibleModules(
            @PathVariable Long userId,
            @RequestParam Long organizacionId,
            Authentication authentication) {
        // Verificar que el usuario autenticado coincida o sea admin
        Long authenticatedUserId = ((com.saas.platform.user.security.UserPrincipal) authentication.getPrincipal()).getId();
        if (!authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<ModuloDTO> modules = userService.getAccessibleModules(userId, organizacionId);
        return ResponseEntity.ok(modules);
    }
}
