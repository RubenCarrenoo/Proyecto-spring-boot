package com.coworkhub.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Base64;

/**
 * Propiedades propias de la aplicación leídas con {@link Value}.
 * <p>
 * Al iniciar, {@link #validate()} (anotado con {@link PostConstruct}) comprueba que la
 * configuración sea coherente. Si no lo es, la aplicación NO arranca: es preferible fallar
 * en el arranque que emitir tokens inseguros o calcular horarios con una zona inválida.
 */
@Component
public class AppProperties {

    private static final Logger log = LoggerFactory.getLogger(AppProperties.class);
    /** HS256 exige claves de al menos 256 bits. */
    private static final int MIN_SECRET_BYTES = 32;

    private final String zoneId;
    private final String jwtSecret;
    private final long jwtExpirationMinutes;
    private final String jwtIssuer;

    public AppProperties(@Value("${app.zone-id}") String zoneId,
                         @Value("${app.jwt.secret}") String jwtSecret,
                         @Value("${app.jwt.expiration-minutes:60}") long jwtExpirationMinutes,
                         @Value("${app.jwt.issuer:coworkhub-api}") String jwtIssuer) {
        this.zoneId = zoneId;
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMinutes = jwtExpirationMinutes;
        this.jwtIssuer = jwtIssuer;
    }

    @PostConstruct
    public void validate() {
        try {
            ZoneId.of(zoneId);
        } catch (DateTimeException e) {
            throw new IllegalStateException("app.zone-id no es una zona horaria válida: " + zoneId, e);
        }
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret es obligatorio");
        }
        if (secretBytes().length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret debe tener al menos " + MIN_SECRET_BYTES + " bytes");
        }
        if (jwtExpirationMinutes <= 0 || jwtExpirationMinutes > 60) {
            throw new IllegalStateException("app.jwt.expiration-minutes debe estar entre 1 y 60 (requisito: 60 min)");
        }
        log.info("Configuración validada: zona={}, expiración JWT={} min", zoneId, jwtExpirationMinutes);
    }

    /** Devuelve la clave JWT en bytes (acepta Base64 o texto plano). */
    public byte[] secretBytes() {
        try {
            return Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException notBase64) {
            return jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public ZoneId zone() {
        return ZoneId.of(zoneId);
    }

    public long getJwtExpirationMinutes() {
        return jwtExpirationMinutes;
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }
}
