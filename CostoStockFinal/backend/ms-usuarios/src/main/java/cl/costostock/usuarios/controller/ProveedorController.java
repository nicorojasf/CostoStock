package cl.costostock.usuarios.controller;

import cl.costostock.usuarios.dto.Dtos.LoginRequest;
import cl.costostock.usuarios.dto.Dtos.ProveedorRegistroRequest;
import cl.costostock.usuarios.dto.Dtos.ProveedorResponse;
import cl.costostock.usuarios.dto.Dtos.ProveedorUpdateRequest;
import cl.costostock.usuarios.dto.Dtos.SesionResponse;
import cl.costostock.usuarios.security.Permiso;
import cl.costostock.usuarios.security.Rol;
import cl.costostock.usuarios.security.TokenService;
import cl.costostock.usuarios.service.ProveedorService;
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

/** Proveedores: mismas reglas que los clientes, con el rol PROVEEDOR. */
@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService service;
    private final TokenService tokens;

    public ProveedorController(ProveedorService service, TokenService tokens) {
        this.service = service;
        this.tokens = tokens;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionResponse registrar(@Valid @RequestBody ProveedorRegistroRequest request) {
        return sesion(service.registrar(request));
    }

    @PostMapping("/login")
    public SesionResponse login(@Valid @RequestBody LoginRequest request) {
        return sesion(service.login(request));
    }

    @GetMapping
    public List<ProveedorResponse> listar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigir(Permiso.VER_TODO);
        return service.listar();
    }

    @GetMapping("/{id}")
    public ProveedorResponse obtener(@PathVariable Long id,
                                     @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigirDuenoO(Rol.PROVEEDOR, id, Permiso.VER_TODO);
        return service.obtener(id);
    }

    @PutMapping("/{id}")
    public ProveedorResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorUpdateRequest request,
                                        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigirDuenoO(Rol.PROVEEDOR, id, Permiso.GESTIONAR_EQUIPO);
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id,
                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigirDuenoO(Rol.PROVEEDOR, id, Permiso.ELIMINAR_REGISTROS);
        service.eliminar(id);
    }

    private SesionResponse sesion(ProveedorResponse p) {
        return new SesionResponse(p.id(), p.nombre(), p.email(), p.rut(), p.telefono(), p.creadoEn(), Rol.PROVEEDOR,
                tokens.emitir(p.id(), Rol.PROVEEDOR));
    }
}
