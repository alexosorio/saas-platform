package com.saas.platform.catalog.repository;

import com.saas.platform.catalog.entity.EnlacePublicoLista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnlacePublicoListaRepository extends JpaRepository<EnlacePublicoLista, Long> {
    Optional<EnlacePublicoLista> findByToken(String token);
    List<EnlacePublicoLista> findByListaPreciosIdOrderByCreatedAtDesc(Long listaId);
}
