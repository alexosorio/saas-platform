package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface CiudadRepository extends JpaRepository<Ciudad, Long> {
    List<Ciudad> findAllByOrderByNombreAsc();

    /** Búsqueda bajo demanda (autocomplete) por nombre. */
    @Query("SELECT c FROM Ciudad c WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY c.nombre")
    List<Ciudad> buscar(@Param("q") String q, Pageable pageable);
}
