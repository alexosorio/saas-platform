package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.CuentaPorPagar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface CuentaPorPagarRepository extends JpaRepository<CuentaPorPagar, Long>, JpaSpecificationExecutor<CuentaPorPagar> {
    List<CuentaPorPagar> findByOrganizacionIdOrderByCreatedAtDesc(Long organizacionId);
    List<CuentaPorPagar> findByOrganizacionIdAndEstadoOrderByCreatedAtDesc(Long organizacionId, String estado);
    List<CuentaPorPagar> findByProveedorId(Long proveedorId);
    java.util.Optional<CuentaPorPagar> findFirstByUnidadId(Long unidadId);
    List<CuentaPorPagar> findByOrganizacionIdAndSaldoGreaterThan(Long organizacionId, java.math.BigDecimal saldo);

    /** Saldo pendiente agrupado por proveedor: [nombre, saldo, cantidad]. */
    @Query("SELECT p.nombre, SUM(c.saldo), COUNT(c) FROM CuentaPorPagar c LEFT JOIN c.proveedor p "
         + "WHERE c.organizacionId = :orgId AND c.saldo > 0 GROUP BY p.nombre ORDER BY SUM(c.saldo) DESC")
    List<Object[]> saldoPorProveedor(@Param("orgId") Long orgId);
}
