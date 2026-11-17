package com.coworkhub.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Servicio adicional")
public record AdditionalServiceResponse(Long id, String nombre, BigDecimal precioUnitario) {
}
