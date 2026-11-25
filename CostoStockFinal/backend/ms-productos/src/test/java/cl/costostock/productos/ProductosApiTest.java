package cl.costostock.productos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.costostock.productos.security.Rol;
import cl.costostock.productos.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Prueba de integración del CRUD de productos y del descuento de stock con H2 en memoria. */
@SpringBootTest
@AutoConfigureMockMvc
class ProductosApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TokenService tokens;

    private String bearer(long id, Rol rol) {
        return "Bearer " + tokens.emitir(id, rol);
    }

    private static final String PRODUCTO = """
            {"proveedorId":1,"nombre":"Lámpara LED","categoria":"Hogar","descripcion":"3 niveles",
             "precio":12990,"stock":3,"comuna":"Maipú","condicion":"NEW","imagenes":[]}
            """;

    @Test
    void flujoCrudYStock() throws Exception {
        String json = mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.PROVEEDOR)).contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emoji").value("🏠"))
                .andReturn().getResponse().getContentAsString();
        String id = json.replaceAll("^\\{\"id\":(\\d+).*", "$1").trim();

        mvc.perform(get("/api/productos").param("proveedorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Lámpara LED"));

        mvc.perform(put("/api/productos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.PROVEEDOR)).contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO.replace("Lámpara LED", "Lámpara LED Pro")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Lámpara LED Pro"));

        mvc.perform(post("/api/productos/stock/descontar").header(HttpHeaders.AUTHORIZATION, bearer(0, Rol.SISTEMA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productoId\":" + id + ",\"cantidad\":5}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detalles[0]").value("Lámpara LED Pro"));

        mvc.perform(post("/api/productos/stock/descontar").header(HttpHeaders.AUTHORIZATION, bearer(0, Rol.SISTEMA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productoId\":" + id + ",\"cantidad\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stock").value(1));

        mvc.perform(patch("/api/productos/" + id + "/estado").header(HttpHeaders.AUTHORIZATION, bearer(9, Rol.SOPORTE)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"PAUSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAUSED"));

        mvc.perform(get("/api/productos").param("soloDisponibles", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Soporte no puede eliminar, otro proveedor tampoco; el admin sí.
        mvc.perform(delete("/api/productos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(9, Rol.SOPORTE)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/productos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(2, Rol.PROVEEDOR)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/productos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.ADMIN)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/productos/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void escribirSinTokenOConRolIncorrecto() throws Exception {
        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/productos/stock/descontar").header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"productoId\":1,\"cantidad\":1}]}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, "Bearer token-falso")
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearConDatosInvalidosDevuelve400() throws Exception {
        mvc.perform(post("/api/productos").header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.PROVEEDOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"precio\":0,\"stock\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.precio").exists());
    }
}
