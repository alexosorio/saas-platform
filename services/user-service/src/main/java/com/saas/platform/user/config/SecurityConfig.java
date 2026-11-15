package com.saas.platform.user.config;

import com.saas.platform.user.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Orígenes permitidos (CORS). Lista separada por comas desde CORS_ORIGINS.
     * Producción: CORS_ORIGINS=https://app.tudominio.com   ·   Local: sin definir = "*".
     */
    @Value("${app.cors.origins:*}")
    private List<String> corsOrigins;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configurando Security Filter Chain");
        
        http
            // Deshabilitar CSRF (stateless API)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configurar CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Stateless session (no sesiones en servidor)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configurar autorización
            .authorizeHttpRequests(auth -> {
                log.info("Configurando reglas de autorización");
                auth
                    // ===================================
                    // ENDPOINTS PÚBLICOS (sin autenticación)
                    // ===================================
                    
                    // Autenticación
                    .requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/api/auth/register").permitAll()
                    .requestMatchers("/api/auth/registro-organizacion").permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/public/**").permitAll()
                    .requestMatchers("/api/auth/refresh").permitAll()
                    
                    // Verificación de email
                    .requestMatchers("/api/users/verify-email").permitAll()
                    .requestMatchers("/api/users/resend-verification").permitAll()
                    .requestMatchers("/api/auth/verificar-email").permitAll()
                    .requestMatchers("/api/auth/reenviar-verificacion").permitAll()
                    
                    // Recuperación de contraseña
                    .requestMatchers("/api/users/forgot-password").permitAll()
                    .requestMatchers("/api/users/request-password-reset").permitAll()
                    .requestMatchers("/api/users/reset-password-code").permitAll()
                    .requestMatchers("/api/users/reset-password-token").permitAll()
                    
                    // Actuator (health checks)
                    .requestMatchers("/actuator/**").permitAll()
                    
                    // Swagger/OpenAPI (documentación)
                    .requestMatchers("/v3/api-docs/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/swagger-ui.html").permitAll()
                    
                    // Error handling
                    .requestMatchers("/error").permitAll()
                    
                    // ===================================
                    // TODOS LOS DEMÁS ENDPOINTS REQUIEREN AUTENTICACIÓN
                    // ===================================
                    .anyRequest().authenticated();
                
                log.info("Reglas de autorización configuradas exitosamente");
            })
            
            // Agregar filtro JWT antes del filtro de autenticación por defecto
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Sin correo confirmado: ninguna operación salvo las imprescindibles
            .addFilterAfter(new com.saas.platform.user.security.EmailVerificadoFilter(), UsernamePasswordAuthenticationFilter.class)
            
            // Manejo de excepciones personalizado
            .exceptionHandling(ex -> {
                ex.authenticationEntryPoint((request, response, authException) -> {
                    log.error("Error de autenticación: {} para URL: {}", 
                        authException.getMessage(), request.getRequestURI());
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"No autenticado\",\"message\":\"" 
                        + authException.getMessage() + "\"}");
                });
                
                ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.error("Acceso denegado: {} para URL: {}", 
                        accessDeniedException.getMessage(), request.getRequestURI());
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Acceso denegado\",\"message\":\"" 
                        + accessDeniedException.getMessage() + "\"}");
                });
            });
        
        log.info("Security Filter Chain configurado exitosamente");
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configurando CORS");
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Orígenes permitidos: CORS_ORIGINS (en producción, solo el dominio del frontend)
        List<String> origenes = origenesPermitidos();
        configuration.setAllowedOriginPatterns(origenes);
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
        ));
        
        // Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Requested-With"
        ));
        
        // Headers expuestos
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type"
        ));
        
        // Permitir credenciales
        configuration.setAllowCredentials(true);
        
        // Tiempo de cache para preflight requests
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("CORS configurado: Origins={}, Methods=GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD", origenes);
        return source;
    }

    /** Limpia la lista de CORS_ORIGINS (espacios, entradas vacías); si queda vacía, acepta cualquiera. */
    private List<String> origenesPermitidos() {
        List<String> limpios = corsOrigins == null ? List.of() : corsOrigins.stream()
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList();
        return limpios.isEmpty() ? List.of("*") : limpios;
    }
}
