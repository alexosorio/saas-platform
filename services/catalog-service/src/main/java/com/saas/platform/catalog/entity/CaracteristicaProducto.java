package com.saas.platform.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "caracteristicas_productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaracteristicaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 50, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_dato", columnDefinition = "tipo_dato_caracteristica")
    @Builder.Default
    private TipoDato tipoDato = TipoDato.texto;

    @Column(name = "opciones_lista", columnDefinition = "text")
    private String opcionesLista;

    @Column
    @Builder.Default
    private Boolean obligatorio = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "aplicable_a", columnDefinition = "aplicable_caracteristica")
    @Builder.Default
    private AplicableA aplicableA = AplicableA.todos;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum TipoDato { texto, numero, fecha, booleano, lista }
    public enum AplicableA { moviles, accesorios, todos }
}
