package com.saas.platform.user.service;

import com.saas.platform.user.dto.ParametroDTO;
import com.saas.platform.user.entity.Parametro;
import com.saas.platform.user.repository.ParametroRepository;
import com.saas.platform.user.security.ContextoActor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParametroService {

    private final ParametroRepository repo;
    private final ContextoActor actor;
    private final AuditoriaService auditoria;

    @Transactional(readOnly = true)
    public List<ParametroDTO> listar() {
        Long org = actor.organizacionId();
        Map<String, Parametro> propios = org == null ? Map.of()
            : repo.findByOrganizacionId(org).stream().collect(Collectors.toMap(Parametro::getClave, p -> p, (a, b) -> a));
        return repo.findByOrganizacionIdIsNullOrderByCategoriaAscNombreAsc().stream()
            .filter(g -> actor.esPlataforma() || "organizacion".equals(g.getAlcance()))   // los de plataforma solo los ve la plataforma
            .map(g -> {
                Parametro p = propios.get(g.getClave());
                return ParametroDTO.builder()
                    .clave(g.getClave()).nombre(g.getNombre()).descripcion(g.getDescripcion())
                    .categoria(g.getCategoria()).tipo(g.getTipo()).alcance(g.getAlcance())
                    .valorGeneral(g.getValor()).valorOrganizacion(p != null ? p.getValor() : null)
                    .valorEfectivo(p != null ? p.getValor() : g.getValor())
                    .minimo(g.getMinimo()).maximo(g.getMaximo())
                    .editable(actor.esPlataforma() || "organizacion".equals(g.getAlcance()))
                    .build();
            }).collect(Collectors.toList());
    }

    /**
     * general = true  -> cambia el valor general (solo la plataforma).
     * general = false -> fija el valor propio de la organización (parámetros de alcance organización).
     */
    @Transactional
    public void actualizar(String clave, String valor, boolean general) {
        Parametro g = repo.findByClaveAndOrganizacionIdIsNull(clave)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parámetro no encontrado"));
        String v = validar(g, valor);
        boolean esPlataforma = "plataforma".equals(g.getAlcance());
        if (general || esPlataforma) {
            if (!actor.esPlataforma())
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el administrador de la plataforma puede cambiar este valor");
            g.setValor(v); g.setUpdatedAt(LocalDateTime.now()); g.setUpdatedBy(actor.id());
            repo.save(g);
            auditoria.registrar("PARAMETRO_GENERAL_ACTUALIZADO", "parametro", clave, java.util.Map.of("valor", v));
            return;
        }
        Long org = actor.organizacionId();
        if (org == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tu usuario no pertenece a una organización");
        Parametro p = repo.findByClaveAndOrganizacionId(clave, org).orElseGet(() -> Parametro.builder()
            .clave(clave).organizacionId(org).tipo(g.getTipo()).alcance(g.getAlcance())
            .categoria(g.getCategoria()).nombre(g.getNombre()).build());
        p.setValor(v); p.setUpdatedAt(LocalDateTime.now()); p.setUpdatedBy(actor.id());
        repo.save(p);
            auditoria.registrar("PARAMETRO_ACTUALIZADO", "parametro", clave, java.util.Map.of("valor", v));
    }

    /** Elimina el valor propio de la organización: vuelve a aplicar el general. */
    @Transactional
    public void restablecer(String clave) {
        Long org = actor.organizacionId();
        if (org != null) repo.findByClaveAndOrganizacionId(clave, org).ifPresent(repo::delete);
            auditoria.registrar("PARAMETRO_RESTABLECIDO", "parametro", clave, null);
    }

    /** Valor efectivo para una organización (propio si existe; si no, el general). */
    @Transactional(readOnly = true)
    public String valor(String clave, Long org, String porDefecto) {
        if (org != null) {
            Optional<Parametro> p = repo.findByClaveAndOrganizacionId(clave, org);
            if (p.isPresent()) return p.get().getValor();
        }
        return repo.findByClaveAndOrganizacionIdIsNull(clave).map(Parametro::getValor).orElse(porDefecto);
    }

    public int entero(String clave, Long org, int porDefecto) {
        try { return Integer.parseInt(valor(clave, org, String.valueOf(porDefecto))); }
        catch (NumberFormatException e) { return porDefecto; }
    }

    private String validar(Parametro def, String valor) {
        if (valor == null || valor.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El valor es obligatorio");
        String v = valor.trim();
        if ("entero".equals(def.getTipo()) || "decimal".equals(def.getTipo())) {
            BigDecimal n;
            try { n = new BigDecimal(v); }
            catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ser un número"); }
            if ("entero".equals(def.getTipo()) && n.stripTrailingZeros().scale() > 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ser un número entero");
            if (def.getMinimo() != null && n.compareTo(def.getMinimo()) < 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mínimo permitido es " + def.getMinimo().stripTrailingZeros().toPlainString());
            if (def.getMaximo() != null && n.compareTo(def.getMaximo()) > 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El máximo permitido es " + def.getMaximo().stripTrailingZeros().toPlainString());
        }
        return v;
    }
}
