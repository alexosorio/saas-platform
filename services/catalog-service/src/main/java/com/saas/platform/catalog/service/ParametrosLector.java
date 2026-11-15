package com.saas.platform.catalog.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Lee parámetros configurables (tabla parametros): el de la organización si existe; si no, el general. */
@Component
public class ParametrosLector {
    @PersistenceContext private EntityManager em;

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public int entero(String clave, Long organizacionId, int porDefecto) {
        try {
            var q = em.createNativeQuery(organizacionId == null
                ? "SELECT valor FROM parametros WHERE clave = :c AND organizacion_id IS NULL"
                : "SELECT valor FROM parametros WHERE clave = :c AND (organizacion_id = :o OR organizacion_id IS NULL) " +
                  "ORDER BY organizacion_id NULLS LAST LIMIT 1").setParameter("c", clave);
            if (organizacionId != null) q.setParameter("o", organizacionId);
            List<Object> r = q.getResultList();
            return r.isEmpty() ? porDefecto : Integer.parseInt(String.valueOf(r.get(0)).trim());
        } catch (Exception e) {
            return porDefecto;   // tabla aún sin crear o valor inválido: se usa el valor por defecto
        }
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public String texto(String clave, Long organizacionId, String porDefecto) {
        try {
            var q = em.createNativeQuery(organizacionId == null
                ? "SELECT valor FROM parametros WHERE clave = :c AND organizacion_id IS NULL"
                : "SELECT valor FROM parametros WHERE clave = :c AND (organizacion_id = :o OR organizacion_id IS NULL) " +
                  "ORDER BY organizacion_id NULLS LAST LIMIT 1").setParameter("c", clave);
            if (organizacionId != null) q.setParameter("o", organizacionId);
            List<Object> r = q.getResultList();
            return r.isEmpty() || r.get(0) == null ? porDefecto : String.valueOf(r.get(0)).trim();
        } catch (Exception e) { return porDefecto; }
    }
}
