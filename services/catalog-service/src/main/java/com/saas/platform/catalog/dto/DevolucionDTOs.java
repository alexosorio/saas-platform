package com.saas.platform.catalog.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class DevolucionDTOs {

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        private String tipoMotivo;          // garantia | cambio | error_venta | desistimiento | otro
        private String observacion;
        private String tratamiento;         // reembolso | saldo_a_favor (solo si hay excedente ya pagado)
        private String metodoReembolso;
        private String referenciaReembolso;
        private List<Item> items;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Item {
        private Long detalleId;             // línea de la orden
        private Integer cantidad;
        private String destino;             // disponible | revision | baja
        private Long bodegaId;              // a dónde reingresa (por defecto, la bodega de origen)
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Devolucion {
        private Long id;
        private String numero;
        private Long ordenId;
        private String ordenNumero;          // número de la factura (orden)
        private String clienteNombre;
        private String tipoMotivo;
        private String observacion;
        private BigDecimal montoTotal;
        private BigDecimal montoAplicadoSaldo;
        private BigDecimal montoNotaCredito;
        private String tratamiento;
        private String metodoReembolso;
        private String referenciaReembolso;
        private LocalDateTime createdAt;
        private List<Linea> lineas;
        private List<java.util.Map<String, Object>> asiento;   // movimientos contables (solo en el detalle)
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Linea {
        private Long detalleOrdenId;
        private String unidadCodigo;
        private String productoNombre;
        private Integer cantidad;
        private BigDecimal monto;
        private String destino;
    }
}
