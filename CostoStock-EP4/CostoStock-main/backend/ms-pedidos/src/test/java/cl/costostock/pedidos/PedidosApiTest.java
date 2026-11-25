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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

    @Test
    void crearListarCancelarYEliminar() throws Exception {
        when(productosClient.descontarStock(anyList()))
                .thenReturn(List.of(new ProductoDto(1L, "Audífonos", "🎧", 19990, 10)));

        String json = mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":5,\"items\":[{\"productoId\":1,\"cantidad\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(39980))
                .andReturn().getResponse().getContentAsString();
        String id = json.replaceAll("^\\{\"id\":(\\d+).*", "$1").trim();

        mvc.perform(get("/api/pedidos").param("clienteId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items[0].nombreProducto").value("Audífonos"));

        mvc.perform(delete("/api/pedidos/" + id)).andExpect(status().isConflict());
        mvc.perform(patch("/api/pedidos/" + id + "/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
        mvc.perform(delete("/api/pedidos/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/pedidos/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void pedidoVacioDevuelve400() throws Exception {
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":5,\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
