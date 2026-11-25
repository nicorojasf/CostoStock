package cl.costostock.usuarios.service;

import cl.costostock.usuarios.dto.Dtos.EquipoCrearRequest;
import cl.costostock.usuarios.dto.Dtos.EquipoResponse;
import cl.costostock.usuarios.dto.Dtos.LoginRequest;
import cl.costostock.usuarios.exception.ApiExceptions.BadRequestException;
import cl.costostock.usuarios.exception.ApiExceptions.ConflictException;
import cl.costostock.usuarios.exception.ApiExceptions.InvalidCredentialsException;
import cl.costostock.usuarios.exception.ApiExceptions.NotFoundException;
import cl.costostock.usuarios.model.UsuarioInterno;
import cl.costostock.usuarios.repository.UsuarioInternoRepository;
import cl.costostock.usuarios.security.Rol;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cuentas del equipo interno (Soporte y Admin) que usan el panel web. */
@Service
public class EquipoService {

    private final UsuarioInternoRepository repository;
    private final PasswordEncoder encoder;

    public EquipoService(UsuarioInternoRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public EquipoResponse login(LoginRequest request) {
        UsuarioInterno u = repository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(InvalidCredentialsException::new);
        if (!encoder.matches(request.password(), u.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return toResponse(u);
    }

    @Transactional(readOnly = true)
    public List<EquipoResponse> listar() {
        return repository.findAll().stream().map(EquipoService::toResponse).toList();
    }

    @Transactional
    public EquipoResponse crear(EquipoCrearRequest request) {
        if (request.rol() != Rol.SOPORTE && request.rol() != Rol.ADMIN) {
            throw new BadRequestException("El rol debe ser SOPORTE o ADMIN");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmail(email)) {
            throw new ConflictException("Este correo ya está registrado");
        }
        return toResponse(repository.save(
                new UsuarioInterno(request.nombre().trim(), email, encoder.encode(request.password()), request.rol())));
    }

    @Transactional
    public void eliminar(Long id, long solicitanteId) {
        if (id == solicitanteId) {
            throw new BadRequestException("No puedes eliminar tu propia cuenta");
        }
        UsuarioInterno u = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el integrante " + id));
        repository.delete(u);
    }

    /** Crea las cuentas de ejemplo si no existen. */
    @Transactional
    public void crearSiNoExiste(String nombre, String email, String password, Rol rol) {
        if (!repository.existsByEmail(email)) {
            repository.save(new UsuarioInterno(nombre, email, encoder.encode(password), rol));
        }
    }

    static EquipoResponse toResponse(UsuarioInterno u) {
        return new EquipoResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.getCreadoEn());
    }
}
