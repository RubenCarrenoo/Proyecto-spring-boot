package com.coworkhub.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Datos de un plan de membresía")
public record MembershipPlanRequest(
        @Schema(example = "Profesional") @NotBlank(message = "es obligatorio") @Size(max = 80) String nombre,
        @Schema(description = "Horas de sala incluidas por mes", example = "20")
        @NotNull(message = "es obligatorio") @PositiveOrZero(message = "no puede ser negativo") Integer horasIncluidasMes,
        @Schema(description = "Descuento sobre horas excedentes, entre 0 y 1 (0.15 = 15 %)", example = "0.15")
        @NotNull(message = "es obligatorio") @DecimalMin(value = "0.0", message = "debe ser >= 0")
        @DecimalMax(value = "1.0", message = "debe ser <= 1") @Digits(integer = 1, fraction = 4) BigDecimal descuentoExcedente,
        @Schema(description = "Máximo de reservas activas futuras", example = "5")
        @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0") Integer maxReservasActivas) {
}
