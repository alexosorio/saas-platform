package com.saas.platform.user.security;

import com.saas.platform.user.entity.Usuario;
import com.saas.platform.user.repository.UsuarioRepository;
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
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final com.saas.platform.user.repository.RolModuloRepository rolModuloRepository;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                
                // Cargar usuario de la base de datos
                Usuario usuario = usuarioRepository.findById(userId).orElse(null);
                
                if (usuario != null && usuario.getActivo()) {
                    // Construir roles
                    List<String> roles = new ArrayList<>();

                    // Agregar rol del usuario si existe
                    if (usuario.getRol() != null) {
                        roles.add(usuario.getRol().getNombre().toUpperCase());
                    } else {
                        // Rol por defecto
                        roles.add("USUARIO");
                    }

                    // Autoridades: ROLE_<nombre> + permisos granulares (modulo:accion) desde roles_modulos
                    java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new ArrayList<>();
                    for (String r : roles) authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + r));
                    if (usuario.getRol() != null) {
                        for (com.saas.platform.user.entity.RolModulo rm : rolModuloRepository.findByRolId(usuario.getRol().getId())) {
                            String mod = rm.codigoEfectivo();
                            if (mod == null || mod.isBlank()) continue;
                            if (Boolean.TRUE.equals(rm.getCrear()))      authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(mod + ":crear"));
                            if (Boolean.TRUE.equals(rm.getLeer()))       authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(mod + ":leer"));
                            if (Boolean.TRUE.equals(rm.getActualizar())) authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(mod + ":actualizar"));
                            if (Boolean.TRUE.equals(rm.getEliminar()))   authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(mod + ":eliminar"));
                        }
                    }

                    // Crear UserPrincipal
                    UserPrincipal userPrincipal = UserPrincipal.builder()
                        .id(usuario.getId())
                        .email(usuario.getEmail())
                        .password(usuario.getPasswordHash())
                        .activo(usuario.getActivo())
                        .emailVerificado(usuario.getEmailVerificado())
                        .roles(roles)
                        .build();

                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userPrincipal,
                            null,
                            authorities
                        );
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Usuario autenticado: {} con roles: {}", usuario.getEmail(), roles);
                }
            }
        } catch (Exception ex) {
            log.error("No se pudo establecer la autenticación del usuario", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extraer JWT del header Authorization
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
