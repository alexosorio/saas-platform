package com.saas.platform.catalog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final OrganizacionResolver organizacionResolver;
    private final SuscripcionVigente suscripcionVigente;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean correoSinVerificar = false;
        Long orgActiva = null;
        try {
            String jwt = getJwtFromRequest(request);
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                String email = jwtTokenProvider.getEmailFromToken(jwt);

                if (userId != null) {
                    // Organizacion activa: 1) claim del token si existe,
                    // 2) header X-Organizacion-Id (multi-empresa), 3) resuelta por BD.
                    Long orgDelToken = jwtTokenProvider.getOrganizacionIdFromTokenOrNull(jwt);
                    Long orgSolicitada = orgDelToken != null ? orgDelToken : parseOrgHeader(request);
                    Long organizacionId = organizacionResolver.resolve(userId, orgSolicitada);

                    UserPrincipal principal = UserPrincipal.builder()
                        .id(userId).email(email).organizacionId(organizacionId).build();

                    java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();
                    for (String perm : jwtTokenProvider.getPermisosFromToken(jwt))
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(perm));
                    authorities.addAll(principal.getAuthorities());
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    correoSinVerificar = Boolean.FALSE.equals(jwtTokenProvider.getEmailVerificadoFromToken(jwt));
                    orgActiva = organizacionId;
                }
            }
        } catch (Exception ex) {
            log.error("No se pudo autenticar: {}", ex.getMessage());
        }
        // Sin correo confirmado no se permite ninguna operación (salvo lo imprescindible)
        if (correoSinVerificar && !permitidoSinVerificar(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Correo sin verificar\",\"codigo\":\"EMAIL_NO_VERIFICADO\","
                + "\"message\":\"Confirma tu correo para poder usar la plataforma. Revisa tu bandeja de entrada o pide un enlace nuevo.\"}");
            return;
        }
        // Suscripción vencida o suspendida: bloqueo inmediato de toda operación
        if (orgActiva != null && !permitidoSinVerificar(request) && !suscripcionVigente.puedeOperar(orgActiva)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Suscripción inactiva\",\"codigo\":\"SUSCRIPCION_INACTIVA\","
                + "\"message\":\"La suscripción de tu organización venció o está suspendida. Registra el pago para seguir usando la plataforma.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Rutas que un usuario con el correo sin confirmar sí puede usar. */
    private boolean permitidoSinVerificar(HttpServletRequest request) {
        String m = request.getMethod();
        String p = request.getRequestURI().substring(request.getContextPath().length());
        if (m.equals("OPTIONS") || p.startsWith("/api/public/") || p.startsWith("/actuator")
            || p.startsWith("/error") || p.startsWith("/v3/api-docs") || p.startsWith("/swagger-ui")) return true;
        return false;
    }

    private Long parseOrgHeader(HttpServletRequest request) {
        String h = request.getHeader("X-Organizacion-Id");
        if (StringUtils.hasText(h)) {
            try { return Long.parseLong(h.trim()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) return bearer.substring(7);
        return null;
    }
}
