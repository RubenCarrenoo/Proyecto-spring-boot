package com.coworkhub.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Estructura uniforme de todas las respuestas de error de la API. */
@Schema(description = "Respuesta de error uniforme")
public record ErrorResponse(
        @Schema(description = "Descripción del error", example = "La sala ya tiene una reserva que se cruza con el horario solicitado")
        String error,
        @Schema(description = "Regla de negocio violada o tipo de error", example = "RN-01")
        String codigo,
        @Schema(description = "Fecha y hora del error", example = "2026-11-17T10:15:30")
        LocalDateTime timestamp) {

    public static ErrorResponse of(String error, String codigo) {
        return new ErrorResponse(error, codigo, LocalDateTime.now());
    }
}
