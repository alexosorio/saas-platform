package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class UpdateProductoRequest {
    @Size(max = 255) private String nombre;
    private Long categoriaId;
    private Long marcaId;
    @Size(max = 100) private String sku;
    private String descripcion;
    private String imagenUrl;
    private String tipoProducto;
    private Boolean requiereImei;
}
