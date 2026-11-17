package com.coworkhub.security;

import com.coworkhub.TestData;
import com.coworkhub.config.AppProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "Y293b3JraHViLXNlY3JldC1rZXktcGFyYS1qd3QtaHMyNTYtY2FtcHVzbGFuZHMtMjAyNg==";
    private static final Instant NOW = Instant.parse("2030-01-15T13:00:00Z");

    private JwtService service(Instant now) {
        AppProperties props = new AppProperties("America/Bogota", SECRET, 60, "coworkhub-api");
        props.validate();
        return new JwtService(props, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Genera y valida un token con rol y miembroId")
    void roundTrip() {
        String token = service(NOW).generateToken(TestData.memberUser(1L));
        var claims = service(NOW).parse(token);
        assertThat(claims.getSubject()).isEqualTo("member1");
        assertThat(claims.get(JwtService.CLAIM_ROLE, String.class)).isEqualTo("MEMBER");
        assertThat(claims.get(JwtService.CLAIM_MEMBER_ID, Long.class)).isEqualTo(1L);
    }

    @Test
    @DisplayName("El token expira a los 60 minutos")
    void expiresAfter60Minutes() {
        String token = service(NOW).generateToken(TestData.admin());
        service(NOW.plus(Duration.ofMinutes(59))).parse(token);
        assertThatThrownBy(() -> service(NOW.plus(Duration.ofMinutes(61))).parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Un token con un carácter extra al final se rechaza")
    void rejectsTrailingCharacter() {
        String token = service(NOW).generateToken(TestData.admin());
        for (String suffix : new String[]{"x", "A", "xx", "="}) {
            assertThatThrownBy(() -> service(NOW).parse(token + suffix))
                    .as("sufijo '%s'", suffix)
                    .isInstanceOf(JwtException.class);
        }
    }

    @Test
    @DisplayName("Un token con el payload modificado se rechaza")
    void rejectsModifiedPayload() {
        String token = service(NOW).generateToken(TestData.memberUser(1L));
        String[] parts = token.split("\\.");
        String forged = parts[0] + "." + parts[1].substring(0, parts[1].length() - 2) + "AA." + parts[2];
        assertThatThrownBy(() -> service(NOW).parse(forged)).isInstanceOf(JwtException.class);
    }
}
