package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.CaracteristicaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface CaracteristicaProductoRepository extends JpaRepository<CaracteristicaProducto, Long> {
    Optional<CaracteristicaProducto> findByCodigo(String codigo);
    List<CaracteristicaProducto> findByAplicableA(CaracteristicaProducto.AplicableA aplicableA);
    boolean existsByCodigo(String codigo);
}
