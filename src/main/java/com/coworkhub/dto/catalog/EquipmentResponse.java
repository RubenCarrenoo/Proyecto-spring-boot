package com.coworkhub.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Equipamiento")
public record EquipmentResponse(Long id, String nombre) {
}
