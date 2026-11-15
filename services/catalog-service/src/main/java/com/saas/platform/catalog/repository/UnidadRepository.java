package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.Unidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface UnidadRepository extends JpaRepository<Unidad, Long> {
    Optional<Unidad> findByCodigoUnico(String codigoUnico);
    Optional<Unidad> findByImei1OrImei2(String imei1, String imei2);
    List<Unidad> findByProductoId(Long productoId);
    List<Unidad> findByProductoIdAndEstado(Long productoId, Unidad.EstadoUnidad estado);
    List<Unidad> findByEstado(Unidad.EstadoUnidad estado);
    boolean existsByCodigoUnico(String codigoUnico);
    /** Unidades con un código de barras dentro de la organización (varias si es el EAN del modelo). */
    List<Unidad> findByCodigoBarrasAndProducto_OrganizacionIdOrderByIdAsc(String codigoBarras, Long organizacionId);
}
