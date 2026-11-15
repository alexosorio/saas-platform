package com.saas.platform.subscription.repository;
import com.saas.platform.subscription.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByActivoTrueOrderByOrden();
}
