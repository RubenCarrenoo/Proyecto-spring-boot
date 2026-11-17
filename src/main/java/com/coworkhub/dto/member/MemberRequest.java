package com.coworkhub.dto.member;

import com.coworkhub.validation.AccountData;
import com.coworkhub.validation.ValidAccountData;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registro / actualización de un miembro.
 * "usuario" y "password" son opcionales: si se envían (solo en el registro) se crea su cuenta MEMBER.
 */
@Schema(description = "Datos de un miembro (la cuenta de usuario es opcional y solo se crea en el registro)")
@ValidAccountData
public record MemberRequest(
        @Schema(example = "1098765432") @NotBlank(message = "es obligatorio") @Size(max = 30) String documento,
        @Schema(example = "Laura Pérez") @NotBlank(message = "es obligatorio") @Size(max = 150) String nombreCompleto,
        @Schema(example = "laura.perez@mail.com") @NotBlank(message = "es obligatorio")
        @Email(message = "no tiene un formato válido") @Size(max = 150) String email,
        @Schema(example = "1") @NotNull(message = "es obligatorio") Long planId,
        @Schema(example = "laura", nullable = true) @Size(min = 3, max = 50, message = "debe tener entre 3 y 50 caracteres") String usuario,
        @Schema(example = "Laura123*", nullable = true) @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password)
        implements AccountData {
}
