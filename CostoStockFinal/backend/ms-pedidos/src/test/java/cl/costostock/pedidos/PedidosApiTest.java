package cl.costostock.pedidos;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.costostock.pedidos.client.ProductosClient;
import cl.costostock.pedidos.client.ProductosClient.ProductoDto;
import cl.costostock.pedidos.security.Rol;
import cl.costostock.pedidos.security.TokenService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Prueba de integración de ms-pedidos; ms-productos se reemplaza por un mock. */
@SpringBootTest
@AutoConfigureMockMvc
class PedidosApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductosClient productosClient;

    @Autowired
    private TokenService tokens;

    private String bearer(long id, Rol rol) {
        return "Bearer " + tokens.emitir(id, rol);
    }

    @Test
    void crearListarCancelarYEliminarSegunRol() throws Exception {
        when(productosClient.descontarStock(anyList()))
                .thenReturn(List.of(new ProductoDto(1L, "Audífonos", "🎧", 19990, 10)));
        String cliente = bearer(5, Rol.CLIENTE);
        String pedido = "{\"clienteId\":5,\"items\":[{\"productoId\":1,\"cantidad\":2}]}";

        // Sin token, con otro cliente o con un proveedor: no se puede comprar.
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/pedidos").header(HttpHeaders.AUTHORIZATION, bearer(6, Rol.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/pedidos").header(HttpHeaders.AUTHORIZATION, bearer(5, Rol.PROVEEDOR))
                        .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isForbidden());

        String json = mvc.perform(post("/api/pedidos").header(HttpHeaders.AUTHORIZATION, cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(39980))
                .andReturn().getResponse().getContentAsString();
        String id = json.replaceAll("^\\{\"id\":(\\d+).*", "$1").trim();

        mvc.perform(get("/api/pedidos").param("clienteId", "5").header(HttpHeaders.AUTHORIZATION, cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items[0].nombreProducto").value("Audífonos"));
        mvc.perform(get("/api/pedidos").header(HttpHeaders.AUTHORIZATION, cliente)).andExpect(status().isForbidden());
        mvc.perform(get("/api/pedidos").header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.SOPORTE))).andExpect(status().isOk());

        mvc.perform(delete("/api/pedidos/" + id).header(HttpHeaders.AUTHORIZATION, cliente)).andExpect(status().isConflict());
        mvc.perform(patch("/api/pedidos/" + id + "/cancelar").header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.SOPORTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
        mvc.perform(delete("/api/pedidos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.SOPORTE)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/pedidos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(1, Rol.ADMIN)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/pedidos/" + id).header(HttpHeaders.AUTHORIZATION, cliente)).andExpect(status().isNotFound());
    }

    @Test
    void pedidoVacioDevuelve400() throws Exception {
        mvc.perform(post("/api/pedidos").header(HttpHeaders.AUTHORIZATION, bearer(5, Rol.CLIENTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":5,\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
