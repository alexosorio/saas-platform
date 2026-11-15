package com.saas.platform.user.security;

import com.saas.platform.user.entity.Usuario;
import com.saas.platform.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Quién hace la petición y a qué organización pertenece (base del aislamiento entre organizaciones). */
@Component
@RequiredArgsConstructor
public class ContextoActor {

    private final UsuarioRepository usuarioRepository;
    private final AuthorizationHelper authz;

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getPrincipal() instanceof UserPrincipal p) ? p.getId() : null;
    }

    public Long organizacionId() {
        Long id = id();
        return id == null ? null : usuarioRepository.findById(id).map(Usuario::getOrganizacionId).orElse(null);
    }

    /** Administrador de la plataforma: puede operar sobre todas las organizaciones. */
    public boolean esPlataforma() { return authz.esPlataforma(); }
}
