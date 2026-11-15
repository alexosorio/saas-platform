package com.saas.platform.user.service;

import com.saas.platform.email.Marca;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Marca de cada organización: nombre y logo opcional.
 * El logo llega como data URL (el frontend convierte SVG/WEBP a PNG) y se guarda como PNG o JPEG,
 * los únicos formatos que muestran todos los clientes de correo.
 */
@Service
@RequiredArgsConstructor
public class MarcaService {

    public static final int MAX_BYTES = 300 * 1024;
    private static final Pattern DATA_URL = Pattern.compile("^data:(image/(?:png|jpeg|jpg));base64,([A-Za-z0-9+/=\\s]+)$");

    @PersistenceContext private EntityManager em;
    private final AuditoriaService auditoria;

    public record Logo(byte[] datos, String tipo, LocalDateTime actualizado) {}

    // ── Lectura ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<Logo> logo(Long orgId) {
        if (orgId == null) return Optional.empty();
        List<?> r = em.createNativeQuery("SELECT logo, logo_tipo, logo_actualizado FROM organizaciones WHERE id = :o AND logo IS NOT NULL")
            .setParameter("o", orgId).getResultList();
        if (r.isEmpty()) return Optional.empty();
        Object[] f = (Object[]) r.get(0);
        LocalDateTime act = f[2] instanceof java.sql.Timestamp t ? t.toLocalDateTime()
            : f[2] instanceof LocalDateTime l ? l : null;
        return Optional.of(new Logo((byte[]) f[0], (String) f[1], act));
    }

    @Transactional(readOnly = true)
    public String nombre(Long orgId) {
        if (orgId == null) return null;
        List<?> r = em.createNativeQuery("SELECT nombre FROM organizaciones WHERE id = :o").setParameter("o", orgId).getResultList();
        return r.isEmpty() ? null : (String) r.get(0);
    }

    /** Marca para correos de una organización. */
    @Transactional(readOnly = true)
    public Marca marcaDe(Long orgId) {
        String nombre = nombre(orgId);
        return logo(orgId).map(l -> new Marca(nombre, l.datos(), l.tipo())).orElse(Marca.sinLogo(nombre));
    }

    /** Marca de la organización a la que pertenece un usuario (por correo). Null si no tiene. */
    @Transactional(readOnly = true)
    public Marca marcaDeUsuario(String email) {
        if (email == null) return null;
        List<?> r = em.createNativeQuery("SELECT organizacion_id FROM usuarios WHERE LOWER(email) = LOWER(:e)")
            .setParameter("e", email.trim()).getResultList();
        if (r.isEmpty() || r.get(0) == null) return null;
        return marcaDe(((Number) r.get(0)).longValue());
    }

    /** Datos para el menú de la aplicación: nombre y logo como data URL (o null). */
    @Transactional(readOnly = true)
    public Map<String, Object> resumen(Long orgId) {
        Optional<Logo> l = logo(orgId);
        java.util.HashMap<String, Object> m = new java.util.HashMap<>();
        m.put("organizacionId", orgId);
        m.put("nombre", nombre(orgId));
        m.put("logo", l.map(x -> "data:" + x.tipo() + ";base64," + Base64.getEncoder().encodeToString(x.datos())).orElse(null));
        m.put("logoActualizado", l.map(Logo::actualizado).orElse(null));
        return m;
    }

    // ── Escritura ───────────────────────────────────────────────────────

    /** Valida y decodifica un data URL PNG/JPEG. */
    public Logo decodificar(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se recibió la imagen del logo");
        Matcher m = DATA_URL.matcher(dataUrl.trim());
        if (!m.matches())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El logo debe ser una imagen PNG o JPEG");
        byte[] datos;
        try { datos = Base64.getMimeDecoder().decode(m.group(2)); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen del logo está dañada"); }
        if (datos.length == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen del logo está vacía");
        if (datos.length > MAX_BYTES)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El logo no puede pesar más de " + (MAX_BYTES / 1024) + " KB");
        // El contenido real debe coincidir con el tipo declarado (no basta con la cabecera del data URL)
        boolean png = datos.length > 8 && (datos[0] & 0xFF) == 0x89 && datos[1] == 'P' && datos[2] == 'N' && datos[3] == 'G';
        boolean jpg = datos.length > 3 && (datos[0] & 0xFF) == 0xFF && (datos[1] & 0xFF) == 0xD8 && (datos[2] & 0xFF) == 0xFF;
        if (!png && !jpg) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no es una imagen PNG o JPEG válida");
        return new Logo(datos, png ? "image/png" : "image/jpeg", LocalDateTime.now());
    }

    @Transactional
    public void guardarLogo(Long orgId, String dataUrl) {
        Logo l = decodificar(dataUrl);
        em.createNativeQuery("UPDATE organizaciones SET logo = :d, logo_tipo = :t, logo_actualizado = :a, updated_at = now() WHERE id = :o")
            .setParameter("d", l.datos()).setParameter("t", l.tipo()).setParameter("a", l.actualizado())
            .setParameter("o", orgId).executeUpdate();
        auditoria.registrar("LOGO_ACTUALIZADO", "organizacion", orgId, Map.of("bytes", l.datos().length, "tipo", l.tipo()), orgId);
    }

    @Transactional
    public void quitarLogo(Long orgId) {
        em.createNativeQuery("UPDATE organizaciones SET logo = NULL, logo_tipo = NULL, logo_actualizado = now(), updated_at = now() WHERE id = :o")
            .setParameter("o", orgId).executeUpdate();
        auditoria.registrar("LOGO_ELIMINADO", "organizacion", orgId, Map.of(), orgId);
    }
}
