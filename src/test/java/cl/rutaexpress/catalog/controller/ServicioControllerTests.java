package cl.rutaexpress.catalog.controller;

import cl.rutaexpress.catalog.dto.ServicioRequest;
import cl.rutaexpress.catalog.dto.ServicioResponse;
import cl.rutaexpress.catalog.exception.ResourceNotFoundException;
import cl.rutaexpress.catalog.security.SecurityConfig;
import cl.rutaexpress.catalog.service.ServicioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ServicioController.class, properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.example.test",
        "spring.security.oauth2.resourceserver.jwt.audiences=catalog-test"
})
@Import(SecurityConfig.class)
class ServicioControllerTests {

    private static final String URL = "/api/catalog/services";
    private static final String VALID_REQUEST = """
            {
              "nombre": "Entrega Express",
              "descripcion": "Entrega durante el mismo dia",
              "tarifa": 7990,
              "capacidadDisponible": 20,
              "activo": true
            }
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ServicioService service;

    @MockitoBean
    private JwtDecoder decoder;

    private final ServicioResponse response = new ServicioResponse(
            1L, "Entrega Express", "Entrega durante el mismo dia", new BigDecimal("7990"), 20, true);

    @Test
    void adminPuedeCrearConLocationYRecurso() throws Exception {
        tokenConRoles("ADMIN");
        when(service.crear(any())).thenReturn(response);

        mvc.perform(post(URL).header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + URL + "/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Entrega Express"))
                .andExpect(jsonPath("$.tarifa").value(7990))
                .andExpect(jsonPath("$.capacidadDisponible").value(20))
                .andExpect(jsonPath("$.activo").value(true));

        verify(service).crear(new ServicioRequest("Entrega Express", "Entrega durante el mismo dia",
                new BigDecimal("7990"), 20, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "DISPATCHER", "CLIENT"})
    void rolesDelProyectoPuedenConsultarYListar(String rol) throws Exception {
        tokenConRoles(rol);
        when(service.obtenerPorId(1L)).thenReturn(response);
        when(service.listar()).thenReturn(List.of(response));

        mvc.perform(get(URL + "/1").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void adminPuedeActualizar() throws Exception {
        tokenConRoles("ADMIN");
        ServicioResponse actualizado = new ServicioResponse(1L, "Normal", null, BigDecimal.ZERO, 0, false);
        when(service.actualizar(eq(1L), any())).thenReturn(actualizado);

        mvc.perform(put(URL + "/1").header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Normal","tarifa":0,"capacidadDisponible":0,"activo":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Normal"))
                .andExpect(jsonPath("$.tarifa").value(0))
                .andExpect(jsonPath("$.capacidadDisponible").value(0))
                .andExpect(jsonPath("$.activo").value(false));

        verify(service).actualizar(1L, new ServicioRequest("Normal", null, BigDecimal.ZERO, 0, false));
    }

    @Test
    void consultarInexistenteDevuelve404() throws Exception {
        tokenConRoles("CLIENT");
        when(service.obtenerPorId(99L)).thenThrow(new ResourceNotFoundException("No existe el servicio con id 99"));

        mvc.perform(get(URL + "/99").header("Authorization", "Bearer test-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No existe el servicio con id 99"));
    }

    @Test
    void actualizarInexistenteDevuelve404() throws Exception {
        tokenConRoles("ADMIN");
        when(service.actualizar(eq(99L), any())).thenThrow(new ResourceNotFoundException("No existe el servicio con id 99"));

        mvc.perform(put(URL + "/99").header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"nombre\":\" \",\"tarifa\":-1,\"capacidadDisponible\":-1,\"activo\":true}",
            "{\"nombre\":\"Express\",\"tarifa\":null,\"capacidadDisponible\":null,\"activo\":true}",
            "{\"nombre\":\"Express\",\"tarifa\":0,\"capacidadDisponible\":0,\"activo\":null}",
            "{\"nombre\":\"Express\",\"tarifa\":0.001,\"capacidadDisponible\":0,\"activo\":true}",
            "{\"nombre\":\"Express\",\"tarifa\":1000000000000,\"capacidadDisponible\":0,\"activo\":true}"
    })
    void requestInvalidoDevuelve400SinInvocarService(String body) throws Exception {
        tokenConRoles("ADMIN");

        mvc.perform(post(URL).header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores").isMap());
        mvc.perform(put(URL + "/1").header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void erroresDeValidacionIdentificanLosCampos() throws Exception {
        tokenConRoles("ADMIN");

        mvc.perform(post(URL).header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.tarifa").exists())
                .andExpect(jsonPath("$.errores.capacidadDisponible").exists())
                .andExpect(jsonPath("$.errores.activo").exists());
        verifyNoInteractions(service);
    }

    @Test
    void longitudesExcesivasDevuelven400() throws Exception {
        tokenConRoles("ADMIN");
        String body = VALID_REQUEST.replace("Entrega Express", "x".repeat(151))
                .replace("Entrega durante el mismo dia", "x".repeat(1001));

        mvc.perform(post(URL).header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.descripcion").exists());
        verifyNoInteractions(service);
    }

    @Test
    void jsonMalformadoEIdNoNumericoDevuelven400() throws Exception {
        tokenConRoles("ADMIN");

        mvc.perform(post(URL).header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(URL + "/abc").header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void sinTokenDevuelve401EnTodosLosEndpoints() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(get(URL + "/1")).andExpect(status().isUnauthorized());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());
        mvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void tokenRechazadoPorDecoderDevuelve401() throws Exception {
        when(decoder.decode("test-token")).thenThrow(new BadJwtException("Token invalido"));

        mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DISPATCHER", "CLIENT", "AUDITOR", "admin"})
    void rolesSinPermisoDeEscrituraReciben403(String rol) throws Exception {
        tokenConRoles(rol);

        mvc.perform(post(URL).header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isForbidden());
        mvc.perform(put(URL + "/1").header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void scopeSinRolesNoConcedeLecturaNiEscritura() throws Exception {
        tokenConRoles();

        mvc.perform(get(URL).header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
        mvc.perform(post(URL).header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void multiplesRolesConAdminPermitenEscritura() throws Exception {
        tokenConRoles("DISPATCHER", "ADMIN");
        when(service.crear(any())).thenReturn(response);

        mvc.perform(post(URL).header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated());
    }

    @Test
    void noSeExponeEliminacionNiRutasAdicionales() throws Exception {
        tokenConRoles("ADMIN");

        mvc.perform(delete(URL + "/1").header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/otra-ruta").header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    private void tokenConRoles(String... roles) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "RS256").subject("test-user").claim("scp", "OT.Create");
        if (roles.length > 0) {
            builder.claim("roles", List.of(roles));
        }
        when(decoder.decode("test-token")).thenReturn(builder.build());
    }
}
