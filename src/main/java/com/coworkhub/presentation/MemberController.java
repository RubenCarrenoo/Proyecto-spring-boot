package com.coworkhub.presentation;

import com.coworkhub.business.MemberService;
import com.coworkhub.domain.enums.MemberStatus;
import com.coworkhub.dto.member.ConsumptionResponse;
import com.coworkhub.dto.member.MemberRequest;
import com.coworkhub.dto.member.MemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/miembros")
@Tag(name = "7. Miembros", description = "Gestión de miembros (ADMIN y RECEPTIONIST). Un MEMBER solo consulta su detalle y consumo")
public class MemberController {

    private final MemberService service;

    public MemberController(MemberService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar miembros (ADMIN, RECEPTIONIST)", description = "Filtros opcionales ?nombre= y ?estado=")
    @ApiResponse(responseCode = "200", description = "Listado de miembros")
    public List<MemberResponse> findAll(
            @Parameter(description = "Parte del nombre", example = "ana") @RequestParam(required = false) String nombre,
            @Parameter(description = "Estado del miembro", example = "SUSPENDED") @RequestParam(required = false) MemberStatus estado) {
        return service.findAll(nombre, estado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un miembro", description = "Un MEMBER solo puede consultar su propio registro.")
    @ApiResponse(responseCode = "200", description = "Miembro encontrado")
    public MemberResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Registrar miembro (ADMIN, RECEPTIONIST)",
            description = "Si se envían usuario y password se crea también su cuenta MEMBER. Documento, correo y usuario son únicos (RN-11).")
    @ApiResponse(responseCode = "201", description = "Miembro registrado")
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberRequest request) {
        MemberResponse created = service.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar miembro (ADMIN, RECEPTIONIST)")
    @ApiResponse(responseCode = "200", description = "Miembro actualizado")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/suspender")
    @Operation(summary = "Suspender miembro (ADMIN, RECEPTIONIST)", description = "Un miembro suspendido no puede reservar (RN-05).")
    @ApiResponse(responseCode = "200", description = "Miembro suspendido")
    public MemberResponse suspend(@PathVariable Long id) {
        return service.suspend(id);
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Activar miembro (ADMIN, RECEPTIONIST)")
    @ApiResponse(responseCode = "200", description = "Miembro activado")
    public MemberResponse activate(@PathVariable Long id) {
        return service.activate(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar miembro (ADMIN, RECEPTIONIST)", description = "No se permite si tiene reservas (RN-12).")
    @ApiResponse(responseCode = "204", description = "Miembro eliminado")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}/consumo")
    @Operation(summary = "Consumo mensual de horas", description = "Un MEMBER solo puede consultar su propio consumo.")
    @ApiResponse(responseCode = "200", description = "Consumo del mes")
    public ConsumptionResponse consumption(
            @PathVariable Long id,
            @Parameter(description = "Mes en formato yyyy-MM", example = "2026-11", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return service.getMonthlyConsumption(id, mes);
    }
}
