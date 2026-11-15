package com.saas.platform.catalog.repository;

import com.saas.platform.catalog.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findByOrganizacionId(Long orgId);
    List<Proveedor> findByOrganizacionIdAndActivoTrue(Long orgId);
    Optional<Proveedor> findByNombreIgnoreCaseAndOrganizacionId(String nombre, Long orgId);

    /** Búsqueda bajo demanda (autocomplete) por nombre o email. */
    @Query("SELECT p FROM Proveedor p WHERE p.organizacionId = :orgId AND p.activo = true AND ("
         + "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(p.email,'')) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY p.nombre")
    List<Proveedor> buscar(@Param("orgId") Long orgId, @Param("q") String q, Pageable pageable);
}
