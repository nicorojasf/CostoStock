package cl.costostock.usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cl.costostock.usuarios.dto.Dtos.LoginRequest;
import cl.costostock.usuarios.dto.Dtos.ProveedorRegistroRequest;
import cl.costostock.usuarios.dto.Dtos.ProveedorUpdateRequest;
import cl.costostock.usuarios.exception.ApiExceptions.BadRequestException;
import cl.costostock.usuarios.exception.ApiExceptions.ConflictException;
import cl.costostock.usuarios.exception.ApiExceptions.InvalidCredentialsException;
import cl.costostock.usuarios.model.Proveedor;
import cl.costostock.usuarios.repository.ProveedorRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class ProveedorServiceTest {

    private ProveedorRepository repository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private ProveedorService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ProveedorRepository.class);
        service = new ProveedorService(repository, encoder);
        when(repository.save(any(Proveedor.class))).thenAnswer(inv -> {
            Proveedor p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });
    }

    private ProveedorRegistroRequest request(String rut) {
        return new ProveedorRegistroRequest("Tienda Ana", rut, "tienda@mail.cl", "+56912345678", "Clave123!");
    }

    @Test
    void rutValidoSegunModulo11() {
        assertTrue(ProveedorService.rutValido("12345678-5"));
        assertTrue(ProveedorService.rutValido("11111111-1"));
        assertFalse(ProveedorService.rutValido("12345678-9"));
        assertFalse(ProveedorService.rutValido("12345678"));
    }

    @Test
    void registrarProveedorValido() {
        assertEquals("12345678-5", service.registrar(request("12345678-5")).rut());
    }

    @Test
    void registrarConRutInvalidoLanza400() {
        assertThrows(BadRequestException.class, () -> service.registrar(request("12345678-9")));
    }

    @Test
    void registrarConCorreoORutDuplicadoLanza409() {
        when(repository.existsByEmail("tienda@mail.cl")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.registrar(request("12345678-5")));

        when(repository.existsByEmail("tienda@mail.cl")).thenReturn(false);
        when(repository.existsByRut("12345678-5")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.registrar(request("12345678-5")));
    }

    @Test
    void loginYActualizacion() {
        Proveedor p = new Proveedor("Tienda", "12345678-5", "tienda@mail.cl", "+56912345678",
                encoder.encode("Clave123!"));
        p.setId(7L);
        when(repository.findByEmail("tienda@mail.cl")).thenReturn(Optional.of(p));
        when(repository.findById(7L)).thenReturn(Optional.of(p));

        assertEquals(7L, service.login(new LoginRequest("tienda@mail.cl", "Clave123!")).id());
        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("tienda@mail.cl", "mala")));

        var actualizado = service.actualizar(7L, new ProveedorUpdateRequest("Tienda Nueva", "+56987654321"));
        assertEquals("Tienda Nueva", actualizado.nombre());
        assertEquals("+56987654321", actualizado.telefono());
    }
}
