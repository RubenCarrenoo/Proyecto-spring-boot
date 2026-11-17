package com.coworkhub.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales de inicio de sesión")
public record LoginRequest(
        @Schema(example = "admin") @NotBlank(message = "es obligatorio") String usuario,
        @Schema(example = "Admin123*") @NotBlank(message = "es obligatorio") String password) {
}
