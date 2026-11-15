package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.Precio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface PrecioRepository extends JpaRepository<Precio, Long> {
    List<Precio> findByProductoId(Long productoId);
    List<Precio> findByListaPreciosId(Long listaPreciosId);
    long countByListaPreciosId(Long listaPreciosId);
    long deleteByListaPreciosId(Long listaPreciosId);
    @Query("SELECT p FROM Precio p WHERE p.listaPrecios.id = :listaId AND p.producto.id = :productoId " +
           "AND (p.vigenteHasta IS NULL OR p.vigenteHasta > :ahora) ORDER BY p.vigenteDesde DESC")
    Optional<Precio> findPrecioVigente(@Param("listaId") Long listaId, @Param("productoId") Long productoId, @Param("ahora") LocalDateTime ahora);
}
