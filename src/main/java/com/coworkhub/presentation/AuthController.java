package com.coworkhub.presentation;

import com.coworkhub.business.AuthService;
import com.coworkhub.dto.auth.LoginRequest;
import com.coworkhub.dto.auth.LoginResponse;
import com.coworkhub.dto.auth.ProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "1. Autenticación", description = "Inicio de sesión con JWT y perfil del usuario")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión", description = "Valida usuario y contraseña y devuelve un JWT válido por 60 minutos.")
    @ApiResponse(responseCode = "200", description = "Autenticación correcta")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/perfil")
    @Operation(summary = "Perfil del usuario autenticado", description = "Devuelve usuario, rol y miembroId (si aplica).")
    @ApiResponse(responseCode = "200", description = "Perfil del usuario")
    public ProfileResponse profile() {
        return authService.profile();
    }
}
