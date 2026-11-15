package com.saas.platform.catalog.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
@Data
public class RegistrarPagoProveedorRequest {
    private BigDecimal monto;
    private String metodo;
    private String banco;
    private String comprobanteUrl;
    private String ultimosDigitos;
    private LocalDate fechaTransaccion;
    private String referencia;
    private String observaciones;
}
