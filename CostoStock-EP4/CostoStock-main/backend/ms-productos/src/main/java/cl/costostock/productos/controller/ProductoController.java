package cl.costostock.productos.controller;

import cl.costostock.productos.dto.Dtos.EstadoRequest;
import cl.costostock.productos.dto.Dtos.ProductoRequest;
import cl.costostock.productos.dto.Dtos.ProductoResponse;
import cl.costostock.productos.dto.Dtos.StockRequest;
import cl.costostock.productos.service.ProductoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductoResponse> listar(
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String comuna,
            @RequestParam(defaultValue = "false") boolean soloDisponibles) {
        return service.listar(proveedorId, categoria, comuna, soloDisponibles);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return service.crear(request);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public ProductoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    /** Usado por ms-pedidos al confirmar una compra. */
    @PostMapping("/stock/descontar")
    public List<ProductoResponse> descontar(@Valid @RequestBody StockRequest request) {
        return service.descontarStock(request.items());
    }

    /** Usado por ms-pedidos al cancelar una compra. */
    @PostMapping("/stock/reponer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reponer(@Valid @RequestBody StockRequest request) {
        service.reponerStock(request.items());
    }
}
