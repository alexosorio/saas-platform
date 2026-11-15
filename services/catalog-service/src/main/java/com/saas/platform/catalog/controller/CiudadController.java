package com.saas.platform.catalog.controller;
import com.saas.platform.catalog.dto.CiudadDTO;
import com.saas.platform.catalog.repository.CiudadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;
@RestController @RequestMapping("/api/cities") @RequiredArgsConstructor @CrossOrigin(originPatterns = "*")
public class CiudadController {
    private final CiudadRepository repo;
    @GetMapping
    public ResponseEntity<List<CiudadDTO>> getAll(@org.springframework.web.bind.annotation.RequestParam(required = false) String q) {
        List<com.saas.platform.catalog.entity.Ciudad> fuente = (q != null && !q.isBlank())
            ? repo.buscar(q.trim(), org.springframework.data.domain.PageRequest.of(0, 20))
            : repo.findAllByOrderByNombreAsc();
        List<CiudadDTO> ciudades = fuente.stream()
            .map(c -> CiudadDTO.builder().id(c.getId()).nombre(c.getNombre()).codigo(c.getCodigo()).pais(c.getPais()).build())
            .collect(Collectors.toList());
        return ResponseEntity.ok(ciudades);
    }
}
