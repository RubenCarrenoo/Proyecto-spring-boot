package com.coworkhub.security;

import com.coworkhub.config.AppProperties;
import com.coworkhub.config.BeansConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Emite y valida tokens JWT firmados con HS256. Duración: 60 minutos.
 * <p>
 * Usa el reloj UTC ({@code @Qualifier("utcClock")}) porque iat/exp son instantes UTC.
 */
@Service
public class JwtService {

    public static final String CLAIM_ROLE = "rol";
    public static final String CLAIM_MEMBER_ID = "miembroId";

    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final SecretKey key;
    private final Duration expiration;
    private final String issuer;
    private final Clock clock;

    public JwtService(AppProperties properties, @Qualifier(BeansConfig.UTC_CLOCK) Clock clock) {
        this.key = Keys.hmacShaKeyFor(properties.secretBytes());
        this.expiration = Duration.ofMinutes(properties.getJwtExpirationMinutes());
        this.issuer = properties.getJwtIssuer();
        this.clock = clock;
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = clock.instant();
        var builder = Jwts.builder()
                .subject(user.getUsername())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .claim(CLAIM_ROLE, user.getRole().name());
        if (user.getMemberId() != null) {
            builder.claim(CLAIM_MEMBER_ID, user.getMemberId());
        }
        return builder.signWith(key).compact();
    }

    /**
     * Valida firma, emisor y expiración. Lanza {@link JwtException} si el token es inválido,
     * fue alterado o venció.
     */
    public Claims parse(String token) {
        requireCanonicalFormat(token);
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Rechaza tokens cuyo texto fue alterado aunque la firma decodificada siga siendo la misma.
     * El decodificador Base64URL de JJWT es tolerante: si se agrega un carácter al final de la firma,
     * esos bits sobrantes se descartan y el token se aceptaría. Aquí se exige que cada segmento sea
     * Base64URL canónico (decodificar y volver a codificar debe producir exactamente el mismo texto).
     */
    private static void requireCanonicalFormat(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new MalformedJwtException("El JWT debe tener 3 segmentos");
        }
        for (String part : parts) {
            try {
                byte[] decoded = URL_DECODER.decode(part);
                if (part.isEmpty() || !URL_ENCODER.encodeToString(decoded).equals(part)) {
                    throw new MalformedJwtException("Segmento del JWT no canónico");
                }
            } catch (IllegalArgumentException e) {
                throw new MalformedJwtException("Segmento del JWT con Base64URL inválido", e);
            }
        }
    }

    public long getExpirationMinutes() {
        return expiration.toMinutes();
    }
}
