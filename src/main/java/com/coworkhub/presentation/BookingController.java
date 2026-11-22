package com.coworkhub.presentation;

import com.coworkhub.business.BookingService;
import com.coworkhub.domain.enums.BookingStatus;
import com.coworkhub.dto.booking.BookingRequest;
import com.coworkhub.dto.booking.BookingResponse;
import com.coworkhub.dto.booking.BookingServiceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@Tag(name = "9. Reservas", description = "Reservas de salas, servicios adicionales y cambios de estado")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crear reserva",
            description = "ADMIN y RECEPTIONIST reservan para cualquier miembro (miembroId obligatorio). "
                    + "Un MEMBER solo a su nombre. Aplica RN-01 a RN-07.")
    @ApiResponse(responseCode = "201", description = "Reserva creada en estado PENDING con su costo calculado")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        BookingResponse created = service.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri()).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar reservas",
            description = "Filtros: ?miembroId=, ?salaId=&fecha=, ?estado=. Un MEMBER solo ve las suyas.")
    @ApiResponse(responseCode = "200", description = "Listado de reservas")
    public List<BookingResponse> findAll(
            @Parameter(example = "1") @RequestParam(required = false) Long miembroId,
            @Parameter(example = "1") @RequestParam(required = false) Long salaId,
            @Parameter(description = "Fecha (yyyy-MM-dd)", example = "2026-12-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(example = "PENDING") @RequestParam(required = false) BookingStatus estado) {
        return service.findAll(miembroId, salaId, fecha, estado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una reserva", description = "Un MEMBER solo puede consultar sus reservas.")
    @ApiResponse(responseCode = "200", description = "Reserva encontrada")
    public BookingResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/servicios")
    @Operation(summary = "Agregar servicio adicional", description = "Solo reservas PENDING (RN-10). Guarda el precio aplicado.")
    @ApiResponse(responseCode = "200", description = "Servicio agregado y costos recalculados")
    public BookingResponse addService(@PathVariable Long id, @Valid @RequestBody BookingServiceRequest request) {
        return service.addService(id, request);
    }

    @DeleteMapping("/{id}/servicios/{servicioId}")
    @Operation(summary = "Quitar servicio adicional", description = "Solo reservas PENDING (RN-10).")
    @ApiResponse(responseCode = "200", description = "Servicio eliminado y costos recalculados")
    public BookingResponse removeService(@PathVariable Long id, @PathVariable Long servicioId) {
        return service.removeService(id, servicioId);
    }

    @PostMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar reserva (ADMIN, RECEPTIONIST)", description = "PENDING → CONFIRMED (RN-09).")
    @ApiResponse(responseCode = "200", description = "Reserva confirmada")
    public BookingResponse confirm(@PathVariable Long id) {
        return service.confirm(id);
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar reserva",
            description = "≥24 h: gratis y devuelve horas; 24-2 h: 50 % del costo de sala; <2 h o iniciada: no permitido (RN-08).")
    @ApiResponse(responseCode = "200", description = "Reserva cancelada")
    public BookingResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @PostMapping("/{id}/completar")
    @Operation(summary = "Completar reserva (ADMIN, RECEPTIONIST)", description = "CONFIRMED → COMPLETED (RN-09).")
    @ApiResponse(responseCode = "200", description = "Reserva completada")
    public BookingResponse complete(@PathVariable Long id) {
        return service.complete(id);
    }
}
