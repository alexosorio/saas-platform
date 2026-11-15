package com.saas.platform.user.service;

import com.saas.platform.user.dto.*;
import com.saas.platform.user.entity.Perfil;
import com.saas.platform.user.entity.Usuario;
import com.saas.platform.user.repository.UsuarioRepository;
import com.saas.platform.user.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UsuarioRepository usuarioRepository;
    private final com.saas.platform.user.repository.RolRepository rolRepository;
    private final RolService rolService;
    private final com.saas.platform.user.security.ContextoActor actor;
    private final AuditoriaService auditoria;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ModuleLoadingService moduleLoadingService;
    private final VerificacionEmailService verificacion;
    
    private static final int CODIGO_LENGTH = 6;
    private static final int CODIGO_EXPIRACION_MINUTOS = 30;
    private static final int TOKEN_EXPIRACION_HORAS = 1;
    
    /**
     * Crear nuevo usuario
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request, Long adminId) {
        // Validar que el email no exista
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("El email ya está registrado");
        }
        
        // Crear usuario
        // Organización: la indicada, o la del administrador que lo crea (multi-tenant)
        // Un administrador de organización solo crea usuarios en SU organización; la plataforma puede elegir.
        Long orgId = actor.esPlataforma() ? request.getOrganizacionId() : null;
        if (orgId == null && adminId != null) {
            orgId = usuarioRepository.findById(adminId).map(Usuario::getOrganizacionId).orElse(null);
        }

        Usuario usuario = Usuario.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .activo(request.getActivo() == null || request.getActivo())
                .emailVerificado(false)
                .organizacionId(orgId)
                .build();

        // Rol asignado desde la interfaz
        if (request.getRolId() != null) {
            rolService.exigirAsignable(request.getRolId());
            rolService.exigirPlataforma(request.getRolId(), null);
            usuario.setRol(rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Rol no encontrado")));
        }

        // Crear perfil
        Perfil perfil = Perfil.builder()
                .usuario(usuario)
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .telefono(request.getTelefono())
                .build();
        
        usuario.setPerfil(perfil);
        
        usuario = usuarioRepository.save(usuario);

        // Enlace + código de verificación
        verificacion.emitir(usuario);
        
        log.info("Usuario creado: {}", usuario.getEmail());
        auditoria.registrar("USUARIO_CREADO", "usuario", usuario.getId(), java.util.Map.of("email", usuario.getEmail(), "rol", usuario.getRol() != null ? usuario.getRol().getNombre() : "-"));
        return mapToResponse(usuario);
    }
    
    /**
     * Obtener usuario por ID
     */
    public UserResponse getUser(Long id) {
        Usuario usuario = findUsuarioEnAlcance(id);
        return mapToResponse(usuario);
    }
    
    /**
     * Obtener todos los usuarios
     */
    public List<UserResponse> getAllUsers() {
        List<Usuario> usuarios = actor.esPlataforma()
                ? usuarioRepository.findAll()
                : usuarioRepository.findByOrganizacionId(actor.organizacionId());
        return usuarios.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Actualizar usuario
     */
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        findUsuarioEnAlcance(id);
        usuarioRepository.findById(id).ifPresent(u ->
            rolService.exigirPlataforma(u.getRol() != null ? u.getRol().getId() : null, null));
        Usuario usuario = findUsuarioById(id);
        Perfil perfil = usuario.getPerfil();
        
        if (request.getNombre() != null) perfil.setNombre(request.getNombre());
        if (request.getApellido() != null) perfil.setApellido(request.getApellido());
        if (request.getTelefono() != null) perfil.setTelefono(request.getTelefono());
        if (request.getAvatarUrl() != null) perfil.setAvatarUrl(request.getAvatarUrl());
        if (request.getIdioma() != null) perfil.setIdioma(request.getIdioma());
        if (request.getZonaHoraria() != null) perfil.setZonaHoraria(request.getZonaHoraria());

        if (request.getActivo() != null) usuario.setActivo(request.getActivo());

        // Asignación de rol desde la interfaz
        if (request.getRolId() != null) {
            rolService.exigirAsignable(request.getRolId());
            rolService.exigirPlataforma(request.getRolId(), null);
            usuario.setRol(rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Rol no encontrado")));
        }

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario actualizado: {}", usuario.getEmail());
        
        auditoria.registrar("USUARIO_ACTUALIZADO", "usuario", id, java.util.Map.of("rolId", String.valueOf(request.getRolId()), "activo", String.valueOf(request.getActivo())));
        return mapToResponse(usuario);
    }
    
    /**
     * Desactivar usuario (soft delete)
     */
    @Transactional
    public MessageResponse deactivateUser(Long id) {
        Usuario usuario = findUsuarioParaGestion(id, "desactivar");
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        
        log.info("Usuario desactivado: {}", usuario.getEmail());
        auditoria.registrar("USUARIO_DESACTIVADO", "usuario", id, java.util.Map.of("email", usuario.getEmail()));
        return new MessageResponse("Usuario desactivado exitosamente", true);
    }
    
    /**
     * Activar usuario
     */
    @Transactional
    public MessageResponse activateUser(Long id) {
        Usuario usuario = findUsuarioParaGestion(id, null);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        
        log.info("Usuario activado: {}", usuario.getEmail());
        auditoria.registrar("USUARIO_ACTIVADO", "usuario", id, java.util.Map.of("email", usuario.getEmail()));
        return new MessageResponse("Usuario activado exitosamente", true);
    }
    
    /**
     * Eliminar usuario permanentemente
     */
    @Transactional
    public MessageResponse deleteUser(Long id) {
        Usuario usuario = findUsuarioParaGestion(id, "eliminar");
        usuarioRepository.delete(usuario);
        
        log.info("Usuario eliminado permanentemente: {}", usuario.getEmail());
        auditoria.registrar("USUARIO_ELIMINADO", "usuario", id, java.util.Map.of("email", usuario.getEmail()));
        return new MessageResponse("Usuario eliminado exitosamente", true);
    }
    
    /**
     * Cambiar contraseña (usuario autenticado)
     */
    @Transactional
    public MessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        Usuario usuario = findUsuarioById(userId);
        
        // Verificar contraseña actual
        if (!passwordEncoder.matches(request.getCurrentPassword(), usuario.getPasswordHash())) {
            throw new InvalidPasswordException("Contraseña actual incorrecta");
        }
        
        // Actualizar contraseña
        usuario.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        usuario.setUltimoCambioPassword(LocalDateTime.now());
        usuarioRepository.save(usuario);
        
        // Enviar notificación
        emailService.enviarNotificacionCambioPassword(
            usuario.getEmail(), 
            usuario.getPerfil().getNombre()
        );
        
        log.info("Contraseña cambiada para: {}", usuario.getEmail());
        return new MessageResponse("Contraseña cambiada exitosamente", true);
    }
    
    /**
     * Solicitar recuperación de contraseña (envía código por email)
     */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        
        // Generar código de recuperación
        String codigo = generarCodigoVerificacion();
        usuario.setCodigoVerificacion(codigo);
        usuario.setCodigoVerificacionExpira(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);
        
        // Enviar código por email
        emailService.enviarCodigoRecuperacion(
            usuario.getEmail(), 
            usuario.getPerfil().getNombre(), 
            codigo
        );
        
        log.info("Código de recuperación enviado a: {}", usuario.getEmail());
        return new MessageResponse("Código de recuperación enviado a tu email", true);
    }
    
    /**
     * Solicitar enlace de recuperación (envía token por email)
     */
    @Transactional
    public MessageResponse requestPasswordResetLink(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        
        // Generar token único
        String token = UUID.randomUUID().toString();
        usuario.setTokenRecuperacion(token);
        usuario.setTokenRecuperacionExpira(LocalDateTime.now().plusHours(TOKEN_EXPIRACION_HORAS));
        usuarioRepository.save(usuario);
        
        // Enviar enlace por email
        emailService.enviarEnlaceRecuperacion(
            usuario.getEmail(), 
            usuario.getPerfil().getNombre(), 
            token
        );
        
        log.info("Enlace de recuperación enviado a: {}", usuario.getEmail());
        return new MessageResponse("Enlace de recuperación enviado a tu email", true);
    }
    
    /**
     * Resetear contraseña con código
     */
    @Transactional
    public MessageResponse resetPasswordWithCode(ResetPasswordWithCodeRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        
        // Validar código
        if (!usuario.codigoVerificacionValido(request.getCode())) {
            throw new InvalidVerificationCodeException("Código inválido o expirado");
        }
        
        // Actualizar contraseña
        usuario.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
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
        return new MessageResponse("Contraseña actualizada exitosamente", true);
    }
    
    /**
     * Resetear contraseña con token (enlace)
     */
    @Transactional
    public MessageResponse resetPasswordWithToken(ResetPasswordWithTokenRequest request) {
        Usuario usuario = usuarioRepository.findByTokenRecuperacion(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Token inválido o expirado"));
        
        // Validar token
        if (!usuario.tokenRecuperacionValido(request.getToken())) {
            throw new InvalidTokenException("Token inválido o expirado");
        }
        
        // Actualizar contraseña
        usuario.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
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
        
        log.info("Contraseña reseteada con token para: {}", usuario.getEmail());
        return new MessageResponse("Contraseña actualizada exitosamente", true);
    }
    
    /**
     * Verificar email con código
     */
    @Transactional
    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        
        // Validar código
        if (!usuario.codigoVerificacionValido(request.getCode())) {
            throw new InvalidVerificationCodeException("Código inválido o expirado");
        }
        
        // Marcar verificado (invalida enlace y código, envía bienvenida, auditoría)
        verificacion.marcarVerificado(usuario, "codigo");
        
        log.info("Email verificado para: {}", usuario.getEmail());
        return new MessageResponse("Email verificado exitosamente", true);
    }
    
    /**
     * Reenviar código de verificación
     */
    @Transactional
    public MessageResponse resendVerificationCode(String email) {
        return verificacion.reenviarPublico(email, null);
    }

    /** Reenvío del enlace por el propio usuario con sesión iniciada. */
    @Transactional
    public MessageResponse reenviarVerificacionPropia() {
        return verificacion.reenviar(findUsuarioById(actor.id()), "propio");
    }

    /** Reenvío del enlace por un administrador a un usuario de su organización. */
    @Transactional
    public MessageResponse reenviarVerificacion(Long id) {
        Usuario u = findUsuarioParaGestion(id, null);
        return verificacion.reenviar(u, "administrador");
    }

    /** Verificación manual por un administrador (p. ej. el correo nunca llega). No sobre la propia cuenta. */
    @Transactional
    public MessageResponse verificarManualmente(Long id) {
        Usuario u = findUsuarioParaGestion(id, "verificar manualmente");
        verificacion.marcarVerificado(u, "manual");
        return new MessageResponse("Correo de " + u.getEmail() + " marcado como verificado", true);
    }
    
    /**
     * Obtener módulos accesibles según el plan de suscripción
     */
    public List<ModuloDTO> getAccessibleModules(Long userId, Long organizacionId) {
        Usuario usuario = findUsuarioById(userId);
        
        if (!usuario.getEmailVerificado()) {
            throw new EmailNotVerifiedException("Debes verificar tu email primero");
        }
        
        if (!usuario.getActivo()) {
            throw new UserInactiveException("Usuario inactivo");
        }
        
        // Delegar al servicio de carga de módulos
        return moduleLoadingService.getModulosAccesibles(userId, organizacionId);
    }
    
    // ========================================
    // MÉTODOS AUXILIARES
    // ========================================
    
    private Usuario findUsuarioById(Long id) {
        return usuarioRepository.findByIdWithPerfil(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
    }
    
    private String generarCodigoVerificacion() {
        SecureRandom random = new SecureRandom();
        int codigo = random.nextInt(900000) + 100000; // 6 dígitos
        return String.valueOf(codigo);
    }
    

    /** Usuario visible para quien hace la petición: de su organización (la plataforma ve todos). */
    private Usuario findUsuarioEnAlcance(Long id) {
        Usuario u = findUsuarioById(id);
        if (!actor.esPlataforma() && !java.util.Objects.equals(u.getOrganizacionId(), actor.organizacionId())) {
            // "No encontrado" y no "prohibido": no se revela que el id existe en otra organización
            throw new UserNotFoundException("Usuario no encontrado");
        }
        return u;
    }

    /** Para activar/desactivar/eliminar: en alcance, no un usuario de plataforma (salvo la plataforma) y no uno mismo. */
    private Usuario findUsuarioParaGestion(Long id, String accionSobreSiMismo) {
        Usuario u = findUsuarioEnAlcance(id);
        rolService.exigirPlataforma(u.getRol() != null ? u.getRol().getId() : null, null);
        if (accionSobreSiMismo != null && id.equals(actor.id()))
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "No puedes " + accionSobreSiMismo + " tu propia cuenta");
        return u;
    }

    private UserResponse mapToResponse(Usuario usuario) {
        Perfil perfil = usuario.getPerfil();
        return UserResponse.builder()
                .id(usuario.getId())
                .email(usuario.getEmail())
                .activo(usuario.getActivo())
                .emailVerificado(usuario.getEmailVerificado())
                .nombre(perfil != null ? perfil.getNombre() : null)
                .apellido(perfil != null ? perfil.getApellido() : null)
                .telefono(perfil != null ? perfil.getTelefono() : null)
                .avatarUrl(perfil != null ? perfil.getAvatarUrl() : null)
                .idioma(perfil != null ? perfil.getIdioma() : null)
                .zonaHoraria(perfil != null ? perfil.getZonaHoraria() : null)
                .ultimoAcceso(usuario.getUltimoAcceso())
                .createdAt(usuario.getCreatedAt())
                .organizacionId(usuario.getOrganizacionId())
                .rol(usuario.getRol() != null
                        ? UserResponse.RolInfo.builder().id(usuario.getRol().getId()).nombre(usuario.getRol().getNombre()).build()
                        : null)
                .build();
    }
}
