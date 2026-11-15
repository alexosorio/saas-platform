package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.AsientoContable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AsientoContableRepository extends JpaRepository<AsientoContable, Long> {
    Optional<AsientoContable> findFirstByTipoAndDocumentoId(String tipo, Long documentoId);
}
