package com.saas.platform.user.dto;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AssignRolRequest {
    @NotNull(message = "El ID del rol es requerido")
    private Long rolId;
}
