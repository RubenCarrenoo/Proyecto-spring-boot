package com.coworkhub.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Servicio adicional incluido en una reserva")
public record BookingItemResponse(
        Long servicioId,
        String nombre,
        Integer cantidad,
        @Schema(description = "Precio unitario histórico aplicado") BigDecimal precioUnitarioAplicado,
        BigDecimal subtotal) {
}
