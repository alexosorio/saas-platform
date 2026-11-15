package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.PagoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PagoProveedorRepository extends JpaRepository<PagoProveedor, Long> {
    List<PagoProveedor> findByCuentaPorPagarIdOrderByCreatedAtDesc(Long cuentaPorPagarId);
}
