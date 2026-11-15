package com.saas.platform.subscription.repository;
import com.saas.platform.subscription.entity.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {
    List<MetodoPago> findByOrganizacionIdAndActivoTrue(Long organizacionId);
    Optional<MetodoPago> findByOrganizacionIdAndPredeterminadoTrue(Long organizacionId);
}
