package com.saas.platform.subscription.service;

import com.saas.platform.notifications.NotificationMessage;
import com.saas.platform.notifications.NotificationPublisher;
import com.saas.platform.subscription.entity.Suscripcion;
import com.saas.platform.subscription.repository.SuscripcionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Correos a los administradores de cada organización:
 *  - ANTES del vencimiento, los días configurados en 'suscripcion.dias_aviso_vencimiento' (7,3,1,0 por defecto);
 *  - cuando la suscripción se SUSPENDE por vencimiento.
 * Cada aviso se registra en avisos_suscripcion para no repetirlo (aunque el proceso corra cada hora).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvisosSuscripcionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.of("es", "CO"));

    private final SuscripcionRepository suscripcionRepo;
    private final ParametrosLector parametros;
    private final NotificationPublisher notificaciones;
    @PersistenceContext private EntityManager em;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    @Value("${app.suscripciones.dias-aviso:7,3,1,0}")
    private String diasAvisoPorDefecto;

    /** Días antes del vencimiento en que se avisa (p. ej. [7, 3, 1, 0]). */
    public SortedSet<Integer> diasAviso() {
        SortedSet<Integer> out = new TreeSet<>(Comparator.reverseOrder());
        for (String p : parametros.texto("suscripcion.dias_aviso_vencimiento", null, diasAvisoPorDefecto).split("[,;\\s]+")) {
            try { int d = Integer.parseInt(p.trim()); if (d >= 0 && d <= 60) out.add(d); } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    /** Envía los avisos previos que correspondan hoy. Devuelve cuántos se enviaron. */
    @Transactional
    public int enviarAvisosPrevios() {
        SortedSet<Integer> dias = diasAviso();
        if (dias.isEmpty()) return 0;
        LocalDate hoy = LocalDate.now();
        int enviados = 0;
        for (Suscripcion s : suscripcionRepo.findAll()) {
            if (!VencimientoService.vigente(s) || s.getFechaFin() == null) continue;
            long faltan = ChronoUnit.DAYS.between(hoy, s.getFechaFin());
            if (faltan < 0) continue;
            // El aviso que toca es el umbral más cercano ya alcanzado (si el proceso no corrió un día, no se pierde)
            Integer umbral = dias.stream().filter(d -> faltan <= d).min(Integer::compare).orElse(null);
            if (umbral == null || yaEnviado(s, "previo", umbral)) continue;
            if (enviar(s, "previo", umbral, asuntoPrevio(s, faltan), contenidoPrevio(s, faltan))) enviados++;
        }
        if (enviados > 0) log.info("Avisos de vencimiento enviados: {}", enviados);
        return enviados;
    }

    /** Aviso de suspensión por vencimiento (una vez por vencimiento). */
    @Transactional(propagation = Propagation.REQUIRED)
    public void avisarSuspension(Suscripcion s) {
        if (s.getFechaFin() == null || yaEnviado(s, "suspendida", 0)) return;
        String plan = s.getPlan() != null ? s.getPlan().getNombre() : "tu plan";
        String contenido = "<p>Tu suscripción al plan <strong>" + esc(plan) + "</strong> venció el <strong>"
            + s.getFechaFin().format(FECHA) + "</strong> y no registramos el pago, por lo que el acceso a la plataforma "
            + "quedó <strong>suspendido</strong>.</p>"
            + "<p>Tus datos están seguros. Apenas registres y se valide el pago, el acceso se habilita de nuevo.</p>"
            + boton("Registrar el pago", appUrl + "/subscription/payments");
        enviar(s, "suspendida", 0, "Tu acceso fue suspendido por falta de pago", contenido);
    }

    // ── internos ────────────────────────────────────────────────────────

    private String asuntoPrevio(Suscripcion s, long faltan) {
        return faltan == 0 ? "Tu suscripción vence hoy"
            : faltan == 1 ? "Tu suscripción vence mañana"
            : "Tu suscripción vence en " + faltan + " días";
    }

    private String contenidoPrevio(Suscripcion s, long faltan) {
        String plan = s.getPlan() != null ? s.getPlan().getNombre() : "tu plan";
        BigDecimal valor = s.getPlan() == null ? null
            : s.getPeriodo() == Suscripcion.Periodo.anual ? s.getPlan().getPrecioAnual() : s.getPlan().getPrecioMensual();
        String cuando = faltan == 0 ? "<strong>vence hoy</strong>"
            : "vence el <strong>" + s.getFechaFin().format(FECHA) + "</strong> (" + (faltan == 1 ? "mañana" : "en " + faltan + " días") + ")";
        String prueba = s.getEstado() == Suscripcion.Estado.periodo_prueba ? " de prueba" : "";
        return "<p>Tu suscripción" + prueba + " al plan <strong>" + esc(plan) + "</strong> " + cuando + ".</p>"
            + (valor != null && valor.signum() > 0
                ? "<p>Valor del período (" + s.getPeriodo() + "): <strong>$" + String.format(Locale.of("es", "CO"), "%,.0f", valor) + "</strong></p>" : "")
            + "<p>Para no perder el acceso, registra el pago antes de esa fecha. "
            + "<strong>Si vence sin pago, el acceso se bloquea automáticamente.</strong></p>"
            + boton("Pagar ahora", appUrl + "/subscription/payments");
    }

    private boolean yaEnviado(Suscripcion s, String tipo, int dias) {
        Number n = (Number) em.createNativeQuery("SELECT COUNT(*) FROM avisos_suscripcion WHERE suscripcion_id = :s "
                + "AND tipo = :t AND fecha_fin = :f AND dias_antes = :d")
            .setParameter("s", s.getId()).setParameter("t", tipo).setParameter("f", s.getFechaFin()).setParameter("d", dias)
            .getSingleResult();
        return n.longValue() > 0;
    }

    /** Publica un correo por destinatario y registra el aviso. */
    private boolean enviar(Suscripcion s, String tipo, int dias, String asunto, String contenido) {
        List<String> para = destinatarios(s.getOrganizacionId());
        if (para.isEmpty()) {
            log.warn("Suscripción {}: no hay administradores con correo para avisar ({})", s.getId(), tipo);
            return false;
        }
        int encolados = 0;
        for (String email : para) {
            try { notificaciones.publicar(NotificationMessage.email(email, asunto, contenido)); encolados++; }
            catch (Exception e) { log.error("No se pudo encolar el aviso '{}' para {}: {}", tipo, email, e.getMessage()); }
        }
        // Si la cola no estaba disponible no se marca como enviado: se reintenta en la próxima corrida
        if (encolados == 0) return false;
        em.createNativeQuery("INSERT INTO avisos_suscripcion (suscripcion_id, organizacion_id, tipo, fecha_fin, dias_antes, destinatarios) "
                + "VALUES (:s, :o, :t, :f, :d, :p) ON CONFLICT DO NOTHING")
            .setParameter("s", s.getId()).setParameter("o", s.getOrganizacionId()).setParameter("t", tipo)
            .setParameter("f", s.getFechaFin()).setParameter("d", dias).setParameter("p", String.join(", ", para))
            .executeUpdate();
        return true;
    }

    /** Administradores activos de la organización (rol con acceso total o con el módulo de suscripciones) y su correo de contacto. */
    @SuppressWarnings("unchecked")
    private List<String> destinatarios(Long orgId) {
        List<Object> r = em.createNativeQuery(
                "SELECT DISTINCT LOWER(u.email) FROM usuarios u JOIN roles r ON r.id = u.rol_id "
              + "WHERE u.organizacion_id = :o AND u.activo = TRUE AND u.email IS NOT NULL AND ("
              + "  r.nombre IN ('Administrador', 'Propietario') "
              + "  OR EXISTS (SELECT 1 FROM roles_modulos rm WHERE rm.rol_id = r.id AND rm.modulo_codigo IN ('*', 'suscripciones', 'facturacion')))")
            .setParameter("o", orgId).getResultList();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        r.forEach(x -> out.add(String.valueOf(x)));
        List<Object> org = em.createNativeQuery("SELECT LOWER(email) FROM organizaciones WHERE id = :o AND email IS NOT NULL")
            .setParameter("o", orgId).getResultList();
        org.forEach(x -> out.add(String.valueOf(x)));
        out.removeIf(e -> e.isBlank() || !e.contains("@"));
        return new ArrayList<>(out);
    }

    private static String boton(String texto, String url) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" align=\"center\" style=\"margin:18px auto;\"><tr>"
            + "<td style=\"border-radius:8px;background:#a871bd;\"><a href=\"" + esc(url) + "\" target=\"_blank\" "
            + "style=\"display:inline-block;padding:13px 30px;font-size:15px;font-weight:bold;color:#ffffff;text-decoration:none;border-radius:8px;\">"
            + esc(texto) + "</a></td></tr></table>";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
