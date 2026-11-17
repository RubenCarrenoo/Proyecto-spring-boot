package com.coworkhub.presentation;

import com.coworkhub.business.RoomService;
import com.coworkhub.dto.catalog.RoomRequest;
import com.coworkhub.dto.catalog.RoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/salas")
@Tag(name = "3. Salas", description = "Catálogo de salas. Lectura: autenticados. Escritura: ADMIN")
public class RoomController {

    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar salas", description = "Filtro opcional por sede con ?sedeId=")
    @ApiResponse(responseCode = "200", description = "Listado de salas")
    public List<RoomResponse> findAll(@Parameter(description = "Id de la sede", example = "1")
                                      @RequestParam(required = false) Long sedeId) {
        return service.findAll(sedeId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una sala")
    @ApiResponse(responseCode = "200", description = "Sala encontrada")
    public RoomResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crear sala (ADMIN)", description = "El nombre es único dentro de la sede (RN-11).")
    @ApiResponse(responseCode = "201", description = "Sala creada")
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
        RoomResponse created = service.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar sala (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Sala actualizada")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar sala (ADMIN)", description = "No se permite si la sala tiene reservas (RN-12).")
    @ApiResponse(responseCode = "204", description = "Sala eliminada")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
