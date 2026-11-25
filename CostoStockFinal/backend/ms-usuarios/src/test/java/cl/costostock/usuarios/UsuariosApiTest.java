package cl.costostock.usuarios;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.costostock.usuarios.security.Rol;
import cl.costostock.usuarios.security.TokenService;
import cl.costostock.usuarios.service.NotificadorCorreo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Prueba de integración: levanta el microservicio completo con H2 en memoria. */
@SpringBootTest
@AutoConfigureMockMvc
class UsuariosApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    @MockitoBean
    private NotificadorCorreo notificador;

    private final ObjectMapper json = new ObjectMapper();

    private JsonNode body(String content) throws Exception {
        return json.readTree(content);
    }

    private String bearer(long id, Rol rol) {
        return "Bearer " + tokens.emitir(id, rol);
    }

    @Test
    void flujoCrudDeClienteConToken() throws Exception {
        String registro = """
                {"nombre":"Benjamín","email":"benja@mail.cl","password":"Clave123!"}
                """;
        JsonNode sesion = body(mvc.perform(post("/api/clientes/registro").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString());
        long id = sesion.get("id").asLong();
        String auth = "Bearer " + sesion.get("token").asText();

        mvc.perform(post("/api/clientes/registro").contentType(MediaType.APPLICATION_JSON).content(registro))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/clientes/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"benja@mail.cl\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());

        // Sin token: 401. Con token de otro cliente: 403. Con su token: OK.
        mvc.perform(get("/api/clientes/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/clientes/" + id).header(HttpHeaders.AUTHORIZATION, bearer(id + 100, Rol.CLIENTE)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/clientes/" + id).header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Benja Pérez\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Benja Pérez"));

        // Listar es solo para Soporte/Admin.
        mvc.perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.SOPORTE))).andExpect(status().isOk());

        // Soporte no puede eliminar; el dueño sí.
        mvc.perform(delete("/api/clientes/" + id).header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.SOPORTE)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/clientes/" + id).header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/clientes/" + id).header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void recuperarContrasenaConCodigo() throws Exception {
        mvc.perform(post("/api/clientes/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Pérez\",\"email\":\"ana@mail.cl\",\"password\":\"Clave123!\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/recuperacion/solicitar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@mail.cl\",\"tipo\":\"CLIENTE\"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(notificador).enviarCodigo(eq("ana@mail.cl"), codigo.capture(), anyInt());

        mvc.perform(post("/api/recuperacion/restablecer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@mail.cl\",\"tipo\":\"CLIENTE\",\"codigo\":\"000000\",\"nuevaPassword\":\"Nueva1234\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/recuperacion/restablecer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@mail.cl\",\"tipo\":\"CLIENTE\",\"codigo\":\"" + codigo.getValue()
                                + "\",\"nuevaPassword\":\"Nueva1234\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/clientes/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@mail.cl\",\"password\":\"Nueva1234\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void equipoInternoAdminYSoporte() throws Exception {
        JsonNode admin = body(mvc.perform(post("/api/equipo/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@costostock.cl\",\"password\":\"Admin1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andReturn().getResponse().getContentAsString());
        String authAdmin = "Bearer " + admin.get("token").asText();
        String nuevo = "{\"nombre\":\"Soporte Dos\",\"email\":\"soporte2@costostock.cl\",\"password\":\"Soporte1234\",\"rol\":\"SOPORTE\"}";

        mvc.perform(post("/api/equipo").header(HttpHeaders.AUTHORIZATION, bearer(2, Rol.SOPORTE))
                        .contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/equipo").header(HttpHeaders.AUTHORIZATION, authAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(nuevo))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/equipo").header(HttpHeaders.AUTHORIZATION, bearer(2, Rol.SOPORTE)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/equipo/" + admin.get("id").asLong()).header(HttpHeaders.AUTHORIZATION, authAdmin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroConDatosInvalidosDevuelve400ConCampos() throws Exception {
        mvc.perform(post("/api/proveedores/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"A\",\"rut\":\"123\",\"email\":\"no\",\"telefono\":\"1\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.rut").exists())
                .andExpect(jsonPath("$.campos.email").exists());
    }
}
