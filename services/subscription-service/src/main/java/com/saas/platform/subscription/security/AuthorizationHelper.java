package com.saas.platform.subscription.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Autorización dinámica por permisos (modulo:accion) que viajan en el JWT. */
@Component("authz")
public class AuthorizationHelper {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;
    public boolean can(String modulo, String accion) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        String exact = modulo + ":" + accion, modWild = modulo + ":*", accWild = "*:" + accion;
        return auth.getAuthorities().stream().map(a -> a.getAuthority()).anyMatch(a ->
            a.equals(exact) || a.equals(modWild) || a.equals(accWild) || a.equals("*:*"));
    }

    /**
     * Permiso de PLATAFORMA (administrar suscripciones de todas las organizaciones).
     * Exige el permiso explícito plataforma_suscripciones:<accion>; el comodín '*' NO lo concede,
     * para que el administrador de una organización no acceda a las de las demás.
     */
    public boolean plataforma(String accion) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        String requerido = "plataforma_suscripciones:" + accion;
        if (auth.getAuthorities().stream().noneMatch(a -> requerido.equals(a.getAuthority()))) return false;
        // Además del permiso, el usuario debe pertenecer a la SÚPER ORGANIZACIÓN
        if (!(auth.getPrincipal() instanceof UserPrincipal p) || p.getOrganizacionId() == null) return false;
        var r = em.createNativeQuery("SELECT es_plataforma FROM organizaciones WHERE id = :o")
            .setParameter("o", p.getOrganizacionId()).getResultList();
        return !r.isEmpty() && Boolean.TRUE.equals(r.get(0));
    }
}
