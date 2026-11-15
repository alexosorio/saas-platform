package com.saas.platform.user.service;

import com.saas.platform.user.dto.CreateRolRequest;
import com.saas.platform.user.dto.RolResponse;
import com.saas.platform.user.dto.UpdateRolRequest;
import com.saas.platform.user.entity.Rol;
import com.saas.platform.user.entity.RolModulo;
import com.saas.platform.user.repository.RolModuloRepository;
import com.saas.platform.user.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepo;
    private final RolModuloRepository rolModuloRepo;
    private final com.saas.platform.user.repository.ModuloRepository moduloRepo;
    private final com.saas.platform.user.repository.UsuarioRepository usuarioRepo;
    private final com.saas.platform.user.security.AuthorizationHelper authz;
    private final com.saas.platform.user.security.ContextoActor actor;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<RolResponse> getAll() {
        Long org = actor.organizacionId();
        return rolRepo.findAll().stream()
            .filter(r -> esVisible(r, org))
            .sorted(java.util.Comparator.comparing((Rol r) -> r.getOrganizacionId() != null).thenComparing(Rol::getNombre))
            .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RolResponse getById(Long id) { return toResponse(findVisible(id)); }

    @Transactional
    public RolResponse create(CreateRolRequest req) {
        exigirPlataforma(null, req.getPermisos());
        Long org = actor.esPlataforma() ? null : actor.organizacionId();
        validarNombre(req.getNombre(), org, null);
        Rol rol = Rol.builder()
            .nombre(req.getNombre().trim()).descripcion(req.getDescripcion())
            .organizacionId(org).esSistema(false).activo(true).build();
        rol = rolRepo.save(rol);
        guardarPermisos(rol, req.getPermisos());
        auditoria.registrar("ROL_CREADO", "rol", rol.getId(), java.util.Map.of("nombre", rol.getNombre()));
        return toResponse(find(rol.getId()));
    }

    @Transactional
    public RolResponse update(Long id, UpdateRolRequest req) {
        exigirPlataforma(id, req.getPermisos());
        Rol rol = findEditable(id);
        if (req.getNombre() != null) { validarNombre(req.getNombre(), rol.getOrganizacionId(), id); rol.setNombre(req.getNombre().trim()); }
        if (req.getDescripcion() != null) rol.setDescripcion(req.getDescripcion());
        rol = rolRepo.save(rol);
        if (req.getPermisos() != null) {
            rolModuloRepo.deleteByRolId(id);
            guardarPermisos(rol, req.getPermisos());
        }
        return toResponse(find(id));
    }

    @Transactional
    public void delete(Long id) {
        exigirPlataforma(id, null);
        Rol rol = findEditable(id);
        if (Boolean.TRUE.equals(rol.getEsSistema()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede eliminar un rol del sistema");
        rolModuloRepo.deleteByRolId(id);
        rolRepo.delete(rol);
        auditoria.registrar("ROL_ELIMINADO", "rol", id, java.util.Map.of("nombre", rol.getNombre()));
    }

    private void guardarPermisos(Rol rol, List<CreateRolRequest.PermisoRequest> permisos) {
        if (permisos == null) return;
        java.util.Set<Long> vistos = new java.util.HashSet<>();
        for (CreateRolRequest.PermisoRequest p : permisos) {
            boolean alguno = Boolean.TRUE.equals(p.getCrear()) || Boolean.TRUE.equals(p.getLeer())
                          || Boolean.TRUE.equals(p.getActualizar()) || Boolean.TRUE.equals(p.getEliminar());
            if (p.getModuloId() == null || !alguno || !vistos.add(p.getModuloId())) continue;
            com.saas.platform.user.entity.Modulo m = moduloRepo.findById(p.getModuloId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Módulo no encontrado: " + p.getModuloId()));
            rolModuloRepo.save(RolModulo.builder()
                .rol(rol)
                .moduloId(m.getId()).moduloCodigo(m.getCodigo()).moduloNombre(m.getNombre())
                .crear(Boolean.TRUE.equals(p.getCrear())).leer(Boolean.TRUE.equals(p.getLeer()))
                .actualizar(Boolean.TRUE.equals(p.getActualizar())).eliminar(Boolean.TRUE.equals(p.getEliminar()))
                .build());
        }
    }



    @Transactional(readOnly = true)
    public List<com.saas.platform.user.dto.ModuloResponse> listarModulos() {
        return moduloRepo.findByActivoTrueOrderByOrdenAscNombreAsc().stream()
            .filter(m -> authz.esPlataforma() || !m.getCodigo().startsWith("plataforma_"))
            .map(m -> com.saas.platform.user.dto.ModuloResponse.builder()
                .id(m.getId()).nombre(m.getNombre()).codigo(m.getCodigo()).descripcion(m.getDescripcion()).build())
            .collect(Collectors.toList());
    }

    /** Reemplaza la matriz de permisos del rol. Impide que un usuario se quite a sí mismo la administración de roles. */
    @Transactional
    public RolResponse actualizarPermisos(Long rolId, List<CreateRolRequest.PermisoRequest> permisos, Long usuarioActualId) {
        exigirPlataforma(rolId, permisos);
        Rol rol = findEditable(rolId);
        Long rolPropio = usuarioRepo.findById(usuarioActualId).map(u -> u.getRol() != null ? u.getRol().getId() : null).orElse(null);
        if (rolId.equals(rolPropio) && !conservaAdminRoles(permisos)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No puedes quitarle a tu propio rol el permiso de administrar roles; perderías el acceso a esta pantalla.");
        }
        rolModuloRepo.deleteByRolId(rolId);
        guardarPermisos(rol, permisos);
        auditoria.registrar("ROL_PERMISOS_ACTUALIZADOS", "rol", rolId, java.util.Map.of("nombre", rol.getNombre(), "modulos", permisos == null ? 0 : permisos.size()));
        return toResponse(find(rolId));
    }

    private boolean conservaAdminRoles(List<CreateRolRequest.PermisoRequest> permisos) {
        if (permisos == null) return false;
        for (CreateRolRequest.PermisoRequest p : permisos) {
            if (p.getModuloId() == null || !Boolean.TRUE.equals(p.getActualizar())) continue;
            String codigo = moduloRepo.findById(p.getModuloId()).map(com.saas.platform.user.entity.Modulo::getCodigo).orElse("");
            if ("roles".equals(codigo) || "*".equals(codigo)) return true;
        }
        return false;
    }

    /** ¿El rol incluye algún módulo de plataforma? */
    public boolean esRolDePlataforma(Long rolId) {
        if (rolId == null) return false;
        return rolModuloRepo.findByRolId(rolId).stream()
            .map(RolModulo::codigoEfectivo).anyMatch(c -> c != null && c.startsWith("plataforma_"));
    }

    /**
     * Roles y módulos de PLATAFORMA solo los ve, edita o asigna quien ya tiene permiso de plataforma.
     * Evita que el administrador de una organización escale a administrar todas.
     */
    public void exigirPlataforma(Long rolId, List<CreateRolRequest.PermisoRequest> permisos) {
        if (authz.esPlataforma()) return;
        boolean tocaPlataforma = esRolDePlataforma(rolId) || (permisos != null && permisos.stream()
            .map(p -> p.getModuloId() == null ? null : moduloRepo.findById(p.getModuloId()).map(com.saas.platform.user.entity.Modulo::getCodigo).orElse(null))
            .anyMatch(c -> c != null && c.startsWith("plataforma_")));
        if (tocaPlataforma)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un administrador de la plataforma puede gestionar este rol o módulo");
    }


    // ── Aislamiento de roles entre organizaciones ─────────────────────────

    /** Rol del sistema (sin los de plataforma, salvo para la plataforma) o propio de la organización. */
    private boolean esVisible(Rol r, Long org) {
        if (actor.esPlataforma()) return true;
        if (r.getOrganizacionId() == null) return !esRolDePlataforma(r.getId());
        return r.getOrganizacionId().equals(org);
    }

    /** Los roles del sistema solo los modifica la plataforma; los propios, su organización. */
    private boolean esEditable(Rol r, Long org) {
        if (actor.esPlataforma()) return true;
        return r.getOrganizacionId() != null && r.getOrganizacionId().equals(org);
    }

    private Rol findVisible(Long id) {
        Rol r = find(id);
        if (!esVisible(r, actor.organizacionId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado");
        return r;
    }

    private Rol findEditable(Long id) {
        Rol r = findVisible(id);
        if (!esEditable(r, actor.organizacionId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Los roles del sistema no se pueden modificar. Usa \"Personalizar\" para crear una copia editable en tu organización.");
        return r;
    }

    /** Para asignar un rol a un usuario: debe ser visible para quien asigna. */
    public void exigirAsignable(Long rolId) { if (rolId != null) findVisible(rolId); }

    /** Nombre único dentro de la organización y distinto de los roles del sistema (evita ambigüedades). */
    private void validarNombre(String nombre, Long org, Long idActual) {
        if (nombre == null || nombre.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del rol es obligatorio");
        String n = nombre.trim();
        boolean choca = rolRepo.findAll().stream()
            .filter(r -> idActual == null || !r.getId().equals(idActual))
            .filter(r -> r.getNombre().equalsIgnoreCase(n))
            .anyMatch(r -> r.getOrganizacionId() == null || java.util.Objects.equals(r.getOrganizacionId(), org));
        if (choca) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un rol llamado \"" + n + "\"");
    }

    /** Crea en la organización una copia editable de un rol (típicamente uno del sistema). */
    @Transactional
    public RolResponse personalizar(Long id, String nombreNuevo) {
        Rol origen = findVisible(id);
        Long org = actor.esPlataforma() ? null : actor.organizacionId();
        if (org == null && !actor.esPlataforma())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tu usuario no pertenece a una organización");
        String base = (nombreNuevo != null && !nombreNuevo.isBlank()) ? nombreNuevo.trim() : origen.getNombre() + " (personalizado)";
        String nombre = base;
        for (int i = 2; ; i++) {
            try { validarNombre(nombre, org, null); break; }
            catch (ResponseStatusException e) {
                if (nombreNuevo != null && !nombreNuevo.isBlank()) throw e;   // el nombre pedido explícitamente choca
                nombre = base + " " + i;
            }
        }
        Rol copia = rolRepo.save(Rol.builder().nombre(nombre).descripcion(origen.getDescripcion())
            .organizacionId(org).esSistema(false).activo(true).build());
        for (RolModulo rm : rolModuloRepo.findByRolId(origen.getId())) {
            String codigo = rm.codigoEfectivo();
            if (codigo != null && codigo.startsWith("plataforma_") && !actor.esPlataforma()) continue;
            rolModuloRepo.save(RolModulo.builder().rol(copia)
                .moduloId(rm.getModuloId()).moduloCodigo(codigo).moduloNombre(rm.nombreEfectivo())
                .crear(rm.getCrear()).leer(rm.getLeer()).actualizar(rm.getActualizar()).eliminar(rm.getEliminar()).build());
        }
        auditoria.registrar("ROL_PERSONALIZADO", "rol", copia.getId(), java.util.Map.of("origen", origen.getNombre(), "nombre", copia.getNombre()));
        return toResponse(find(copia.getId()));
    }

    private Rol find(Long id) {
        return rolRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
    }

    private RolResponse toResponse(Rol r) {
        List<RolResponse.PermisoDTO> permisos = rolModuloRepo.findByRolId(r.getId()).stream()
            .map(m -> RolResponse.PermisoDTO.builder()
                .id(m.getId()).moduloId(m.getModuloId())
                .moduloCodigo(m.codigoEfectivo()).moduloNombre(m.nombreEfectivo())
                .crear(m.getCrear()).leer(m.getLeer()).actualizar(m.getActualizar()).eliminar(m.getEliminar())
                .build())
            .collect(Collectors.toList());
        return RolResponse.builder()
            .id(r.getId()).nombre(r.getNombre()).descripcion(r.getDescripcion())
            .esSistema(r.getEsSistema()).activo(r.getActivo()).permisos(permisos)
            .organizacionId(r.getOrganizacionId()).editable(esEditable(r, actor.organizacionId()))
            .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt()).build();
    }
}
