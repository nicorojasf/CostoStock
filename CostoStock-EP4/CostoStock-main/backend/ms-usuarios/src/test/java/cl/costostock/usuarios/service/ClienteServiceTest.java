package cl.costostock.usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.Mockito;

class ClienteServiceTest {

    private ClienteRepository repository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private ClienteService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ClienteRepository.class);
        service = new ClienteService(repository, encoder);
        when(repository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            if (c.getId() == null) c.setId(1L);
            return c;
        });
    }

    @Test
    void registrarNormalizaCorreoYGuardaHash() {
        when(repository.existsByEmail("ana@mail.cl")).thenReturn(false);

        ClienteResponse r = service.registrar(new ClienteRegistroRequest(" Ana ", " ANA@mail.cl ", "Clave123!"));

        assertEquals(1L, r.id());
        assertEquals("Ana", r.nombre());
        assertEquals("ana@mail.cl", r.email());
        verify(repository).save(Mockito.<Cliente>argThat(c -> encoder.matches("Clave123!", c.getPasswordHash())));
    }

    @Test
    void registrarConCorreoDuplicadoLanzaConflicto() {
        when(repository.existsByEmail("ana@mail.cl")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.registrar(new ClienteRegistroRequest("Ana", "ana@mail.cl", "Clave123!")));
        verify(repository, never()).save(any());
    }

    @Test
    void loginCorrectoDevuelveCliente() {
        Cliente c = new Cliente("Ana", "ana@mail.cl", encoder.encode("Clave123!"));
        c.setId(5L);
        when(repository.findByEmail("ana@mail.cl")).thenReturn(Optional.of(c));

        assertEquals(5L, service.login(new LoginRequest("Ana@Mail.cl", "Clave123!")).id());
    }

    @Test
    void loginConClaveIncorrectaLanza401() {
        Cliente c = new Cliente("Ana", "ana@mail.cl", encoder.encode("Clave123!"));
        when(repository.findByEmail("ana@mail.cl")).thenReturn(Optional.of(c));

        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("ana@mail.cl", "otra")));
    }

    @Test
    void loginConCorreoInexistenteLanza401() {
        when(repository.findByEmail("x@mail.cl")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("x@mail.cl", "Clave123!")));
    }

    @Test
    void actualizarCambiaElNombre() {
        Cliente c = new Cliente("Ana", "ana@mail.cl", "hash");
        c.setId(2L);
        when(repository.findById(2L)).thenReturn(Optional.of(c));

        assertEquals("Ana María", service.actualizar(2L, new ClienteUpdateRequest(" Ana María ")).nombre());
    }

    @Test
    void obtenerInexistenteLanza404() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.obtener(9L));
    }

    @Test
    void listarYEliminar() {
        Cliente c = new Cliente("Ana", "ana@mail.cl", "hash");
        c.setId(3L);
        when(repository.findAll()).thenReturn(List.of(c));
        when(repository.findById(3L)).thenReturn(Optional.of(c));

        assertEquals(1, service.listar().size());
        service.eliminar(3L);
        verify(repository).delete(c);
    }
}
