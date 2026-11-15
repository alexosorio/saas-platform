package com.saas.platform.subscription.repository;
import com.saas.platform.subscription.entity.PlanModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface PlanModuloRepository extends JpaRepository<PlanModulo, Long> {
    List<PlanModulo> findByPlanId(Long planId);
}
