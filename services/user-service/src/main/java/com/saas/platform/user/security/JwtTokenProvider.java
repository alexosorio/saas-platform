package com.saas.platform.user.security;

import com.saas.platform.user.entity.Usuario;
import com.saas.platform.user.entity.RolModulo;
import com.saas.platform.user.repository.RolModuloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtTokenProvider {

    @Autowired
    private RolModuloRepository rolModuloRepository;
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration; // En milisegundos (ej: 3600000 = 1 hora)
    
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration; // En milisegundos (ej: 604800000 = 7 días)
    
    /**
     * Generar access token (corta duración)
     */
    public String generateAccessToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", usuario.getId());
        claims.put("email", usuario.getEmail());
        claims.put("emailVerificado", usuario.getEmailVerificado());

        // Permisos dinámicos (modulo:accion) desde roles_modulos -> viajan en el token
        if (usuario.getRol() != null) {
            claims.put("rol", usuario.getRol().getNombre());
            List<String> permisos = new ArrayList<>();
            for (RolModulo rm : rolModuloRepository.findByRolId(usuario.getRol().getId())) {
                String m = rm.codigoEfectivo();
                if (m == null || m.isBlank()) continue;
                if (Boolean.TRUE.equals(rm.getCrear()))      permisos.add(m + ":crear");
                if (Boolean.TRUE.equals(rm.getLeer()))       permisos.add(m + ":leer");
                if (Boolean.TRUE.equals(rm.getActualizar())) permisos.add(m + ":actualizar");
                if (Boolean.TRUE.equals(rm.getEliminar()))   permisos.add(m + ":eliminar");
            }
            claims.put("permisos", permisos);
        }

        return createToken(claims, usuario.getId().toString(), accessTokenExpiration);
    }
    
    /**
     * Generar refresh token (larga duración)
     */
    public String generateRefreshToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", usuario.getId());
        claims.put("type", "refresh");
        
        return createToken(claims, usuario.getId().toString(), refreshTokenExpiration);
    }
    
    /**
     * Crear token JWT
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Obtener clave de firma
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Obtener user ID del token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }
    
    /**
     * Obtener email del token
     */
    public String getEmailFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("email", String.class);
    }
    
    /**
     * Validar token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Firma JWT inválida");
        } catch (MalformedJwtException ex) {
            log.error("Token JWT malformado");
        } catch (ExpiredJwtException ex) {
            log.error("Token JWT expirado");
        } catch (UnsupportedJwtException ex) {
            log.error("Token JWT no soportado");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string vacío");
        }
        return false;
    }
    
    /**
     * Obtener todos los claims del token
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Obtener tiempo de expiración del access token
     */
    public Long getAccessTokenExpiration() {
        return accessTokenExpiration / 1000; // Retornar en segundos
    }
}
