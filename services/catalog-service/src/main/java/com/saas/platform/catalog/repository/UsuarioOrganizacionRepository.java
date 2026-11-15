package com.saas.platform.catalog.repository;

import com.saas.platform.catalog.entity.UsuarioOrganizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioOrganizacionRepository extends JpaRepository<UsuarioOrganizacion, Long> {
    List<UsuarioOrganizacion> findByUsuarioId(Long usuarioId);
    boolean existsByUsuarioIdAndOrganizacionId(Long usuarioId, Long organizacionId);
}
