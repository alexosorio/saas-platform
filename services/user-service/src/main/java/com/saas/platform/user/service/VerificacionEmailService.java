package com.saas.platform.user.service;

import com.saas.platform.user.dto.MessageResponse;
import com.saas.platform.user.entity.Usuario;
import com.saas.platform.user.exception.EmailAlreadyVerifiedException;
import com.saas.platform.user.exception.InvalidTokenException;
import com.saas.platform.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Verificación del correo de un usuario.
 * Cada envío genera DOS credenciales de un solo uso:
 *  - un ENLACE (token aleatorio, vigencia configurable 'seguridad.horas_enlace_verificacion', 48 h por defecto)
 *  - un CÓDIGO de 6 dígitos (30 min) por si el enlace no abre o se prefiere escribirlo.
 * Reenvío público (por correo o por un enlace vencido), del propio usuario o de un administrador,
 * con una espera mínima entre envíos; un administrador también puede verificar manualmente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificacionEmailService {

    public static final String PARAM_HORAS = "seguridad.horas_enlace_verificacion";
    private static final int HORAS_POR_DEFECTO = 48;
    private static final int MINUTOS_CODIGO = 30;
    private static final Duration ESPERA_REENVIO = Duration.ofSeconds(60);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final ParametroService parametros;
    private final AuditoriaService auditoria;

    @Value("${app.url}")
    private String appUrl;

    // ── Emisión ─────────────────────────────────────────────────────────

    /** Genera enlace + código, los guarda y envía el correo. No valida la espera entre envíos. */
    @Transactional
    public void emitir(Usuario u) {
        int horas = parametros.entero(PARAM_HORAS, null, HORAS_POR_DEFECTO);
        String token = nuevoToken();
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime ahora = LocalDateTime.now();

        u.setTokenVerificacion(token);
        u.setTokenVerificacionExpira(ahora.plusHours(horas));
        u.setCodigoVerificacion(codigo);
        u.setCodigoVerificacionExpira(ahora.plusMinutes(MINUTOS_CODIGO));
        u.setVerificacionEnviadaEn(ahora);
        usuarioRepository.save(u);

        String enlace = appUrl.replaceAll("/+$", "") + "/verificar-email?token="
            + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String nombre = u.getPerfil() != null && u.getPerfil().getNombre() != null ? u.getPerfil().getNombre() : u.getEmail();
        // El envío es asíncrono y no debe tumbar el registro si el proveedor falla
        try { emailService.enviarVerificacion(u.getEmail(), nombre, enlace, horas, codigo, MINUTOS_CODIGO); }
        catch (Exception e) { log.warn("No se pudo enviar la verificación a {}: {}", u.getEmail(), e.getMessage()); }
        log.info("Verificación de correo enviada a {} (enlace válido {} h)", u.getEmail(), horas);
    }

    // ── Reenvíos ────────────────────────────────────────────────────────

    /**
     * Reenvío público desde la pantalla de verificación: por correo o por un enlace (vencido) recibido.
     * Respuesta siempre genérica: no revela si el correo existe ni si ya estaba verificado.
     */
    @Transactional
    public MessageResponse reenviarPublico(String email, String token) {
        Optional<Usuario> u = Optional.empty();
        if (token != null && !token.isBlank()) u = usuarioRepository.findByTokenVerificacion(token.trim());
        if (u.isEmpty() && email != null && !email.isBlank()) u = usuarioRepository.findByEmail(email.trim().toLowerCase());
        u.filter(x -> !Boolean.TRUE.equals(x.getEmailVerificado()) && Boolean.TRUE.equals(x.getActivo()))
         .filter(this::puedeReenviar)
         .ifPresent(x -> {
             emitir(x);
             auditoria.registrar("VERIFICACION_REENVIADA", "usuario", x.getId(), Map.of("origen", "publico"), x.getOrganizacionId());
         });
        return new MessageResponse(
            "Si el correo está registrado y pendiente de verificación, te enviamos un nuevo enlace. Revisa también la carpeta de spam.", true);
    }

    /** Reenvío pedido por el propio usuario (sesión iniciada) o por un administrador (ya validado su alcance). */
    @Transactional
    public MessageResponse reenviar(Usuario u, String origen) {
        if (Boolean.TRUE.equals(u.getEmailVerificado()))
            throw new EmailAlreadyVerifiedException("El correo " + u.getEmail() + " ya está verificado");
        if (!puedeReenviar(u)) {
            long seg = Math.max(1, ESPERA_REENVIO.minus(Duration.between(u.getVerificacionEnviadaEn(), LocalDateTime.now())).toSeconds());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Ya se envió un enlace hace un momento. Intenta de nuevo en " + seg + " segundos.");
        }
        emitir(u);
        auditoria.registrar("VERIFICACION_REENVIADA", "usuario", u.getId(), Map.of("origen", origen, "email", u.getEmail()), u.getOrganizacionId());
        return new MessageResponse("Enviamos un nuevo enlace de verificación a " + u.getEmail(), true);
    }

    // ── Verificación ────────────────────────────────────────────────────

    /** Verifica con el enlace. Distingue enlace inválido/usado de enlace vencido (este último se puede reenviar). */
    @Transactional
    public MessageResponse verificarConEnlace(String token) {
        if (token == null || token.isBlank()) throw new InvalidTokenException("Enlace de verificación inválido");
        Usuario u = usuarioRepository.findByTokenVerificacion(token.trim())
            .orElseThrow(() -> new InvalidTokenException("El enlace no es válido o ya fue usado. Si tu correo ya está verificado, inicia sesión."));
        if (u.getTokenVerificacionExpira() == null || u.getTokenVerificacionExpira().isBefore(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.GONE, "El enlace de verificación expiró. Solicita uno nuevo.");
        marcarVerificado(u, "enlace");
        return new MessageResponse("¡Listo! Tu correo " + u.getEmail() + " quedó verificado.", true);
    }

    /** Marca el correo como verificado, invalida enlace y código y envía la bienvenida. */
    @Transactional
    public void marcarVerificado(Usuario u, String origen) {
        if (Boolean.TRUE.equals(u.getEmailVerificado()))
            throw new EmailAlreadyVerifiedException("El correo " + u.getEmail() + " ya está verificado");
        u.setEmailVerificado(true);
        u.setTokenVerificacion(null);
        u.setTokenVerificacionExpira(null);
        u.setCodigoVerificacion(null);
        u.setCodigoVerificacionExpira(null);
        usuarioRepository.save(u);
        auditoria.registrar("manual".equals(origen) ? "EMAIL_VERIFICADO_MANUAL" : "EMAIL_VERIFICADO",
            "usuario", u.getId(), Map.of("email", u.getEmail(), "origen", origen), u.getOrganizacionId());
        String nombre = u.getPerfil() != null && u.getPerfil().getNombre() != null ? u.getPerfil().getNombre() : u.getEmail();
        try { emailService.enviarBienvenida(u.getEmail(), nombre); }
        catch (Exception e) { log.warn("No se pudo enviar la bienvenida a {}: {}", u.getEmail(), e.getMessage()); }
        log.info("Correo verificado ({}) para {}", origen, u.getEmail());
    }

    private boolean puedeReenviar(Usuario u) {
        return u.getVerificacionEnviadaEn() == null
            || u.getVerificacionEnviadaEn().plus(ESPERA_REENVIO).isBefore(LocalDateTime.now());
    }

    private static String nuevoToken() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
