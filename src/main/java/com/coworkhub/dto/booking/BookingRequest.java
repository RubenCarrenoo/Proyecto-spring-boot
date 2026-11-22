package com.coworkhub.dto.booking;

import com.coworkhub.validation.TimeRange;
import com.coworkhub.validation.ValidTimeRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Solicitud de reserva")
@ValidTimeRange(message = "inicio debe ser anterior a fin")
public record BookingRequest(
        @Schema(example = "1") @NotNull(message = "es obligatorio") Long salaId,
        @Schema(description = "Obligatorio para ADMIN/RECEPTIONIST. Un MEMBER puede omitirlo (se usa el suyo).",
                example = "1", nullable = true) Long miembroId,
        @Schema(type = "string", example = "2026-12-01T09:00:00") @NotNull(message = "es obligatorio") LocalDateTime inicio,
        @Schema(type = "string", example = "2026-12-01T10:00:00") @NotNull(message = "es obligatorio") LocalDateTime fin,
        @Schema(example = "4") @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0") Integer asistentes,
        @Schema(description = "Servicios adicionales opcionales") List<@Valid BookingServiceRequest> servicios)
        implements TimeRange<LocalDateTime> {

    @Override
    public LocalDateTime rangeStart() {
        return inicio;
    }

    @Override
    public LocalDateTime rangeEnd() {
        return fin;
    }
}
