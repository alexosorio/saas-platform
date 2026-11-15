package com.saas.platform.subscription.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Mapeo de SOLO LECTURA de gets_shared.usuarios.
 * El catalog-service NO gestiona usuarios (eso es del user-service);
 * solo lee organizacion_id para resolver el tenant del usuario autenticado.
 */
@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    private Long id;

    @Column(name = "organizacion_id")
    private Long organizacionId;

    @Column(name = "email")
    private String email;
}
