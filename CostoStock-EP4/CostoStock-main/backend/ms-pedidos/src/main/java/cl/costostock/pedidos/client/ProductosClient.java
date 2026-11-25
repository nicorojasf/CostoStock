package cl.costostock.pedidos.client;

import cl.costostock.pedidos.exception.ApiExceptions.BadRequestException;
import cl.costostock.pedidos.exception.ApiExceptions.ServicioNoDisponibleException;
import cl.costostock.pedidos.exception.ApiExceptions.StockInsuficienteException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** Comunicación microservicio a microservicio: ms-pedidos → ms-productos. */
@Component
public class ProductosClient {

    public record StockItem(Long productoId, Integer cantidad) {
    }

    public record StockRequest(List<StockItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductoDto(Long id, String nombre, String emoji, int precio, int stock) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorBody(String mensaje, List<String> detalles) {
    }

    private final RestClient restClient;

    public ProductosClient(RestClient productosRestClient) {
        this.restClient = productosRestClient;
    }

    /** Descuenta stock y devuelve los productos actualizados (con su precio vigente). */
    public List<ProductoDto> descontarStock(List<StockItem> items) {
        try {
            ProductoDto[] respuesta = restClient.post()
                    .uri("/api/productos/stock/descontar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StockRequest(items))
                    .retrieve()
                    .body(ProductoDto[].class);
            return respuesta == null ? List.of() : Arrays.asList(respuesta);
        } catch (HttpClientErrorException.Conflict e) {
            ErrorBody body = e.getResponseBodyAs(ErrorBody.class);
            throw new StockInsuficienteException(body == null || body.detalles() == null ? List.of() : body.detalles());
        } catch (HttpClientErrorException.NotFound e) {
            throw new BadRequestException("Uno de los productos ya no existe");
        } catch (ResourceAccessException e) {
            throw new ServicioNoDisponibleException();
        }
    }

    /** Devuelve el stock de un pedido cancelado. */
    public void reponerStock(List<StockItem> items) {
        try {
            restClient.post()
                    .uri("/api/productos/stock/reponer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new StockRequest(items))
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new ServicioNoDisponibleException();
        }
    }
}
