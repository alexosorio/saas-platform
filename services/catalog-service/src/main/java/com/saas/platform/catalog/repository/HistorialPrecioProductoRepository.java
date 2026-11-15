package com.saas.platform.catalog.repository;

import com.saas.platform.catalog.entity.HistorialPrecioProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistorialPrecioProductoRepository extends JpaRepository<HistorialPrecioProducto, Long> {
    List<HistorialPrecioProducto> findByProductoIdOrderByFechaCambioDesc(Long productoId);
}
