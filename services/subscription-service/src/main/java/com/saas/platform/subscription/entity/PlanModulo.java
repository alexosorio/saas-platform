package com.saas.platform.subscription.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "planes_modulos", uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id","modulo_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanModulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modulo_id", nullable = false)
    private Modulo modulo;
}
