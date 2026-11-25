package cl.costostock.productos.dto;

import cl.costostock.productos.model.Producto.Condicion;
import cl.costostock.productos.model.Producto.Estado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class Dtos {

    private Dtos() {
    }

    /** Datos para crear o editar una publicación. */
    public record ProductoRequest(
            Long proveedorId,
            @NotBlank(message = "El título es obligatorio")
            @Size(min = 3, max = 80, message = "El título debe tener entre 3 y 80 caracteres")
            String nombre,
            @NotBlank(message = "La categoría es obligatoria")
            String categoria,
            @NotBlank(message = "La descripción es obligatoria")
            @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
            String descripcion,
            @NotNull(message = "El precio es obligatorio")
            @Min(value = 1, message = "El precio debe ser mayor a 0")
            @Max(value = 99_999_999, message = "El precio es demasiado alto")
            Integer precio,
            @NotNull(message = "El stock es obligatorio")
            @Min(value = 0, message = "El stock no puede ser negativo")
            @Max(value = 99_999, message = "El stock es demasiado alto")
            Integer stock,
            @NotBlank(message = "La comuna es obligatoria")
            String comuna,
            String emoji,
            Condicion condicion,
            @Size(max = 3, message = "Máximo 3 fotos")
            List<String> imagenes) {
    }

    public record EstadoRequest(@NotNull(message = "El estado es obligatorio") Estado estado) {
    }

    public record ProductoResponse(Long id, Long proveedorId, String nombre, String categoria, String descripcion,
                                   int precio, int stock, String comuna, String emoji, Condicion condicion,
                                   List<String> imagenes, Estado estado, long creadoEn, long actualizadoEn) {
    }

    public record StockItem(
            @NotNull(message = "El producto es obligatorio") Long productoId,
            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser al menos 1")
            Integer cantidad) {
    }

    /** Lista de productos y cantidades para descontar o reponer stock. */
    public record StockRequest(
            @NotEmpty(message = "Debe indicar al menos un producto")
            List<@Valid StockItem> items) {
    }
}
