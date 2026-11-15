package com.saas.platform.catalog.service;

import com.saas.platform.catalog.entity.Inventario;
import com.saas.platform.catalog.entity.Unidad;
import com.saas.platform.catalog.exception.InvalidOperationException;
import com.saas.platform.catalog.exception.ResourceNotFoundException;
import com.saas.platform.catalog.repository.InventarioRepository;
import com.saas.platform.catalog.security.AuthorizationHelper;
import com.saas.platform.catalog.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Desde qué bodegas puede VENDER cada usuario.
 *  - Usuario con bodegas asignadas y SIN el permiso 'ventas_otra_bodega': solo sus bodegas.
 *  - Con el permiso (o el comodín '*'): cualquier bodega de la organización.
 *  - Sin bodegas asignadas: sin restricción (compatibilidad); la regla se activa al asignarle bodegas.
 */
@Service
@RequiredArgsConstructor
public class BodegaAccesoService {

    public static final String PERMISO_OTRA_BODEGA = "ventas_otra_bodega";

    private final InventarioRepository inventarioRepo;
    private final AuthorizationHelper authz;
    private final AuditoriaService auditoria;
    @PersistenceContext private EntityManager em;

    // ── Regla de venta ──────────────────────────────────────────────────

    /** Bodegas desde las que puede vender quien hace la petición; null = sin restricción. */
    @Transactional(readOnly = true)
    public Set<Long> bodegasPermitidas() {
        Long usuarioId = actorId();
        if (usuarioId == null || authz.can(PERMISO_OTRA_BODEGA, "crear")) return null;
        Set<Long> propias = asignadas(usuarioId);
        return propias.isEmpty() ? null : propias;
    }

    /** ¿La unidad tiene existencias disponibles en alguna de las bodegas indicadas? */
    public boolean disponibleEn(Long unidadId, Set<Long> bodegas) {
        if (bodegas == null) return true;
        return inventarioRepo.findByUnidadId(unidadId).stream()
            .anyMatch(i -> bodegas.contains(i.getBodega().getId()) && i.getDisponible() > 0);
    }

    /** Lanza error si quien vende no puede tomar esta unidad por estar en otra bodega. */
    @Transactional(readOnly = true)
    public void validarVenta(Unidad u) {
        Set<Long> permitidas = bodegasPermitidas();
        if (permitidas == null || disponibleEn(u.getId(), permitidas)) return;
        String donde = inventarioRepo.findByUnidadId(u.getId()).stream()
            .filter(i -> i.getDisponible() > 0).map(i -> i.getBodega().getNombre())
            .distinct().collect(Collectors.joining(", "));
        throw new InvalidOperationException("La unidad " + u.getCodigoUnico() + " está en "
            + (donde.isBlank() ? "otra bodega" : "la bodega " + donde)
            + ". No tienes permiso para vender productos de otras bodegas.");
    }

    /** Existencias de la unidad, primero las de las bodegas del usuario (para descontar de ahí). */
    @Transactional(readOnly = true)
    public List<Inventario> inventariosPreferidos(Long unidadId) {
        return inventariosPreferidos(unidadId, null);
    }

    /**
     * Existencias de la unidad en orden de preferencia para descontar:
     * 1) bodegas de la sede de la orden, 2) bodegas asignadas al usuario, 3) el resto.
     */
    @Transactional(readOnly = true)
    public List<Inventario> inventariosPreferidos(Long unidadId, Long sedeOrden) {
        Set<Long> propias = Optional.ofNullable(actorId()).map(this::asignadas).orElse(Set.of());
        List<Inventario> l = new ArrayList<>(inventarioRepo.findByUnidadId(unidadId));
        l.sort(Comparator.comparing((Inventario i) ->
            sedeOrden != null && sedeOrden.equals(i.getBodega().getSedeId()) ? 0
                : propias.contains(i.getBodega().getId()) ? 1 : 2));
        return l;
    }

    // ── Sede de la orden ────────────────────────────────────────────────

    /**
     * Sede con la que arranca una orden nueva y si el usuario puede cambiarla.
     *  - sedeId          : sede de las bodegas asignadas al usuario (o la única sede activa de la org).
     *  - puedeCambiar    : true si tiene el permiso 'ventas_otra_bodega' o no tiene bodegas asignadas.
     *  - sedesPermitidas : sedes que puede elegir; null = cualquier sede activa de la organización.
     */
    public record ContextoVenta(Long sedeId, String sedeNombre, boolean puedeCambiar, List<Long> sedesPermitidas) {}

    @Transactional(readOnly = true)
    public ContextoVenta contextoVenta(Long orgId) {
        Long usuarioId = actorId();
        List<Long> propias = usuarioId == null ? List.of() : sedesDelUsuario(usuarioId, orgId);
        boolean conPermiso = authz.can(PERMISO_OTRA_BODEGA, "crear");
        boolean sinRestriccion = conPermiso || propias.isEmpty();

        Long sede = propias.isEmpty() ? null : propias.get(0);
        if (sede == null) {
            List<Long> activas = sedesActivasDeOrg(orgId);
            if (activas.size() == 1) sede = activas.get(0);
        }
        // Puede cambiar si tiene permiso, si no tiene bodegas asignadas, o si sus bodegas están en varias sedes
        boolean puedeCambiar = sinRestriccion || propias.size() > 1;
        return new ContextoVenta(sede, sede == null ? null : nombreSede(sede), puedeCambiar,
            sinRestriccion ? null : propias);
    }

