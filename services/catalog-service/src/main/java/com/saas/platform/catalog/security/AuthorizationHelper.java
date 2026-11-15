package com.saas.platform.catalog.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Autorización dinámica por permisos (modulo:accion) que viajan en el JWT. */
@Component("authz")
public class AuthorizationHelper {
    public boolean can(String modulo, String accion) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        String exact = modulo + ":" + accion, modWild = modulo + ":*", accWild = "*:" + accion;
        return auth.getAuthorities().stream().map(a -> a.getAuthority()).anyMatch(a ->
            a.equals(exact) || a.equals(modWild) || a.equals(accWild) || a.equals("*:*"));
    }
}
