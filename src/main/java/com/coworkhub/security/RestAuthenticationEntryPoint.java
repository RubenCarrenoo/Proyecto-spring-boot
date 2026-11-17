package com.coworkhub.security;

import com.coworkhub.exception.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Responde 401 cuando no hay token, o el token es inválido o expiró. */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object detail = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        String message = detail != null ? detail.toString() : "Se requiere autenticación (token JWT ausente)";
        ErrorResponseWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED, message, ErrorCodes.UNAUTHORIZED);
    }
}
