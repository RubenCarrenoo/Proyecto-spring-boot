package com.coworkhub.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos de un equipamiento")
public record EquipmentRequest(
        @Schema(example = "Proyector") @NotBlank(message = "es obligatorio") @Size(max = 100) String nombre) {
}
