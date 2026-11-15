package com.saas.platform.catalog.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Firma JWT invalida");
        } catch (MalformedJwtException ex) {
            log.error("Token JWT malformado");
        } catch (ExpiredJwtException ex) {
            log.error("Token JWT expirado");
        } catch (UnsupportedJwtException ex) {
            log.error("Token JWT no soportado");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims vacio");
        }
        return false;
    }

    public Claims getAllClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
            .parseSignedClaims(token).getPayload();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaims(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) return ((Integer) userId).longValue();
        if (userId instanceof Long) return (Long) userId;
        if (userId instanceof Number) return ((Number) userId).longValue();
        // fallback al subject
        try { return Long.parseLong(claims.getSubject()); } catch (Exception e) { return null; }
    }

    public String getEmailFromToken(String token) {
        return getAllClaims(token).get("email", String.class);
    }

    /**
     * Devuelve el organizacionId del claim del token, o null si el token no lo
     * incluye (caso actual del user-service). Cuando es null, el catalog-service
     * resuelve la organizacion contra la BD via OrganizacionResolver.
     */
    public Long getOrganizacionIdFromTokenOrNull(String token) {
        Object orgId = getAllClaims(token).get("organizacionId");
        if (orgId instanceof Number) return ((Number) orgId).longValue();
        return null;
    }

    @SuppressWarnings("unchecked")
    /** Claim 'emailVerificado' del token (null si el token no lo trae: tokens anteriores). */
    public Boolean getEmailVerificadoFromToken(String token) {
        try {
            Object v = getAllClaims(token).get("emailVerificado");
            if (v instanceof Boolean b) return b;
            if (v != null) return Boolean.valueOf(String.valueOf(v));
        } catch (Exception ignored) {}
        return null;
    }

    public java.util.List<String> getPermisosFromToken(String token) {
        try {
            Object p = getAllClaims(token).get("permisos");
            if (p instanceof java.util.List<?> l) {
                java.util.List<String> out = new java.util.ArrayList<>();
                for (Object o : l) out.add(String.valueOf(o));
                return out;
            }
        } catch (Exception ignored) {}
        return java.util.Collections.emptyList();
    }
}
