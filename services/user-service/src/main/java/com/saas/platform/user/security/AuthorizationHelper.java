package com.saas.platform.user.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Autorización dinámica basada en permisos (modulo:accion) cargados desde la BD
 * (roles_modulos). Sin nombres de rol hardcodeados: quién tiene cada permiso es dato.
 * Uso en endpoints:  @PreAuthorize("@authz.can('usuarios','leer')")
 * Comodines: un rol con módulo '*' y acción concede '*:accion' (super-admin data-driven).
 */
@Component("authz")
public class AuthorizationHelper {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    public boolean can(String modulo, String accion) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        String exact = modulo + ":" + accion;
        String modWild = modulo + ":*";
        String accWild = "*:" + accion;
        return auth.getAuthorities().stream().map(a -> a.getAuthority()).anyMatch(a ->
            a.equals(exact) || a.equals(modWild) || a.equals(accWild) || a.equals("*:*"));
    }

    /** ¿El usuario tiene algún permiso de PLATAFORMA explícito? (el comodín '*' no cuenta) */
    public boolean esPlataforma() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        boolean permiso = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().startsWith("plataforma_"));
        if (!permiso || !(auth.getPrincipal() instanceof UserPrincipal p)) return false;
        // Además del permiso, el usuario debe pertenecer a la SÚPER ORGANIZACIÓN
        var r = em.createNativeQuery("SELECT o.es_plataforma FROM usuarios u JOIN organizaciones o ON o.id = u.organizacion_id WHERE u.id = :u")
            .setParameter("u", p.getId()).getResultList();
        return !r.isEmpty() && Boolean.TRUE.equals(r.get(0));
    }
}
