package cl.costostock.usuarios.controller;

import cl.costostock.usuarios.dto.Dtos.LoginRequest;
import cl.costostock.usuarios.dto.Dtos.ProveedorRegistroRequest;
import cl.costostock.usuarios.dto.Dtos.ProveedorResponse;
import cl.costostock.usuarios.dto.Dtos.ProveedorUpdateRequest;
import cl.costostock.usuarios.service.ProveedorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService service;

    public ProveedorController(ProveedorService service) {
        this.service = service;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public ProveedorResponse registrar(@Valid @RequestBody ProveedorRegistroRequest request) {
        return service.registrar(request);
    }

    @PostMapping("/login")
    public ProveedorResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping
    public List<ProveedorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ProveedorResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PutMapping("/{id}")
    public ProveedorResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorUpdateRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
