package com.saas.platform.subscription.repository;
import com.saas.platform.subscription.entity.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {
    List<Suscripcion> findByOrganizacionIdOrderByCreatedAtDesc(Long organizacionId);
    Optional<Suscripcion> findFirstByOrganizacionIdAndEstadoInOrderByCreatedAtDesc(
        Long organizacionId, List<Suscripcion.Estado> estados);
    Optional<Suscripcion> findFirstByOrganizacionIdOrderByCreatedAtDesc(Long organizacionId);
}
