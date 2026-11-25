package cl.costostock.usuarios.controller;

import cl.costostock.usuarios.dto.Dtos.MensajeResponse;
import cl.costostock.usuarios.dto.Dtos.RestablecerRequest;
import cl.costostock.usuarios.dto.Dtos.SolicitarCodigoRequest;
import cl.costostock.usuarios.service.RecuperacionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Recuperación de contraseña (público): 1) pedir código, 2) restablecer con el código. */
@RestController
@RequestMapping("/api/recuperacion")
public class RecuperacionController {

    private final RecuperacionService service;

    public RecuperacionController(RecuperacionService service) {
        this.service = service;
    }

    @PostMapping("/solicitar")
    public MensajeResponse solicitar(@Valid @RequestBody SolicitarCodigoRequest request) {
        service.solicitar(request.email(), request.tipo());
        return new MensajeResponse("Si el correo está registrado, te enviamos un código de 6 dígitos");
    }

    @PostMapping("/restablecer")
    public MensajeResponse restablecer(@Valid @RequestBody RestablecerRequest request) {
        service.restablecer(request.email(), request.tipo(), request.codigo(), request.nuevaPassword());
        return new MensajeResponse("Tu contraseña fue actualizada. Ya puedes iniciar sesión");
    }
}
