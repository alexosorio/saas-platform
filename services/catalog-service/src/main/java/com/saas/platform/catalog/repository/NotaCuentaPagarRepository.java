package com.saas.platform.catalog.repository;

import com.saas.platform.catalog.entity.NotaCuentaPagar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface NotaCuentaPagarRepository extends JpaRepository<NotaCuentaPagar, Long> {
    List<NotaCuentaPagar> findByCuentaPorPagarIdInOrderByCreatedAtAsc(Collection<Long> ids);
}
