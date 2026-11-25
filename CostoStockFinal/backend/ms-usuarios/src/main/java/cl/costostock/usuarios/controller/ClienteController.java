package cl.costostock.usuarios.controller;

import cl.costostock.usuarios.dto.Dtos.ClienteRegistroRequest;
import cl.costostock.usuarios.dto.Dtos.ClienteResponse;
import cl.costostock.usuarios.dto.Dtos.ClienteUpdateRequest;
import cl.costostock.usuarios.dto.Dtos.LoginRequest;
import cl.costostock.usuarios.dto.Dtos.SesionResponse;
import cl.costostock.usuarios.security.Permiso;
import cl.costostock.usuarios.security.Rol;
import cl.costostock.usuarios.security.TokenService;
import cl.costostock.usuarios.service.ClienteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Clientes. Registro y login son públicos y devuelven el token de sesión; el resto exige token:
 * cada cliente solo ve y modifica su cuenta, Soporte puede ver y Admin puede además eliminar.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;
    private final TokenService tokens;

    public ClienteController(ClienteService service, TokenService tokens) {
        this.service = service;
        this.tokens = tokens;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionResponse registrar(@Valid @RequestBody ClienteRegistroRequest request) {
        return sesion(service.registrar(request));
    }

    @PostMapping("/login")
    public SesionResponse login(@Valid @RequestBody LoginRequest request) {
        return sesion(service.login(request));
    }

    @GetMapping
    public List<ClienteResponse> listar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigir(Permiso.VER_TODO);
        return service.listar();
    }

    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable Long id,
                                   @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigirDuenoO(Rol.CLIENTE, id, Permiso.VER_TODO);
        return service.obtener(id);
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteUpdateRequest request,
                                      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigirDuenoO(Rol.CLIENTE, id, Permiso.GESTIONAR_EQUIPO);
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id,
                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigirDuenoO(Rol.CLIENTE, id, Permiso.ELIMINAR_REGISTROS);
        service.eliminar(id);
    }

    private SesionResponse sesion(ClienteResponse c) {
        return new SesionResponse(c.id(), c.nombre(), c.email(), null, null, c.creadoEn(), Rol.CLIENTE,
                tokens.emitir(c.id(), Rol.CLIENTE));
    }
}
