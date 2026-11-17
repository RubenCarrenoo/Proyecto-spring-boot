package com.coworkhub.dto.catalog;

import com.coworkhub.domain.enums.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Sala")
public record RoomResponse(
        Long id,
        String nombre,
        RoomType tipo,
        Integer capacidad,
        BigDecimal tarifaHora,
        boolean activa,
        Long sedeId,
        String sedeNombre,
        List<EquipmentResponse> equipamientos) {
}
