package com.coworkhub.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

@Schema(description = "Sede")
public record LocationResponse(
        Long id,
        String nombre,
        String ciudad,
        String direccion,
        @Schema(type = "string", example = "07:00:00") LocalTime horaApertura,
        @Schema(type = "string", example = "20:00:00") LocalTime horaCierre) {
}
