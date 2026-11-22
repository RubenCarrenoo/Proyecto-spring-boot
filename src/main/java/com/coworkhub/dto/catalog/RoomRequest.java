package com.coworkhub.dto.catalog;

import com.coworkhub.domain.enums.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Datos para crear o actualizar una sala")
public record RoomRequest(
        @Schema(example = "Sala Andes") @NotBlank(message = "es obligatorio") @Size(max = 100) String nombre,
        @Schema(example = "MEETING_ROOM") @NotNull(message = "es obligatorio") RoomType tipo,
        @Schema(example = "8") @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0") Integer capacidad,
        @Schema(example = "50000") @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0")
        @Digits(integer = 10, fraction = 2) BigDecimal tarifaHora,
        @Schema(description = "Si se omite, la sala queda activa", example = "true") Boolean activa,
        @Schema(example = "1") @NotNull(message = "es obligatorio") Long sedeId,
        @Schema(description = "Ids de equipamiento de la sala", example = "[1, 2]") List<@NotNull Long> equipamientoIds) {
}
