package com.coworkhub.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil del usuario autenticado")
public record ProfileResponse(
        @Schema(example = "ana") String usuario,
        @Schema(example = "MEMBER") String rol,
        @Schema(example = "1", nullable = true) Long miembroId,
        @Schema(example = "Ana María Gómez", nullable = true) String nombreMiembro) {
}
