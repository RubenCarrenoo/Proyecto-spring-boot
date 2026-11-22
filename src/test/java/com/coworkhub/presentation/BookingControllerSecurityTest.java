package com.coworkhub.presentation;

import com.coworkhub.TestData;
import com.coworkhub.business.BookingService;
import com.coworkhub.config.SecurityConfig;
import com.coworkhub.exception.GlobalExceptionHandler;
import com.coworkhub.security.JwtService;
import com.coworkhub.security.RestAccessDeniedHandler;
import com.coworkhub.security.RestAuthenticationEntryPoint;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de seguridad HTTP: 401 sin token / token alterado, 403 por rol, 400 por validación. */
@WebMvcTest(controllers = {BookingController.class, LocationController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class})
class BookingControllerSecurityTest {

    @Autowired MockMvc mvc;

    @MockitoBean BookingService bookingService;
    @MockitoBean com.coworkhub.business.LocationService locationService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Sin token -> 401 con estructura de error uniforme")
    void noTokenReturns401() throws Exception {
        mvc.perform(get("/api/reservas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Token alterado -> 401")
    void tamperedTokenReturns401() throws Exception {
        when(jwtService.parse(any())).thenThrow(new JwtException("firma inválida"));
        mvc.perform(get("/api/reservas").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("El token JWT es inválido"));
    }

    @Test
    @DisplayName("MEMBER no puede confirmar reservas -> 403")
    void memberCannotConfirm() throws Exception {
        mvc.perform(post("/api/reservas/1/confirmar").with(user(TestData.memberUser(1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("RN-13"));
        verify(bookingService, never()).confirm(any());
    }

    @Test
    @DisplayName("RECEPTIONIST sí puede confirmar reservas -> 200")
    void receptionistCanConfirm() throws Exception {
        mvc.perform(post("/api/reservas/1/confirmar").with(user(TestData.receptionist())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("MEMBER puede listar sus reservas -> 200")
    void memberCanList() throws Exception {
        when(bookingService.findAll(any(), any(), any(), any())).thenReturn(List.of());
        mvc.perform(get("/api/reservas").with(user(TestData.memberUser(1L))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RECEPTIONIST no puede crear catálogos (sedes) -> 403")
    void receptionistCannotCreateLocation() throws Exception {
        mvc.perform(post("/api/sedes").with(user(TestData.receptionist()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"ciudad\":\"Y\",\"direccion\":\"Z\",\"horaApertura\":\"08:00\",\"horaCierre\":\"18:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Crear sede sin nombre -> 400")
    void createLocationWithoutName() throws Exception {
        mvc.perform(post("/api/sedes").with(user(TestData.admin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ciudad\":\"Y\",\"direccion\":\"Z\",\"horaApertura\":\"08:00\",\"horaCierre\":\"18:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }
}
