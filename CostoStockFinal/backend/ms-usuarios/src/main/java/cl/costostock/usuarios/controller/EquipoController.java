package cl.costostock.usuarios.controller;

import cl.costostock.usuarios.dto.Dtos.EquipoCrearRequest;
import cl.costostock.usuarios.dto.Dtos.EquipoResponse;
import cl.costostock.usuarios.dto.Dtos.LoginRequest;
import cl.costostock.usuarios.dto.Dtos.SesionResponse;
import cl.costostock.usuarios.security.PerfilAcceso;
import cl.costostock.usuarios.security.Permiso;
import cl.costostock.usuarios.security.TokenService;
import cl.costostock.usuarios.service.EquipoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Equipo interno (panel web). Soporte puede ver al equipo; solo Admin crea o elimina cuentas. */
@RestController
@RequestMapping("/api/equipo")
public class EquipoController {

    private final EquipoService service;
    private final TokenService tokens;

    public EquipoController(EquipoService service, TokenService tokens) {
        this.service = service;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public SesionResponse login(@Valid @RequestBody LoginRequest request) {
        EquipoResponse u = service.login(request);
        return new SesionResponse(u.id(), u.nombre(), u.email(), null, null, u.creadoEn(), u.rol(),
                tokens.emitir(u.id(), u.rol()));
    }

    @GetMapping
    public List<EquipoResponse> listar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigir(Permiso.VER_TODO);
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoResponse crear(@Valid @RequestBody EquipoCrearRequest request,
                                @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        tokens.desdeEncabezado(auth).exigir(Permiso.GESTIONAR_EQUIPO);
        return service.crear(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id,
                         @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        PerfilAcceso perfil = tokens.desdeEncabezado(auth);
        perfil.exigir(Permiso.GESTIONAR_EQUIPO);
        service.eliminar(id, perfil.usuarioId());
    }
}
