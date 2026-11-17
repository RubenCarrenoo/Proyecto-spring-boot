package com.coworkhub.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Plan de membresía")
public record MembershipPlanResponse(
        Long id,
        String nombre,
        Integer horasIncluidasMes,
        BigDecimal descuentoExcedente,
        Integer maxReservasActivas) {
}
