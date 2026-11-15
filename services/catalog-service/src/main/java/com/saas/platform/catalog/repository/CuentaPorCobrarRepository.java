package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.CuentaPorCobrar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
public interface CuentaPorCobrarRepository extends JpaRepository<CuentaPorCobrar, Long>, JpaSpecificationExecutor<CuentaPorCobrar> {
    List<CuentaPorCobrar> findByOrganizacionIdOrderByCreatedAtDesc(Long organizacionId);
    List<CuentaPorCobrar> findByOrganizacionIdAndEstadoOrderByCreatedAtDesc(Long organizacionId, String estado);
    Optional<CuentaPorCobrar> findByOrdenId(Long ordenId);
    List<CuentaPorCobrar> findByOrganizacionIdAndSaldoGreaterThan(Long organizacionId, java.math.BigDecimal saldo);
}