    /** La sede de la orden debe ser de la organización, estar activa y, sin permiso, ser una de las del usuario. */
    @Transactional(readOnly = true)
    public void validarSedeOrden(Long sedeId, Long orgId) {
        if (sedeId == null || !sedesActivasDeOrg(orgId).contains(sedeId))
            throw new InvalidOperationException("La sede de la orden no existe o está inactiva");
        Long usuarioId = actorId();
        if (usuarioId == null || authz.can(PERMISO_OTRA_BODEGA, "crear")) return;
        List<Long> propias = sedesDelUsuario(usuarioId, orgId);
        if (!propias.isEmpty() && !propias.contains(sedeId))
            throw new InvalidOperationException("Solo puedes facturar en tu sede (" + nombreSede(propias.get(0))
                + "). Para hacerlo en otra necesitas el permiso de vender desde otras bodegas.");
    }

    /** Sede en la que está disponible la unidad (la primera con existencias), para mostrarla al vender. */
    @Transactional(readOnly = true)
    public Optional<Inventario> ubicacion(Long unidadId, Long sedePreferida) {
        return inventariosPreferidos(unidadId, sedePreferida).stream()
            .filter(i -> i.getDisponible() > 0).findFirst();
    }

    @Transactional(readOnly = true)
    public String nombreSede(Long sedeId) {
        List<?> r = em.createNativeQuery("SELECT nombre FROM sedes WHERE id = :s").setParameter("s", sedeId).getResultList();
        return r.isEmpty() ? null : String.valueOf(r.get(0));
    }

    /** Sedes de las bodegas asignadas al usuario (solo activas, de su organización). */
    @SuppressWarnings("unchecked")
    private List<Long> sedesDelUsuario(Long usuarioId, Long orgId) {
        List<Object> r = em.createNativeQuery(
                "SELECT s.id FROM usuarios_bodegas ub JOIN bodegas b ON b.id = ub.bodega_id " +
                "JOIN sedes s ON s.id = b.sede_id JOIN ciudades_organizaciones co ON co.id = s.ciudad_organizacion_id " +
                "WHERE ub.usuario_id = :u AND co.organizacion_id = :o AND s.activo = true " +
                "GROUP BY s.id ORDER BY COUNT(*) DESC, s.id")
            .setParameter("u", usuarioId).setParameter("o", orgId).getResultList();
        return r.stream().map(o -> ((Number) o).longValue()).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Long> sedesActivasDeOrg(Long orgId) {
        List<Object> r = em.createNativeQuery(
                "SELECT s.id FROM sedes s JOIN ciudades_organizaciones co ON co.id = s.ciudad_organizacion_id " +
                "WHERE co.organizacion_id = :o AND s.activo = true ORDER BY s.id")
            .setParameter("o", orgId).getResultList();
        return r.stream().map(o -> ((Number) o).longValue()).toList();
    }

    // ── Asignación de bodegas a usuarios ─────────────────────────────────

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Set<Long> asignadas(Long usuarioId) {
        List<Object> r = em.createNativeQuery("SELECT bodega_id FROM usuarios_bodegas WHERE usuario_id = :u")
            .setParameter("u", usuarioId).getResultList();
        return r.stream().map(o -> ((Number) o).longValue()).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<Long> asignadasDe(Long usuarioId, Long orgId) {
        validarUsuarioDeOrg(usuarioId, orgId);
        return new ArrayList<>(asignadas(usuarioId));
    }

    @Transactional
    public void asignar(Long usuarioId, List<Long> bodegaIds, Long orgId) {
        validarUsuarioDeOrg(usuarioId, orgId);
        Set<Long> deOrg = bodegasDeOrg(orgId);
        List<Long> ids = bodegaIds == null ? List.of() : bodegaIds.stream().filter(Objects::nonNull).distinct().toList();
        for (Long b : ids)
            if (!deOrg.contains(b)) throw new ResourceNotFoundException("Bodega no encontrada: " + b);
        em.createNativeQuery("DELETE FROM usuarios_bodegas WHERE usuario_id = :u").setParameter("u", usuarioId).executeUpdate();
        for (Long b : ids)
            em.createNativeQuery("INSERT INTO usuarios_bodegas (usuario_id, bodega_id) VALUES (:u, :b)")
                .setParameter("u", usuarioId).setParameter("b", b).executeUpdate();
        auditoria.registrar("BODEGAS_ASIGNADAS", "usuario", usuarioId, java.util.Map.of("bodegas", ids.toString()));
    }

    private void validarUsuarioDeOrg(Long usuarioId, Long orgId) {
        long n = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM usuarios WHERE id = :u AND organizacion_id = :o")
            .setParameter("u", usuarioId).setParameter("o", orgId).getSingleResult()).longValue();
        if (n == 0) throw new ResourceNotFoundException("Usuario no encontrado");
    }

    @SuppressWarnings("unchecked")
    private Set<Long> bodegasDeOrg(Long orgId) {
        List<Object> r = em.createNativeQuery(
                "SELECT b.id FROM bodegas b JOIN sedes s ON s.id = b.sede_id " +
                "JOIN ciudades_organizaciones co ON co.id = s.ciudad_organizacion_id WHERE co.organizacion_id = :o")
            .setParameter("o", orgId).getResultList();
        return r.stream().map(o -> ((Number) o).longValue()).collect(Collectors.toSet());
    }

    private Long actorId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.getPrincipal() instanceof UserPrincipal p) ? p.getId() : null;
    }
}
