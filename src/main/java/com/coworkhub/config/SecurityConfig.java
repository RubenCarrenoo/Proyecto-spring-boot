package com.coworkhub.config;

import com.coworkhub.security.JwtAuthenticationFilter;
import com.coworkhub.security.RestAccessDeniedHandler;
import com.coworkhub.security.RestAuthenticationEntryPoint;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad stateless con JWT.
 * <p>
 * Defensa en dos niveles:
 * <ol>
 *   <li>Reglas por URL y método HTTP en el {@link SecurityFilterChain} (este archivo).</li>
 *   <li>{@code @PreAuthorize} en los servicios ({@link EnableMethodSecurity}) y comprobación de
 *   propiedad de recursos en la capa de negocio.</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String RECEPTIONIST = "RECEPTIONIST";

    private static final String[] CATALOGS = {
            "/api/sedes/**", "/api/salas/**", "/api/equipamientos/**", "/api/servicios-adicionales/**", "/api/planes/**"
    };

    private static final String[] PUBLIC = {
            "/actuator/health", "/actuator/health/**",
            "/doc/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**",
            "/error"
    };

    /** Frontend (HTML, CSS y JS) servido desde src/main/resources/static. La seguridad real está en la API. */
    private static final String[] FRONTEND = {
            "/", "/index.html", "/app.html", "/favicon.ico", "/css/**", "/js/**", "/img/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Públicos
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(PUBLIC).permitAll()
                        .requestMatchers(HttpMethod.GET, FRONTEND).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Perfil: cualquier autenticado
                        .requestMatchers(HttpMethod.GET, "/auth/perfil").authenticated()
                        // Catálogos: lectura para autenticados, escritura solo ADMIN
                        .requestMatchers(HttpMethod.GET, CATALOGS).authenticated()
                        .requestMatchers(CATALOGS).hasRole(ADMIN)
                        // Miembros: listado solo personal; detalle y consumo -> propiedad en servicio
                        .requestMatchers(HttpMethod.GET, "/api/miembros").hasAnyRole(ADMIN, RECEPTIONIST)
                        .requestMatchers(HttpMethod.GET, "/api/miembros/*", "/api/miembros/*/consumo").authenticated()
                        .requestMatchers("/api/miembros", "/api/miembros/**").hasAnyRole(ADMIN, RECEPTIONIST)
                        // Disponibilidad
                        .requestMatchers(HttpMethod.GET, "/api/disponibilidad").authenticated()
                        // Reservas: confirmar y completar solo personal
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/confirmar", "/api/reservas/*/completar")
                        .hasAnyRole(ADMIN, RECEPTIONIST)
                        .requestMatchers("/api/reservas", "/api/reservas/**").authenticated()
                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * El filtro JWT es un @Component; sin esto Spring Boot también lo registraría como filtro de
     * servlet global y se ejecutaría fuera de la cadena de seguridad.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * CORS: solo hace falta si el frontend se abre desde otro origen (por ejemplo Live Server de VS Code
     * en el puerto 5500). Servido desde /static comparte origen con la API y no lo necesita.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5500,http://127.0.0.1:5500}") List<String> origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** AuthenticationManager usado por /auth/login: valida usuario y contraseña BCrypt. */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
