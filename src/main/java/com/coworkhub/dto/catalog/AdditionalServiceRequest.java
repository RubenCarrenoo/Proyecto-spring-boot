package com.coworkhub.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Datos de un servicio adicional")
public record AdditionalServiceRequest(
        @Schema(example = "Café ilimitado") @NotBlank(message = "es obligatorio") @Size(max = 100) String nombre,
        @Schema(example = "8000") @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0")
        @Digits(integer = 10, fraction = 2) BigDecimal precioUnitario) {
}
