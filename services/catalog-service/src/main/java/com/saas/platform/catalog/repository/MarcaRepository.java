package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface MarcaRepository extends JpaRepository<Marca, Long> {
    Optional<Marca> findByCodigo(String codigo);
    Optional<Marca> findByNombre(String nombre);
    List<Marca> findByActivoTrue();
    boolean existsByCodigo(String codigo);
}
