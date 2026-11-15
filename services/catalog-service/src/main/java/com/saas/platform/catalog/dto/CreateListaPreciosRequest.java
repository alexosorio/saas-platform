package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class CreateListaPreciosRequest {
    @NotBlank @Size(max = 200) private String nombre;
    @NotNull private String tipoCliente;     // mayorista | minorista
    private String descripcion;
    // Formula
    private String baseCalculo;              // precio_venta | precio_compra (default precio_venta)
    private String tipoAjuste;               // margen_porcentaje | valor_fijo (default margen_porcentaje)
    private BigDecimal valorAjuste;          // default 0
}
