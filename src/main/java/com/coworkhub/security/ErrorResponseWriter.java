package com.coworkhub.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Escribe la estructura uniforme de error ({"error","codigo","timestamp"}) desde los filtros de
 * seguridad, donde todavía no actúa @RestControllerAdvice.
 */
final class ErrorResponseWriter {

    private ErrorResponseWriter() {
    }

    static void write(HttpServletResponse response, int status, String error, String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String body = "{\"error\":\"" + escape(error) + "\",\"codigo\":\"" + escape(code)
                + "\",\"timestamp\":\"" + LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS) + "\"}";
        response.getWriter().write(body);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
