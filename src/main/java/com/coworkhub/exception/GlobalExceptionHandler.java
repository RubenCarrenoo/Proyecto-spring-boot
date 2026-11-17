package com.coworkhub.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Traduce todas las excepciones a la estructura uniforme {@link ErrorResponse}.
 * Ninguna situación de negocio termina en HTTP 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- 409: reglas de negocio ----------

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessRuleException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), ex.getCode());
    }

    /** Red de seguridad: si la BD rechaza un duplicado o una FK que no se detectó antes. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        String msg = ex.getMostSpecificCause().getMessage();
        boolean duplicate = msg != null && msg.toLowerCase().contains("duplicate");
        return duplicate
                ? build(HttpStatus.CONFLICT, "Ya existe un registro con esos datos únicos", ErrorCodes.RN_11)
                : build(HttpStatus.CONFLICT, "El registro está relacionado con otros datos", ErrorCodes.RN_12);
    }

    /** Si dos solicitudes compiten por la misma sala y una no obtiene el bloqueo a tiempo. */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleLock(PessimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT,
                "La sala está siendo reservada por otra solicitud en este momento, intente de nuevo", ErrorCodes.RN_01);
    }

    // ---------- 404 ----------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), ErrorCodes.NOT_FOUND);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", ErrorCodes.NOT_FOUND);
    }

    // ---------- 400: datos inválidos ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getAllErrors().stream()
                .map(err -> err instanceof FieldError fe ? fe.getField() + ": " + fe.getDefaultMessage() : err.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, msg, ErrorCodes.VALIDATION);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        String msg = ex.getAllErrors().stream()
                .map(err -> err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, msg, ErrorCodes.VALIDATION);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, msg, ErrorCodes.VALIDATION);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido o tiene un formato incorrecto",
                ErrorCodes.VALIDATION);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' tiene un valor inválido: " + ex.getValue(),
                ErrorCodes.VALIDATION);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getParameterName() + "' es obligatorio",
                ErrorCodes.VALIDATION);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), ErrorCodes.VALIDATION);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no soportado: " + ex.getMethod(),
                ErrorCodes.METHOD_NOT_ALLOWED);
    }

    // ---------- 401 / 403 lanzados dentro de controladores o servicios ----------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", ErrorCodes.UNAUTHORIZED);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(RuntimeException ex) {
        String msg = ex.getMessage() == null || ex.getMessage().equals("Access Denied")
                ? "No tiene permisos para realizar esta operación"
                : ex.getMessage();
        return build(HttpStatus.FORBIDDEN, msg, ErrorCodes.RN_13);
    }

    // ---------- 500: errores no previstos ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", ErrorCodes.INTERNAL);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String code) {
        return ResponseEntity.status(status).body(ErrorResponse.of(message, code));
    }
}
