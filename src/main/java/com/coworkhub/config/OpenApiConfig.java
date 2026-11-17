package com.coworkhub.config;

import com.coworkhub.exception.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuración de OpenAPI / Swagger UI.
 * <ul>
 *   <li>Esquema de seguridad "bearerAuth" (botón Authorize).</li>
 *   <li>Documenta en todas las operaciones las respuestas de error 400/401/403/404/409
 *   con la estructura {@link ErrorResponse}.</li>
 * </ul>
 * Swagger UI: http://localhost:8080/doc/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI coworkhubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CoWorkHub API")
                        .version("1.0.0")
                        .description("""
                                API REST para la gestión de una red de espacios de coworking: sedes, salas, \
                                equipamiento, planes, miembros, disponibilidad, reservas, servicios adicionales, \
                                costos, penalizaciones y consumo mensual.

                                **Autenticación:** haga `POST /auth/login`, copie el `token` y péguelo en el \
                                botón **Authorize**.

                                Usuarios de prueba: `admin / Admin123*`, `recepcion / Recep123*`, \
                                `ana / Ana123*`, `carlos / Carlos123*`.""")
                        .contact(new Contact().name("CoWorkHub - Proyecto final Spring Boot")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido en POST /auth/login (válido 60 minutos)")));
    }

    /** Agrega las respuestas de error estándar a cada operación documentada. */
    @Bean
    @SuppressWarnings("rawtypes")
    public OpenApiCustomizer errorResponsesCustomizer() {
        return openApi -> {
            Map<String, Schema> schemas = ModelConverters.getInstance().read(ErrorResponse.class);
            schemas.forEach(openApi.getComponents()::addSchemas);
            Schema<?> ref = new Schema<>().$ref("#/components/schemas/ErrorResponse");
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, op) -> {
                boolean isPublic = path.startsWith("/auth/login");
                addError(op, "400", "Datos inválidos", ref, "nombre: es obligatorio", "VALIDACION");
                addError(op, "401", "No autenticado: token ausente, inválido o expirado", ref,
                        isPublic ? "Credenciales inválidas" : "Se requiere autenticación (token JWT ausente)",
                        "NO_AUTENTICADO");
                if (!isPublic) {
                    addError(op, "403", "Acceso prohibido por rol o propiedad del recurso", ref,
                            "No tiene permisos para realizar esta operación", "RN-13");
                }
                if (path.contains("{")) {
                    addError(op, "404", "Recurso inexistente", ref, "Reserva con id 99 no existe", "NO_ENCONTRADO");
                }
                if (!"GET".equals(method.name()) || path.contains("disponibilidad")) {
                    addError(op, "409", "Conflicto o incumplimiento de una regla de negocio", ref,
                            "La sala ya tiene una reserva que se cruza con el horario solicitado", "RN-01");
                }
            }));
        };
    }

    private static void addError(Operation op, String code, String description, Schema<?> ref,
                                 String exampleMessage, String exampleCode) {
        ApiResponses responses = op.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            op.setResponses(responses);
        }
        if (responses.containsKey(code)) {
            return;
        }
        Example example = new Example().value(Map.of(
                "error", exampleMessage, "codigo", exampleCode, "timestamp", "2026-11-17T10:15:30"));
        responses.addApiResponse(code, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(ref).addExamples("ejemplo", example))));
    }
}
