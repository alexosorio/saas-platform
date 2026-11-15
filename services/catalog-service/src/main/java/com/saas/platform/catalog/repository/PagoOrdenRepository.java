package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.PagoOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface PagoOrdenRepository extends JpaRepository<PagoOrden, Long> {
    List<PagoOrden> findByOrdenIdOrderByFechaPagoDesc(Long ordenId);
}
