package com.coworkhub.security;

import com.coworkhub.exception.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Responde 403 cuando el usuario está autenticado pero su rol no permite la operación. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ErrorResponseWriter.write(response, HttpServletResponse.SC_FORBIDDEN,
                "No tiene permisos para realizar esta operación", ErrorCodes.RN_13);
    }
}
