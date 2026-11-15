package com.saas.platform.catalog.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "ciudades")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Ciudad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String nombre;
    @Column(length = 20) private String codigo;
    @Column(length = 100) private String pais;
}
