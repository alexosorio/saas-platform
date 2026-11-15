package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;
@Data
public class CreateUnidadRequest {
    @NotNull(message = "El producto es obligatorio")
    private Long productoId;
    private Long proveedorId;
    @NotBlank(message = "El codigo unico es obligatorio")
    @Size(max = 100) private String codigoUnico;
    @Size(max = 64) private String codigoBarras;   // el del empaque de la unidad (opcional)
    private String imei1;
    private String imei2;
    private String condicion;     // nuevo, usado
    private Integer nivelBateria;
    @NotNull(message = "El precio de compra es obligatorio")
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private String notas;
    private Map<String, String> caracteristicas;  // {codigoCaracteristica: valor}
    private Long bodegaId;
}
