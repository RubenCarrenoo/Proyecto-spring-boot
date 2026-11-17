package com.coworkhub.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token JWT emitido tras un inicio de sesión correcto")
public record LoginResponse(
        @Schema(description = "JWT a enviar en el encabezado Authorization: Bearer <token>") String token,
        @Schema(example = "Bearer") String tipo,
        @Schema(example = "60") long expiraEnMinutos,
        @Schema(example = "admin") String usuario,
        @Schema(example = "ADMIN") String rol,
        @Schema(description = "Id del miembro vinculado (solo cuentas MEMBER)", example = "1", nullable = true) Long miembroId) {
}
