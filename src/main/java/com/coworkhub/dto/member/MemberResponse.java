package com.coworkhub.dto.member;

import com.coworkhub.domain.enums.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Miembro")
public record MemberResponse(
        Long id,
        String documento,
        String nombreCompleto,
        String email,
        MemberStatus estado,
        Long planId,
        String planNombre,
        @Schema(description = "Usuario de la cuenta vinculada (si tiene)", nullable = true) String usuario) {
}
