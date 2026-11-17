package com.coworkhub.presentation;

import com.coworkhub.business.LocationService;
import com.coworkhub.dto.catalog.LocationRequest;
import com.coworkhub.dto.catalog.LocationResponse;
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
@RequestMapping("/api/sedes")
@Tag(name = "2. Sedes", description = "Catálogo de sedes. Lectura: autenticados. Escritura: ADMIN")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar sedes")
    @ApiResponse(responseCode = "200", description = "Listado de sedes")
    public List<LocationResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una sede")
    @ApiResponse(responseCode = "200", description = "Sede encontrada")
    public LocationResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crear sede (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Sede creada")
    public ResponseEntity<LocationResponse> create(@Valid @RequestBody LocationRequest request) {
        LocationResponse created = service.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar sede (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Sede actualizada")
    public LocationResponse update(@PathVariable Long id, @Valid @RequestBody LocationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar sede (ADMIN)", description = "No se permite si la sede tiene salas (RN-12).")
    @ApiResponse(responseCode = "204", description = "Sede eliminada")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
