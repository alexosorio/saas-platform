package com.saas.platform.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Mientras el usuario no confirme su correo no puede hacer ninguna operación: solo consultar su
 * propio perfil y la marca de su organización, pedir otro enlace, verificar con código y cerrar sesión.
 * El estado se lee de la base en cada petición (JwtAuthenticationFilter), así que apenas verifica,
 * queda habilitado en este servicio. Catálogo y suscripciones lo leen del token: el frontend lo
 * renueva al verificar.
 * No es @Component a propósito: solo se registra en la cadena de seguridad (SecurityConfig).
 */
public class EmailVerificadoFilter extends OncePerRequestFilter {

    private static final Set<String> PERMITIDOS = Set.of(
        "GET /api/users/me",
        "POST /api/users/me/reenviar-verificacion",
        "POST /api/users/verify-email",
        "POST /api/users/resend-verification",
        "POST /api/users/change-password",
        "GET /api/organizacion/marca",
        "GET /api/parametros/sesion"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p && !p.isEmailVerificado()
                && !permitido(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Correo sin verificar\",\"codigo\":\"EMAIL_NO_VERIFICADO\","
                + "\"message\":\"Confirma tu correo para poder usar la plataforma. Revisa tu bandeja de entrada o pide un enlace nuevo.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean permitido(HttpServletRequest r) {
        String m = r.getMethod();
        String p = r.getRequestURI().substring(r.getContextPath().length());
        if (m.equals("OPTIONS") || p.startsWith("/api/auth/") || p.startsWith("/api/public/")
            || p.startsWith("/actuator") || p.startsWith("/error") || p.startsWith("/v3/api-docs") || p.startsWith("/swagger-ui"))
            return true;
        return PERMITIDOS.contains(m + " " + p);
    }
}
