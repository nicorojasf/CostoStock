package cl.costostock.pedidos.controller;

import cl.costostock.pedidos.dto.Dtos.PedidoRequest;
import cl.costostock.pedidos.dto.Dtos.PedidoResponse;
import cl.costostock.pedidos.security.AccesoDenegadoException;
import cl.costostock.pedidos.security.PerfilAcceso;
import cl.costostock.pedidos.security.Permiso;
import cl.costostock.pedidos.security.Rol;
import cl.costostock.pedidos.security.TokenService;
import cl.costostock.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pedidos. Todo exige token:
 * - CLIENTE: compra y ve, cancela o elimina solo sus pedidos.
 * - SOPORTE: ve todos los pedidos y puede cancelarlos.
 * - ADMIN: además puede eliminar cualquier pedido cancelado.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private static final String AUTH = HttpHeaders.AUTHORIZATION;

    private final PedidoService service;
    private final TokenService tokens;

    public PedidoController(PedidoService service, TokenService tokens) {
        this.service = service;
        this.tokens = tokens;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@Valid @RequestBody PedidoRequest request,
                                @RequestHeader(value = AUTH, required = false) String auth) {
        PerfilAcceso perfil = tokens.desdeEncabezado(auth);
        perfil.exigir(Permiso.COMPRAR);
        if (!perfil.esDueno(Rol.CLIENTE, request.clienteId())) {
            throw new AccesoDenegadoException("Solo puedes comprar con tu propia cuenta");
        }
        return service.crear(request);
    }

    @GetMapping
    public List<PedidoResponse> listar(@RequestParam(required = false) Long clienteId,
                                       @RequestHeader(value = AUTH, required = false) String auth) {
        PerfilAcceso perfil = tokens.desdeEncabezado(auth);
        if (clienteId == null) {
            perfil.exigir(Permiso.VER_TODO);
        } else {
            perfil.exigirDuenoO(Rol.CLIENTE, clienteId, Permiso.VER_TODO);
        }
        return service.listar(clienteId);
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id, @RequestHeader(value = AUTH, required = false) String auth) {
        PerfilAcceso perfil = tokens.desdeEncabezado(auth);
        PedidoResponse pedido = service.obtener(id);
        perfil.exigirDuenoO(Rol.CLIENTE, pedido.clienteId(), Permiso.VER_TODO);
        return pedido;
    }

    @PatchMapping("/{id}/cancelar")
    public PedidoResponse cancelar(@PathVariable Long id, @RequestHeader(value = AUTH, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigirDuenoO(Rol.CLIENTE, service.obtener(id).clienteId(), Permiso.CANCELAR_PEDIDOS);
        return service.cancelar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @RequestHeader(value = AUTH, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigirDuenoO(Rol.CLIENTE, service.obtener(id).clienteId(), Permiso.ELIMINAR_REGISTROS);
        service.eliminar(id);
    }
}
