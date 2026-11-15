package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    Optional<Categoria> findByCodigo(String codigo);
    List<Categoria> findByActivoTrue();
    boolean existsByCodigo(String codigo);
}
