package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {
    Optional<Inventario> findByBodegaIdAndUnidadId(Long bodegaId, Long unidadId);
    List<Inventario> findByBodegaId(Long bodegaId);
    List<Inventario> findByUnidadId(Long unidadId);
    @Query("SELECT i FROM Inventario i WHERE i.unidad.producto.id = :productoId AND i.cantidad > 0")
    List<Inventario> findByProductoIdConStock(@Param("productoId") Long productoId);
    @Query("SELECT i FROM Inventario i WHERE i.bodega.id = :bodegaId AND i.cantidad > 0")
    List<Inventario> findByBodegaIdConStock(@Param("bodegaId") Long bodegaId);
    /** Paginado; q filtra por producto, código único, IMEI o código de barras. */
    @Query(value = "SELECT i FROM Inventario i JOIN i.unidad u JOIN u.producto p WHERE i.bodega.id = :bodegaId AND i.cantidad > 0 "
         + "AND (:q = '' OR LOWER(p.nombre) LIKE :q OR LOWER(u.codigoUnico) LIKE :q OR u.imei1 LIKE :q OR LOWER(u.codigoBarras) LIKE :q)",
           countQuery = "SELECT COUNT(i) FROM Inventario i JOIN i.unidad u JOIN u.producto p WHERE i.bodega.id = :bodegaId AND i.cantidad > 0 "
         + "AND (:q = '' OR LOWER(p.nombre) LIKE :q OR LOWER(u.codigoUnico) LIKE :q OR u.imei1 LIKE :q OR LOWER(u.codigoBarras) LIKE :q)")
    org.springframework.data.domain.Page<Inventario> paginarPorBodega(@Param("bodegaId") Long bodegaId, @Param("q") String q,
                                                                     org.springframework.data.domain.Pageable pageable);
    @Query("SELECT COALESCE(SUM(i.cantidad),0) FROM Inventario i WHERE i.unidad.producto.id = :productoId")
    Integer getStockTotalProducto(@Param("productoId") Long productoId);
}
