package cl.costostock.usuarios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Prueba de integración: levanta el microservicio completo con H2 en memoria. */
@SpringBootTest
@AutoConfigureMockMvc
class UsuariosApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void flujoCrudCompletoDeCliente() throws Exception {
        String body = """
                {"nombre":"Benjamín","email":"benja@mail.cl","password":"Clave123!"}
                """;
        String json = mvc.perform(post("/api/clientes/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("benja@mail.cl"))
                .andReturn().getResponse().getContentAsString();
        String id = json.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(post("/api/clientes/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/clientes/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"benja@mail.cl\",\"password\":\"Clave123!\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/clientes/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"benja@mail.cl\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(put("/api/clientes/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Benja Pérez\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Benja Pérez"));

        mvc.perform(get("/api/clientes/" + id)).andExpect(status().isOk());
        mvc.perform(delete("/api/clientes/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/clientes/" + id)).andExpect(status().isNotFound());
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
