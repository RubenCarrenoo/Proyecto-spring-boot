package com.coworkhub.presentation;

import com.coworkhub.business.AdditionalServiceCatalogService;
import com.coworkhub.dto.catalog.AdditionalServiceRequest;
import com.coworkhub.dto.catalog.AdditionalServiceResponse;
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
@RequestMapping("/api/servicios-adicionales")
@Tag(name = "5. Servicios adicionales", description = "Catálogo de servicios. Lectura: autenticados. Escritura: ADMIN")
public class AdditionalServiceController {

    private final AdditionalServiceCatalogService service;

    public AdditionalServiceController(AdditionalServiceCatalogService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar servicios adicionales")
    @ApiResponse(responseCode = "200", description = "Listado de servicios")
    public List<AdditionalServiceResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un servicio adicional")
    @ApiResponse(responseCode = "200", description = "Servicio encontrado")
    public AdditionalServiceResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Crear servicio adicional (ADMIN)", description = "El nombre es único (RN-11).")
    @ApiResponse(responseCode = "201", description = "Servicio creado")
    public ResponseEntity<AdditionalServiceResponse> create(@Valid @RequestBody AdditionalServiceRequest request) {
        AdditionalServiceResponse created = service.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri()).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar servicio adicional (ADMIN)",
            description = "El cambio de precio no afecta reservas existentes (precio histórico).")
    @ApiResponse(responseCode = "200", description = "Servicio actualizado")
    public AdditionalServiceResponse update(@PathVariable Long id, @Valid @RequestBody AdditionalServiceRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar servicio adicional (ADMIN)", description = "No se permite si está en reservas (RN-12).")
    @ApiResponse(responseCode = "204", description = "Servicio eliminado")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
