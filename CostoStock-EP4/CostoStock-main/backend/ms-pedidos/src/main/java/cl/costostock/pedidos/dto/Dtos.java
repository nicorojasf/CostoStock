package cl.costostock.pedidos.dto;

import cl.costostock.pedidos.model.Pedido.Estado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class Dtos {

    private Dtos() {
    }

    public record ItemRequest(
            @NotNull(message = "El producto es obligatorio") Long productoId,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser al menos 1")
            Integer cantidad) {
    }

    public record PedidoRequest(
            @NotNull(message = "El cliente es obligatorio") Long clienteId,
            @NotEmpty(message = "El pedido debe tener al menos un producto")
            List<@Valid ItemRequest> items) {
    }

    public record ItemResponse(Long productoId, String nombreProducto, String emoji, int precioUnitario,
                               int cantidad, int subtotal) {
    }

    public record PedidoResponse(Long id, Long clienteId, long creadoEn, int neto, int iva, int total,
                                 Estado estado, int cantidadTotal, List<ItemResponse> items) {
    }
}
