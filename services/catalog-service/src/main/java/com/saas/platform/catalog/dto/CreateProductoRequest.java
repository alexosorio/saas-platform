package com.saas.platform.catalog.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class CreateProductoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255) private String nombre;
    @NotNull(message = "La categoria es obligatoria")
    private Long categoriaId;
    private Long marcaId;
    @Size(max = 100) private String sku;
    private String descripcion;
    private String imagenUrl;
    private String tipoProducto;     // 'movil' | 'accesorio'
    private Boolean requiereImei;    // si null, se deriva de tipoProducto
}
