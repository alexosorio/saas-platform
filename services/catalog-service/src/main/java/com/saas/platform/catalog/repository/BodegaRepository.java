package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.Bodega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface BodegaRepository extends JpaRepository<Bodega, Long> {
    List<Bodega> findBySedeId(Long sedeId);
    List<Bodega> findBySedeIdAndActivoTrue(Long sedeId);
    List<Bodega> findByActivoTrue();
    Optional<Bodega> findByCodigoAndSedeId(String codigo, Long sedeId);
    List<Bodega> findBySedeIdInAndActivoTrue(List<Long> sedeIds);
}
