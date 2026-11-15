package com.saas.platform.subscription.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs del módulo de administración de suscripciones (plataforma). */
public class AdminDTOs {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SuscripcionAdmin {
        private Long id;
        private Long organizacionId;
        private String organizacionNombre;
        private String organizacionEmail;
        private String planNombre;
        private String periodo;
        private String estado;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private Integer diasParaVencer;      // negativo = vencida hace N días
        private Long pagosPendientes;        // pagos esperando validación
        private LocalDateTime createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PagoAdmin {
        private Long id;
        private Long suscripcionId;
        private String organizacionNombre;
        private String planNombre;
        private BigDecimal monto;
        private String moneda;
        private String estado;
        private String metodo;
        private String banco;
        private String referencia;
        private String ultimosDigitos;
        private String comprobanteUrl;
        private LocalDate fechaTransaccion;
        private String motivoRechazo;
        private LocalDateTime fechaValidacion;
        private LocalDateTime createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Facturacion {
        private BigDecimal totalFacturado;
        private long pagosAprobados;
        private BigDecimal ticketPromedio;
        private long suscripcionesActivas;
        private long pendientesValidacion;
        private List<Fila> porMes;
        private List<Fila> porPlan;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Fila { private String etiqueta; private BigDecimal total; private long cantidad; }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Motivo { private String motivo; }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ResultadoVencimientos { private int suspendidas; private List<String> detalle; }
}
