package com.saas.platform.user.service;

import com.saas.platform.user.dto.*;
import com.saas.platform.user.entity.Perfil;
import com.saas.platform.user.entity.Usuario;
import com.saas.platform.user.exception.EmailAlreadyExistsException;
import com.saas.platform.user.exception.InvalidCredentialsException;
import com.saas.platform.user.exception.UserBlockedException;
import com.saas.platform.user.exception.UserNotFoundException;
import com.saas.platform.user.repository.UsuarioRepository;
import com.saas.platform.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;
    
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final com.saas.platform.user.repository.RolRepository rolRepository;
    private final AuditoriaService auditoria;
    private final VerificacionEmailService verificacion;
    private final MarcaService marcaService;
    
    private static final int CODIGO_EXPIRACION_MINUTOS = 30;
    
    /**
     * Login - Autenticar usuario
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Buscar usuario
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Credenciales inválidas"));
        
        // Verificar si está bloqueado
        if (usuario.estaBloqueado()) {
            throw new UserBlockedException(
                "Cuenta bloqueada por múltiples intentos fallidos. " +
                "Intenta nuevamente en 15 minutos o recupera tu contraseña."
            );
        }
        
        // Verificar contraseña
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            usuario.incrementarIntentosFallidos();
            usuarioRepository.save(usuario);
            throw new InvalidCredentialsException("Credenciales inválidas");
        }
        
        // Verificar que esté activo
        if (!usuario.getActivo()) {
            throw new InvalidCredentialsException("Usuario inactivo");
        }
        
        // Login exitoso - resetear intentos fallidos
        usuario.resetearIntentosFallidos();
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
        
        // Generar tokens
        String accessToken = jwtTokenProvider.generateAccessToken(usuario);
        String refreshToken = jwtTokenProvider.generateRefreshToken(usuario);
        
        log.info("Login exitoso: {}", usuario.getEmail());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .userId(usuario.getId()).nombre(nombreDe(usuario.getId())).apellido(apellidoDe(usuario.getId()))
                .email(usuario.getEmail())
                .emailVerificado(usuario.getEmailVerificado())
                .rol(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .build();
    }
    
    /**
     * Asistente de registro: crea la organización y su usuario PROPIETARIO, y deja la sesión iniciada.
     * La suscripción de prueba la crea luego el subscription-service con el token devuelto.
     */
    @Transactional
    public AuthResponse registrarOrganizacion(RegistroOrganizacionRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email))
            throw new EmailAlreadyExistsException("El email ya está registrado");

        // Organización (slug único a partir del nombre)
        String base = java.text.Normalizer.normalize(req.getOrganizacion().trim().toLowerCase(), java.text.Normalizer.Form.NFD)
            .replaceAll("[\\p{M}]", "").replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "organizacion";
        if (base.length() > 80) base = base.substring(0, 80);
        String slug = base;
        for (int i = 2; ((Number) em.createNativeQuery("SELECT COUNT(*) FROM organizaciones WHERE slug = :s")
                .setParameter("s", slug).getSingleResult()).longValue() > 0; i++) slug = base + "-" + i;
        Long orgId = ((Number) em.createNativeQuery(
                "INSERT INTO organizaciones (nombre, slug, email, telefono, activo) VALUES (:n, :s, :e, :t, TRUE) RETURNING id")
            .setParameter("n", req.getOrganizacion().trim()).setParameter("s", slug)
            .setParameter("e", email).setParameter("t", req.getTelefono())
            .getSingleResult()).longValue();

        // Logo opcional: se valida antes de crear nada (si es inválido, el registro no queda a medias)
        if (req.getLogo() != null && !req.getLogo().isBlank()) marcaService.guardarLogo(orgId, req.getLogo());

        // Quien registra la organización la administra: rol del sistema 'Administrador'
        com.saas.platform.user.entity.Rol propietario = rolRepository.findAll().stream()
            .filter(r -> "Administrador".equalsIgnoreCase(r.getNombre()) && r.getOrganizacionId() == null)
            .findFirst().orElseThrow(() -> new IllegalStateException("No existe el rol del sistema 'Administrador'"));

        Usuario usuario = Usuario.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .activo(true).emailVerificado(false)
                .organizacionId(orgId).rol(propietario)
                .build();
        Perfil perfil = Perfil.builder().usuario(usuario).nombre(req.getNombre().trim())
                .apellido(req.getApellido()).telefono(req.getTelefono()).build();
        usuario.setPerfil(perfil);
        usuario = usuarioRepository.save(usuario);

        // Enlace + código de verificación (un fallo de envío no impide el registro; se puede reenviar)
        verificacion.emitir(usuario);
        if (propietario.getId() != null && rolModulosDe(propietario.getId()) == 0)
            log.warn("El rol 'Administrador' no tiene permisos en roles_modulos: el nuevo usuario no podrá administrar su organización.");

        auditoria.registrar("ORGANIZACION_REGISTRADA", "organizacion", orgId, java.util.Map.of("nombre", req.getOrganizacion().trim(), "propietario", email), orgId);
        log.info("Organización registrada: {} (id {}), propietario {}", req.getOrganizacion(), orgId, email);
        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(usuario))
                .refreshToken(jwtTokenProvider.generateRefreshToken(usuario))
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .userId(usuario.getId()).nombre(nombreDe(usuario.getId())).apellido(apellidoDe(usuario.getId())).email(usuario.getEmail())
                .emailVerificado(usuario.getEmailVerificado())
                .rol(propietario.getNombre())
                .build();
    }

    /**
     * Registrar nuevo usuario
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validar que el email no exista
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("El email ya está registrado");
        }
        
        // Crear usuario
        Usuario usuario = Usuario.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .activo(true)
                .emailVerificado(false)
                .build();
        
        // Crear perfil
        Perfil perfil = Perfil.builder()
                .usuario(usuario)
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .build();
        
        usuario.setPerfil(perfil);
        
        usuario = usuarioRepository.save(usuario);

        // Enlace + código de verificación
        verificacion.emitir(usuario);
        
        // Generar tokens
        String accessToken = jwtTokenProvider.generateAccessToken(usuario);
        String refreshToken = jwtTokenProvider.generateRefreshToken(usuario);
        
        log.info("Usuario registrado: {}", usuario.getEmail());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .userId(usuario.getId()).nombre(nombreDe(usuario.getId())).apellido(apellidoDe(usuario.getId()))
                .email(usuario.getEmail())
                .emailVerificado(false)
                .message("Usuario registrado. Por favor verifica tu email.")
                .build();
    }
    
    /**
     * Refrescar token de acceso
     */
    public AuthResponse refreshToken(String refreshToken) {
        // Validar refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidCredentialsException("Token inválido");
        }
        
        // Obtener usuario del token
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado"));
        
        // Verificar que esté activo
        if (!usuario.getActivo()) {
            throw new InvalidCredentialsException("Usuario inactivo");
        }
        
        // Generar nuevo access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(usuario);
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .userId(usuario.getId()).nombre(nombreDe(usuario.getId())).apellido(apellidoDe(usuario.getId()))
                .email(usuario.getEmail())
                .emailVerificado(usuario.getEmailVerificado())
                .rol(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .build();
    }
    
    /**
     * Validar token
     */
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }
    
    /** Nombre y apellido del perfil (consulta directa: no depende de la carga perezosa del perfil). */
    private String[] perfilDe(Long usuarioId) {
        java.util.List<?> r = em.createNativeQuery("SELECT nombre, apellido FROM perfiles WHERE usuario_id = :u")
            .setParameter("u", usuarioId).getResultList();
        if (r.isEmpty()) return new String[]{ null, null };
        Object[] f = (Object[]) r.get(0);
        return new String[]{ (String) f[0], (String) f[1] };
    }
    private String nombreDe(Long usuarioId) { return perfilDe(usuarioId)[0]; }
    private String apellidoDe(Long usuarioId) { return perfilDe(usuarioId)[1]; }

    private long rolModulosDe(Long rolId) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM roles_modulos WHERE rol_id = :r")
            .setParameter("r", rolId).getSingleResult()).longValue();
    }

    private String generarCodigoVerificacion() {
        SecureRandom random = new SecureRandom();
        int codigo = random.nextInt(900000) + 100000; // 6 dígitos
        return String.valueOf(codigo);
    }
}
