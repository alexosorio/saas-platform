package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findByInventarioIdOrderByCreatedAtDesc(Long inventarioId);
    List<MovimientoInventario> findByOrdenIdOrderByCreatedAtDesc(Long ordenId);
}
