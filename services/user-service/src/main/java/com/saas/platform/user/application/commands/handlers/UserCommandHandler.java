package com.saas.platform.user.application.commands.handlers;

import com.saas.platform.user.application.commands.*;
import com.saas.platform.user.application.commands.dto.CommandResult;
import com.saas.platform.user.domain.model.Usuario;
import com.saas.platform.user.domain.model.Perfil;
import com.saas.platform.user.domain.repository.UsuarioRepository;
import com.saas.platform.user.infrastructure.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handler centralizado para todos los comandos de usuario
 * Implementa el patrón CQRS - Command Side
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCommandHandler {
    
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    private static final int CODIGO_EXPIRACION_MINUTOS = 30;
    private static final int TOKEN_EXPIRACION_HORAS = 1;
    
    /**
     * Handler para CreateUserCommand
     */
    @Transactional
    public CommandResult handle(CreateUserCommand command) {
        log.info("Procesando CreateUserCommand para email: {}", command.getEmail());
        
        // Validar que el email no exista
        if (usuarioRepository.existsByEmail(command.getEmail())) {
            return CommandResult.failure("El email ya está registrado");
        }
        
        // Crear usuario
        Usuario usuario = Usuario.builder()
                .email(command.getEmail())
                .passwordHash(passwordEncoder.encode(command.getPassword()))
                .activo(true)
                .emailVerificado(false)
                .build();
        
        // Crear perfil
        Perfil perfil = Perfil.builder()
                .usuario(usuario)
                .nombre(command.getNombre())
                .apellido(command.getApellido())
                .telefono(command.getTelefono())
                .build();
        
        usuario.setPerfil(perfil);
        
        // Generar código de verificación
        String codigo = generarCodigoVerificacion();
        usuario.setCodigoVerificacion(codigo);
        usuario.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(CODIGO_EXPIRACION_MINUTOS));
        
        usuario = usuarioRepository.save(usuario);
        
        // Enviar email de verificación (async)
        emailService.enviarCodigoVerificacion(
            usuario.getEmail(), 
            perfil.getNombre(), 
            codigo
        );
        
        log.info("Usuario creado exitosamente: {}", usuario.getId());
        return CommandResult.success("Usuario creado exitosamente", usuario.getId());
    }
    
    /**
     * Handler para UpdateUserCommand
     */
    @Transactional
    public CommandResult handle(UpdateUserCommand command) {
        log.info("Procesando UpdateUserCommand para usuario: {}", command.getUserId());
        
        Usuario usuario = findUsuarioById(command.getUserId());
        Perfil perfil = usuario.getPerfil();
        
        if (command.getNombre() != null) perfil.setNombre(command.getNombre());
        if (command.getApellido() != null) perfil.setApellido(command.getApellido());
        if (command.getTelefono() != null) perfil.setTelefono(command.getTelefono());
        if (command.getAvatarUrl() != null) perfil.setAvatarUrl(command.getAvatarUrl());
        if (command.getIdioma() != null) perfil.setIdioma(command.getIdioma());
        if (command.getZonaHoraria() != null) perfil.setZonaHoraria(command.getZonaHoraria());
        
        usuarioRepository.save(usuario);
        
        log.info("Usuario actualizado exitosamente: {}", usuario.getId());
        return CommandResult.success("Usuario actualizado exitosamente");
    }
    
    /**
     * Handler para ChangePasswordCommand
     */
    @Transactional
    public CommandResult handle(ChangePasswordCommand command) {
        log.info("Procesando ChangePasswordCommand para usuario: {}", command.getUserId());
        
        Usuario usuario = findUsuarioById(command.getUserId());
        
        // Verificar contraseña actual
        if (!passwordEncoder.matches(command.getCurrentPassword(), usuario.getPasswordHash())) {
            return CommandResult.failure("Contraseña actual incorrecta");
        }
        
        // Actualizar contraseña
        usuario.setPasswordHash(passwordEncoder.encode(command.getNewPassword()));
        usuario.setUltimoCambioPassword(LocalDateTime.now());
        usuarioRepository.save(usuario);
        
        // Enviar notificación
        emailService.enviarNotificacionCambioPassword(
            usuario.getEmail(), 
            usuario.getPerfil().getNombre()
        );
        
        log.info("Contraseña cambiada exitosamente para usuario: {}", usuario.getId());
        return CommandResult.success("Contraseña cambiada exitosamente");
    }
    
    /**
     * Handler para DeactivateUserCommand
     */
    @Transactional
    public CommandResult handle(DeactivateUserCommand command) {
        log.info("Procesando DeactivateUserCommand para usuario: {}", command.getUserId());
        
        Usuario usuario = findUsuarioById(command.getUserId());
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        
        log.info("Usuario desactivado: {}", usuario.getId());
        return CommandResult.success("Usuario desactivado exitosamente");
    }
    
    /**
     * Handler para ActivateUserCommand
     */
    @Transactional
    public CommandResult handle(ActivateUserCommand command) {
        log.info("Procesando ActivateUserCommand para usuario: {}", command.getUserId());
        
        Usuario usuario = findUsuarioById(command.getUserId());
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        
        log.info("Usuario activado: {}", usuario.getId());
        return CommandResult.success("Usuario activado exitosamente");
    }
    
    /**
     * Handler para DeleteUserCommand
     */
    @Transactional
    public CommandResult handle(DeleteUserCommand command) {
        log.info("Procesando DeleteUserCommand para usuario: {}", command.getUserId());
        
        Usuario usuario = findUsuarioById(command.getUserId());
        usuarioRepository.delete(usuario);
        
        log.info("Usuario eliminado: {}", command.getUserId());
        return CommandResult.success("Usuario eliminado exitosamente");
    }
    
    /**
     * Handler para VerifyEmailCommand
     */
    @Transactional
    public CommandResult handle(VerifyEmailCommand command) {
        log.info("Procesando VerifyEmailCommand para email: {}", command.getEmail());
        
        Usuario usuario = usuarioRepository.findByEmail(command.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Validar código
        if (!usuario.codigoVerificacionValido(command.getCode())) {
            return CommandResult.failure("Código inválido o expirado");
        }
        
        // Marcar email como verificado
        usuario.setEmailVerificado(true);
        usuario.setCodigoVerificacion(null);
        usuario.setCodigoVerificacionExpira(null);
        usuarioRepository.save(usuario);
        
        // Enviar bienvenida
        emailService.enviarBienvenida(
            usuario.getEmail(), 
            usuario.getPerfil().getNombre()
        );
        
        log.info("Email verificado para: {}", usuario.getEmail());
        return CommandResult.success("Email verificado exitosamente");
    }
    
    /**
     * Handler para RequestPasswordRecoveryCommand
     */
    @Transactional
    public CommandResult handle(RequestPasswordRecoveryCommand command) {
        log.info("Procesando RequestPasswordRecoveryCommand para email: {}", command.getEmail());
        
        Usuario usuario = usuarioRepository.findByEmail(command.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Generar código
        String codigo = generarCodigoVerificacion();
        usuario.setCodigoVerificacion(codigo);
        usuario.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);
        
        // Enviar código
        emailService.enviarCodigoRecuperacion(
            usuario.getEmail(), 
            usuario.getPerfil().getNombre(), 
            codigo
        );
        
        log.info("Código de recuperación enviado a: {}", usuario.getEmail());
        return CommandResult.success("Código de recuperación enviado a tu email");
    }
    
    /**
     * Handler para ResetPasswordWithCodeCommand
     */
    @Transactional
    public CommandResult handle(ResetPasswordWithCodeCommand command) {
        log.info("Procesando ResetPasswordWithCodeCommand para email: {}", command.getEmail());
        
        Usuario usuario = usuarioRepository.findByEmail(command.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Validar código
        if (!usuario.codigoVerificacionValido(command.getCode())) {
            return CommandResult.failure("Código inválido o expirado");
        }
        
        // Actualizar contraseña
        usuario.setPasswordHash(passwordEncoder.encode(command.getNewPassword()));
        usuario.setCodigoVerificacion(null);
        usuario.setCodigoVerificacionExpira(null);
        usuario.setUltimoCambioPassword(LocalDateTime.now());
        usuario.resetearIntentosFallidos();
        usuarioRepository.save(usuario);
        
        // Enviar notificación
        emailService.enviarNotificacionCambioPassword(
            usuario.getEmail(), 
            usuario.getPerfil().getNombre()
        );
        
        log.info("Contraseña reseteada con código para: {}", usuario.getEmail());
        return CommandResult.success("Contraseña actualizada exitosamente");
    }
    
    /**
     * Handler para ResetPasswordWithTokenCommand
     */
    @Transactional
    public CommandResult handle(ResetPasswordWithTokenCommand command) {
        log.info("Procesando ResetPasswordWithTokenCommand");
        
        Usuario usuario = usuarioRepository.findByTokenRecuperacion(command.getToken())
                .orElseThrow(() -> new RuntimeException("Token inválido"));
        
        // Validar token
        if (!usuario.tokenRecuperacionValido(command.getToken())) {
            return CommandResult.failure("Token inválido o expirado");
        }
        
        // Actualizar contraseña
        usuario.setPasswordHash(passwordEncoder.encode(command.getNewPassword()));
        usuario.setTokenRecuperacion(null);
        usuario.setTokenRecuperacionExpira(null);
        usuario.setUltimoCambioPassword(LocalDateTime.now());
        usuario.resetearIntentosFallidos();
        usuarioRepository.save(usuario);
        
        // Enviar notificación
        emailService.enviarNotificacionCambioPassword(
            usuario.getEmail(), 
            usuario.getPerfil().getNombre()
        );
        
        log.info("Contraseña reseteada con token");
        return CommandResult.success("Contraseña actualizada exitosamente");
    }
    
    /**
     * Handler para ResendVerificationCodeCommand
     */
    @Transactional
    public CommandResult handle(ResendVerificationCodeCommand command) {
        log.info("Procesando ResendVerificationCodeCommand para email: {}", command.getEmail());
        
        Usuario usuario = usuarioRepository.findByEmail(command.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (usuario.getEmailVerificado()) {
            return CommandResult.failure("El email ya está verificado");
        }
        
        // Generar nuevo código
        String codigo = generarCodigoVerificacion();
        usuario.setCodigoVerificacion(codigo);
        usuario.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(CODIGO_EXPIRACION_MINUTOS));
        usuarioRepository.save(usuario);
        
        // Enviar código
        emailService.enviarCodigoVerificacion(
            usuario.getEmail(), 
            usuario.getPerfil().getNombre(), 
            codigo
        );
        
        log.info("Código de verificación reenviado a: {}", usuario.getEmail());
        return CommandResult.success("Código de verificación enviado");
    }
    
    // ========================================
    // MÉTODOS AUXILIARES
    // ========================================
    
    private Usuario findUsuarioById(Long id) {
        return usuarioRepository.findByIdWithPerfil(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
    
    private String generarCodigoVerificacion() {
        SecureRandom random = new SecureRandom();
        int codigo = random.nextInt(900000) + 100000;
        return String.valueOf(codigo);
    }
}
