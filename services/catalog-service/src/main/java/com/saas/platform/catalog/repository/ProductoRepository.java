package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Producto> {
    List<Producto> findByOrganizacionId(Long organizacionId);
    List<Producto> findByOrganizacionIdAndActivoTrue(Long organizacionId);
    Optional<Producto> findBySkuAndOrganizacionId(String sku, Long organizacionId);
    boolean existsBySkuAndOrganizacionId(String sku, Long organizacionId);
    List<Producto> findByCategoriaIdAndOrganizacionId(Long categoriaId, Long organizacionId);
    List<Producto> findByMarcaIdAndOrganizacionId(Long marcaId, Long organizacionId);
    @Query("SELECT p FROM Producto p WHERE p.organizacionId = :orgId AND " +
           "(LOWER(p.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "EXISTS (SELECT 1 FROM Unidad u WHERE u.producto = p AND LOWER(u.codigoBarras) LIKE LOWER(CONCAT('%', :search, '%'))))")
    List<Producto> searchByOrganizacion(@Param("orgId") Long orgId, @Param("search") String search);
}
