package com.coworkhub.presentation;

import com.coworkhub.business.MembershipPlanService;
import com.coworkhub.dto.catalog.MembershipPlanRequest;
import com.coworkhub.dto.catalog.MembershipPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/planes")
@Tag(name = "6. Planes", description = "Planes de membresía. Lectura: autenticados. Escritura: ADMIN")
public class MembershipPlanController {

    private final MembershipPlanService service;

    public MembershipPlanController(MembershipPlanService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar planes")
    @ApiResponse(responseCode = "200", description = "Listado de planes")
    public List<MembershipPlanResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un plan")
    @ApiResponse(responseCode = "200", description = "Plan encontrado")
    public MembershipPlanResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crear plan (ADMIN)", description = "El nombre es único (RN-11).")
    @ApiResponse(responseCode = "201", description = "Plan creado")
    public ResponseEntity<MembershipPlanResponse> create(@Valid @RequestBody MembershipPlanRequest request) {
        MembershipPlanResponse created = service.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar plan (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Plan actualizado")
    public MembershipPlanResponse update(@PathVariable Long id, @Valid @RequestBody MembershipPlanRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar plan (ADMIN)", description = "No se permite si tiene miembros (RN-12).")
    @ApiResponse(responseCode = "204", description = "Plan eliminado")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
