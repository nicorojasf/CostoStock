package cl.costostock.usuarios.service;

import cl.costostock.usuarios.dto.Dtos.LoginRequest;
import cl.costostock.usuarios.dto.Dtos.ProveedorRegistroRequest;
import cl.costostock.usuarios.dto.Dtos.ProveedorResponse;
import cl.costostock.usuarios.dto.Dtos.ProveedorUpdateRequest;
import cl.costostock.usuarios.exception.ApiExceptions.BadRequestException;
import cl.costostock.usuarios.exception.ApiExceptions.ConflictException;
import cl.costostock.usuarios.exception.ApiExceptions.InvalidCredentialsException;
import cl.costostock.usuarios.exception.ApiExceptions.NotFoundException;
import cl.costostock.usuarios.model.Proveedor;
import cl.costostock.usuarios.repository.ProveedorRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProveedorService {

    private final ProveedorRepository repository;
    private final PasswordEncoder passwordEncoder;

    public ProveedorService(ProveedorRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ProveedorResponse registrar(ProveedorRegistroRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String rut = request.rut().trim().toUpperCase(Locale.ROOT);
        if (!rutValido(rut)) {
            throw new BadRequestException("El dígito verificador del RUT no es válido");
        }
        if (repository.existsByEmail(email)) {
            throw new ConflictException("Este correo ya está registrado");
        }
        if (repository.existsByRut(rut)) {
            throw new ConflictException("Este RUT ya está registrado");
        }
        Proveedor proveedor = new Proveedor(request.nombre().trim(), rut, email, request.telefono(),
                passwordEncoder.encode(request.password()));
        return toResponse(repository.save(proveedor));
    }

    @Transactional(readOnly = true)
    public ProveedorResponse login(LoginRequest request) {
        Proveedor proveedor = repository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), proveedor.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return toResponse(proveedor);
    }

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return repository.findAll().stream().map(ProveedorService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorUpdateRequest request) {
        Proveedor proveedor = buscar(id);
        proveedor.setNombre(request.nombre().trim());
        proveedor.setTelefono(request.telefono());
        return toResponse(repository.save(proveedor));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    private Proveedor buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el proveedor " + id));
    }

    /** Valida el dígito verificador con el algoritmo módulo 11. Espera el formato 12345678-5. */
    static boolean rutValido(String rut) {
        String[] partes = rut.split("-");
        if (partes.length != 2 || partes[1].length() != 1) {
            return false;
        }
        int suma = 0;
        int factor = 2;
        for (int i = partes[0].length() - 1; i >= 0; i--) {
            char c = partes[0].charAt(i);
            if (!Character.isDigit(c)) {
                return false;
            }
            suma += (c - '0') * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int resto = 11 - (suma % 11);
        char esperado = resto == 11 ? '0' : resto == 10 ? 'K' : (char) ('0' + resto);
        return Character.toUpperCase(partes[1].charAt(0)) == esperado;
    }

    static ProveedorResponse toResponse(Proveedor p) {
        return new ProveedorResponse(p.getId(), p.getNombre(), p.getRut(), p.getEmail(), p.getTelefono(),
                p.getCreadoEn());
    }
}
