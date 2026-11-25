package cl.costostock.productos.controller;

import cl.costostock.productos.dto.Dtos.EstadoRequest;
import cl.costostock.productos.dto.Dtos.ProductoRequest;
import cl.costostock.productos.dto.Dtos.ProductoResponse;
import cl.costostock.productos.dto.Dtos.StockRequest;
import cl.costostock.productos.security.PerfilAcceso;
import cl.costostock.productos.security.Permiso;
import cl.costostock.productos.security.Rol;
import cl.costostock.productos.security.TokenService;
import cl.costostock.productos.service.ProductoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publicaciones. Leer el catálogo es público. Escribir exige token:
 * - PROVEEDOR: crea y modifica solo sus publicaciones.
 * - SOPORTE: puede pausar o activar cualquier publicación.
 * - ADMIN: además puede eliminar cualquier publicación.
 * - SISTEMA (ms-pedidos): descuenta y repone stock.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private static final String AUTH = HttpHeaders.AUTHORIZATION;

    private final ProductoService service;
    private final TokenService tokens;

    public ProductoController(ProductoService service, TokenService tokens) {
        this.service = service;
        this.tokens = tokens;
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
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request,
                                  @RequestHeader(value = AUTH, required = false) String auth) {
        PerfilAcceso perfil = tokens.desdeEncabezado(auth);
        perfil.exigir(Permiso.PUBLICAR);
        // El dueño siempre es quien inicia sesión, aunque el cuerpo diga otra cosa.
        return service.crear(conProveedor(request, perfil.usuarioId()));
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request,
                                       @RequestHeader(value = AUTH, required = false) String auth) {
        PerfilAcceso perfil = tokens.desdeEncabezado(auth);
        ProductoResponse actual = service.obtener(id);
        perfil.exigirDuenoO(Rol.PROVEEDOR, actual.proveedorId(), Permiso.ELIMINAR_REGISTROS);
        return service.actualizar(id, conProveedor(request, actual.proveedorId()));
    }

    @PatchMapping("/{id}/estado")
    public ProductoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request,
                                          @RequestHeader(value = AUTH, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigirDuenoO(Rol.PROVEEDOR, service.obtener(id).proveedorId(),
                Permiso.MODERAR_PUBLICACIONES);
        return service.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @RequestHeader(value = AUTH, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigirDuenoO(Rol.PROVEEDOR, service.obtener(id).proveedorId(),
                Permiso.ELIMINAR_REGISTROS);
        service.eliminar(id);
    }

    /** Uso interno: ms-pedidos al confirmar una compra. */
    @PostMapping("/stock/descontar")
    public List<ProductoResponse> descontar(@Valid @RequestBody StockRequest request,
                                            @RequestHeader(value = AUTH, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigir(Permiso.GESTIONAR_STOCK);
        return service.descontarStock(request.items());
    }

    /** Uso interno: ms-pedidos al cancelar una compra. */
    @PostMapping("/stock/reponer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reponer(@Valid @RequestBody StockRequest request,
                        @RequestHeader(value = AUTH, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigir(Permiso.GESTIONAR_STOCK);
        service.reponerStock(request.items());
    }

    private static ProductoRequest conProveedor(ProductoRequest r, Long proveedorId) {
        return new ProductoRequest(proveedorId, r.nombre(), r.categoria(), r.descripcion(), r.precio(), r.stock(),
                r.comuna(), r.emoji(), r.condicion(), r.imagenes());
    }
}
