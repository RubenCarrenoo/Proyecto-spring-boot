package com.coworkhub.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Servicio adicional a agregar a una reserva")
public record BookingServiceRequest(
        @Schema(example = "1") @NotNull(message = "es obligatorio") Long servicioId,
        @Schema(example = "2") @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0")
        @Max(value = 100, message = "no puede superar 100") Integer cantidad) {
}
