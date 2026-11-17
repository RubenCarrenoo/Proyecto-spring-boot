package com.coworkhub.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.ZoneOffset;

/**
 * Beans de infraestructura.
 * <p>
 * Se declaran DOS relojes:
 * <ul>
 *   <li>{@code businessClock} (marcado {@link Primary}): usa la zona horaria de las sedes
 *   (America/Bogota). Todas las reglas de negocio (anticipación, cancelaciones, horario de sede)
 *   lo reciben por defecto sin necesidad de calificador.</li>
 *   <li>{@code utcClock}: reloj UTC que se inyecta con {@link Qualifier} únicamente en
 *   {@code JwtService}, porque los claims {@code iat}/{@code exp} de un JWT son instantes UTC.</li>
 * </ul>
 * Inyectar {@link Clock} (en vez de llamar a {@code LocalDateTime.now()}) permite fijar la hora en
 * las pruebas unitarias.
 */
@Configuration
public class BeansConfig {

    public static final String UTC_CLOCK = "utcClock";

    @Bean
    @Primary
    public Clock businessClock(AppProperties properties) {
        return Clock.system(properties.zone());
    }

    @Bean(name = UTC_CLOCK)
    public Clock utcClock() {
        return Clock.system(ZoneOffset.UTC);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
