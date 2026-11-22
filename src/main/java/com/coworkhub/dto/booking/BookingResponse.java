package com.coworkhub.dto.booking;

import com.coworkhub.domain.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Reserva")
public record BookingResponse(
        Long id,
        Long salaId,
        String salaNombre,
        Long sedeId,
        String sedeNombre,
        Long miembroId,
        String miembroNombre,
        @Schema(type = "string", example = "2026-12-01T09:00:00") LocalDateTime inicio,
        @Schema(type = "string", example = "2026-12-01T10:00:00") LocalDateTime fin,
        BigDecimal horas,
        Integer asistentes,
        BookingStatus estado,
        @Schema(description = "Horas cubiertas por el plan (sin costo)") BigDecimal horasIncluidasAplicadas,
        BigDecimal costoSala,
        BigDecimal costoServicios,
        BigDecimal costoTotal,
        BigDecimal cargoCancelacion,
        List<BookingItemResponse> servicios) {
}
