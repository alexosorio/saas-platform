package com.saas.platform.catalog.repository;
import com.saas.platform.catalog.entity.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Orden> {
    Optional<Orden> findByNumeroOrden(String numeroOrden);
    List<Orden> findByOrganizacionIdOrderByCreatedAtDesc(Long organizacionId);
    List<Orden> findByClienteIdOrderByCreatedAtDesc(Long clienteId);
    List<Orden> findByOrganizacionIdAndEstadoOrderByCreatedAtDesc(Long organizacionId, Orden.EstadoOrden estado);
    boolean existsByNumeroOrden(String numeroOrden);
}
