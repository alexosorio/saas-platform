package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Mapeo de SOLO LECTURA de gets_shared.usuarios_organizaciones.
 * Relacion muchos-a-muchos usuario <-> organizacion. Permite validar a que
 * organizaciones pertenece un usuario (para usuarios multi-empresa).
 */
@Entity
@Table(name = "usuarios_organizaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioOrganizacion {

    @Id
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "organizacion_id", nullable = false)
    private Long organizacionId;

    @Column(name = "es_propietario")
    private Boolean esPropietario;
}
