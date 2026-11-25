package cl.costostock.usuarios.service;

import cl.costostock.usuarios.dto.Dtos.ClienteRegistroRequest;
import cl.costostock.usuarios.dto.Dtos.ClienteResponse;
import cl.costostock.usuarios.dto.Dtos.ClienteUpdateRequest;
import cl.costostock.usuarios.dto.Dtos.LoginRequest;
import cl.costostock.usuarios.exception.ApiExceptions.ConflictException;
import cl.costostock.usuarios.exception.ApiExceptions.InvalidCredentialsException;
import cl.costostock.usuarios.exception.ApiExceptions.NotFoundException;
import cl.costostock.usuarios.model.Cliente;
import cl.costostock.usuarios.repository.ClienteRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ClienteResponse registrar(ClienteRegistroRequest request) {
        String email = normalizarEmail(request.email());
        if (repository.existsByEmail(email)) {
            throw new ConflictException("Este correo ya está registrado");
        }
        Cliente cliente = new Cliente(request.nombre().trim(), email, passwordEncoder.encode(request.password()));
        return toResponse(repository.save(cliente));
    }

    @Transactional(readOnly = true)
    public ClienteResponse login(LoginRequest request) {
        Cliente cliente = repository.findByEmail(normalizarEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), cliente.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return toResponse(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return repository.findAll().stream().map(ClienteService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public ClienteResponse actualizar(Long id, ClienteUpdateRequest request) {
        Cliente cliente = buscar(id);
        cliente.setNombre(request.nombre().trim());
        return toResponse(repository.save(cliente));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private Cliente buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el cliente " + id));
    }

    static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static ClienteResponse toResponse(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNombre(), c.getEmail(), c.getCreadoEn());
    }
}
