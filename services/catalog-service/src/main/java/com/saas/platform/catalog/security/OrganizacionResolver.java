package com.saas.platform.catalog.security;

import com.saas.platform.catalog.entity.UsuarioOrganizacion;
import com.saas.platform.catalog.exception.InvalidOperationException;
import com.saas.platform.catalog.repository.UsuarioOrganizacionRepository;
import com.saas.platform.catalog.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resuelve a que organizacion (empresa suscrita) pertenece la peticion del
 * usuario autenticado. Como el JWT del user-service NO incluye organizacionId,
 * lo resolvemos contra la BD compartida (gets_shared) a partir del userId.
 *
 * Reglas:
 *  - Si la peticion indica una organizacion activa (header X-Organizacion-Id),
 *    se valida que el usuario pertenezca a ella; si no, se rechaza.
 *  - Si no se indica, se usa usuarios.organizacion_id (organizacion principal).
 *  - Si esta vacia, se usa la primera membresia en usuarios_organizaciones.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizacionResolver {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioOrganizacionRepository usuarioOrganizacionRepository;

    public Long resolve(Long userId, Long organizacionSolicitada) {
        if (userId == null) return null;

        // 1. Si la peticion pide una organizacion concreta, validar pertenencia.
        if (organizacionSolicitada != null) {
            boolean pertenece = usuarioOrganizacionRepository
                    .existsByUsuarioIdAndOrganizacionId(userId, organizacionSolicitada);
            if (!pertenece) {
                // Tambien aceptar si es su organizacion principal en usuarios
                Long principal = usuarioRepository.findById(userId)
                        .map(u -> u.getOrganizacionId()).orElse(null);
                if (!organizacionSolicitada.equals(principal)) {
                    throw new InvalidOperationException(
                        "El usuario no pertenece a la organizacion " + organizacionSolicitada);
                }
            }
            return organizacionSolicitada;
        }

        // 2. Organizacion principal del usuario.
        Long principal = usuarioRepository.findById(userId)
                .map(u -> u.getOrganizacionId()).orElse(null);
        if (principal != null) return principal;

        // 3. Primera membresia disponible.
        List<UsuarioOrganizacion> membresias = usuarioOrganizacionRepository.findByUsuarioId(userId);
        if (!membresias.isEmpty()) return membresias.get(0).getOrganizacionId();

        // 4. Sin organizacion asociada.
        log.warn("Usuario {} no tiene organizacion asociada", userId);
        throw new InvalidOperationException(
            "El usuario no tiene ninguna organizacion asociada");
    }
}
