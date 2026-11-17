package com.coworkhub.dto.member;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Consumo mensual de horas y cobros de un miembro")
public record ConsumptionResponse(
        Long miembroId,
        String nombreMiembro,
        @Schema(example = "2026-11") String mes,
        String plan,
        @Schema(description = "Horas incluidas en el plan por mes") Integer horasIncluidasPlan,
        @Schema(description = "Horas incluidas ya consumidas") BigDecimal horasIncluidasUsadas,
        @Schema(description = "Horas incluidas que quedan disponibles") BigDecimal horasIncluidasDisponibles,
        @Schema(description = "Horas reservadas (no canceladas)") BigDecimal horasReservadas,
        @Schema(description = "Horas cobradas como excedente") BigDecimal horasExcedentes,
        @Schema(description = "Reservas no canceladas del mes") long totalReservas,
        @Schema(description = "Reservas canceladas del mes") long reservasCanceladas,
        BigDecimal costoSalas,
        BigDecimal costoServicios,
        @Schema(description = "Suma de penalizaciones por cancelación") BigDecimal penalizaciones,
        @Schema(description = "costoSalas + costoServicios + penalizaciones") BigDecimal totalAPagar) {
}
