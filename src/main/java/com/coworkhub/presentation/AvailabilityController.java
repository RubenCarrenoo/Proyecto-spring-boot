package com.coworkhub.presentation;

import com.coworkhub.business.AvailabilityService;
import com.coworkhub.dto.catalog.RoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/disponibilidad")
@Tag(name = "8. Disponibilidad", description = "Consulta de salas libres (cualquier usuario autenticado)")
public class AvailabilityController {

    private final AvailabilityService service;

    public AvailabilityController(AvailabilityService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consultar salas disponibles",
            description = "Devuelve las salas activas de la sede sin reservas PENDING/CONFIRMED en el rango, con la "
                    + "capacidad mínima y TODO el equipamiento solicitado.")
    @ApiResponse(responseCode = "200", description = "Salas disponibles")
    public List<RoomResponse> findAvailable(
            @Parameter(description = "Id de la sede", example = "1", required = true) @RequestParam Long sedeId,
            @Parameter(description = "Fecha (yyyy-MM-dd)", example = "2026-12-01", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "Hora de inicio (HH:mm)", example = "09:00", required = true)
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaInicio,
            @Parameter(description = "Hora de fin (HH:mm)", example = "11:00", required = true)
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime horaFin,
            @Parameter(description = "Capacidad mínima", example = "4") @RequestParam(required = false) Integer capacidadMinima,
            @Parameter(description = "Ids de equipamiento requeridos (separados por coma)", example = "1,2")
            @RequestParam(required = false) List<Long> equipamientoIds) {
        return service.findAvailableRooms(sedeId, fecha, horaInicio, horaFin, capacidadMinima, equipamientoIds);
    }
}
