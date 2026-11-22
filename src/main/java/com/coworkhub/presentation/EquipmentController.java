package com.coworkhub.presentation;

import com.coworkhub.business.EquipmentService;
import com.coworkhub.dto.catalog.EquipmentRequest;
import com.coworkhub.dto.catalog.EquipmentResponse;
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
@RequestMapping("/api/equipamientos")
@Tag(name = "4. Equipamientos", description = "Catálogo de equipamiento. Lectura: autenticados. Escritura: ADMIN")
public class EquipmentController {

    private final EquipmentService service;

    public EquipmentController(EquipmentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar equipamientos")
    @ApiResponse(responseCode = "200", description = "Listado de equipamientos")
    public List<EquipmentResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un equipamiento")
    @ApiResponse(responseCode = "200", description = "Equipamiento encontrado")
    public EquipmentResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crear equipamiento (ADMIN)", description = "El nombre es único (RN-11).")
    @ApiResponse(responseCode = "201", description = "Equipamiento creado")
    public ResponseEntity<EquipmentResponse> create(@Valid @RequestBody EquipmentRequest request) {
        EquipmentResponse created = service.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar equipamiento (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Equipamiento actualizado")
    public EquipmentResponse update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar equipamiento (ADMIN)", description = "No se permite si está asignado a salas (RN-12).")
    @ApiResponse(responseCode = "204", description = "Equipamiento eliminado")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
