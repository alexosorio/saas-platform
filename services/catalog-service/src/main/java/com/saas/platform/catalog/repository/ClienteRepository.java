package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Cliente> {
    List<Cliente> findByOrganizacionId(Long organizacionId);
    List<Cliente> findByOrganizacionIdAndActivoTrue(Long organizacionId);
    Optional<Cliente> findByDocumentoAndOrganizacionId(String documento, Long organizacionId);
    Optional<Cliente> findByEmailAndOrganizacionId(String email, Long organizacionId);
    List<Cliente> findByOrganizacionIdAndTipo(Long organizacionId, Cliente.TipoCliente tipo);
    List<Cliente> findByOrganizacionIdAndIdIn(Long organizacionId, java.util.List<Long> ids);

    /** Búsqueda bajo demanda (autocomplete) por nombre, documento o email. */
    @Query("SELECT c FROM Cliente c WHERE c.organizacionId = :orgId AND c.activo = true AND ("
         + "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(c.documento,'')) LIKE LOWER(CONCAT('%', :q, '%')) "
         + "OR LOWER(COALESCE(c.email,'')) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY c.nombre")
    List<Cliente> buscar(@Param("orgId") Long orgId, @Param("q") String q, Pageable pageable);
}
