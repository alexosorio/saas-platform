package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.DevolucionVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DevolucionVentaRepository extends JpaRepository<DevolucionVenta, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<DevolucionVenta> {
    List<DevolucionVenta> findByOrdenIdOrderByCreatedAtDesc(Long ordenId);
    long countByOrdenId(Long ordenId);
    List<DevolucionVenta> findTop300ByOrganizacionIdOrderByCreatedAtDesc(Long organizacionId);
}
