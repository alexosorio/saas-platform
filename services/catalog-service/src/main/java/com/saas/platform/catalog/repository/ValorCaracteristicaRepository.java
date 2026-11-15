package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.ValorCaracteristica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ValorCaracteristicaRepository extends JpaRepository<ValorCaracteristica, Long> {
    List<ValorCaracteristica> findByUnidadId(Long unidadId);
}
