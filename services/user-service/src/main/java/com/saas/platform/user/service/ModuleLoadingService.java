package com.saas.platform.user.service;

import com.saas.platform.user.dto.ModuloDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModuleLoadingService {
    
    private final RestTemplate restTemplate;
    
    @Value("${services.authorization.url}")
    private String authorizationServiceUrl;
    
    @Value("${services.authorization.token:}")
    private String serviceToken;
    
    /**
     * Obtener módulos accesibles para un usuario en una organización
     * Consulta al servicio de autorización
     * 
     * @param userId ID del usuario
     * @param organizacionId ID de la organización
     * @return Lista de módulos accesibles
     */
    public List<ModuloDTO> getModulosAccesibles(Long userId, Long organizacionId) {
        try {
            String url = String.format(
                "%s/api/authorization/modules?userId=%d&organizacionId=%d",
                authorizationServiceUrl,
                userId,
                organizacionId
            );
            
            log.info("Consultando módulos accesibles para usuario: {} en organización: {}", 
                userId, organizacionId);
            
            // Crear headers con token de servicio si está configurado
            HttpHeaders headers = new HttpHeaders();
            if (serviceToken != null && !serviceToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + serviceToken);
                log.debug("Usando service token para autenticación entre microservicios");
            } else {
                log.warn("No hay service token configurado. La llamada podría fallar si el servicio de autorización requiere autenticación.");
            }
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<ModuloDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<ModuloDTO>>() {}
            );
            
            List<ModuloDTO> modulos = response.getBody();
            log.info("Módulos cargados: {}", modulos != null ? modulos.size() : 0);
            
            return modulos != null ? modulos : Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Error consultando servicio de autorización: {}", e.getMessage(), e);
            
            // En caso de error, retornar lista vacía
            // En producción, podrías querer lanzar una excepción o retornar un fallback
            return Collections.emptyList();
        }
    }
    
    /**
     * Verificar si el servicio de autorización está disponible
     */
    public boolean isAuthorizationServiceAvailable() {
        try {
            String healthUrl = authorizationServiceUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Servicio de autorización no disponible: {}", e.getMessage());
            return false;
        }
    }
}
