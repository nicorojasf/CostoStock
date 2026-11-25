package cl.costostock.usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.costostock.usuarios.dto.Dtos.EquipoCrearRequest;
import cl.costostock.usuarios.dto.Dtos.LoginRequest;
import cl.costostock.usuarios.exception.ApiExceptions.BadRequestException;
import cl.costostock.usuarios.exception.ApiExceptions.ConflictException;
import cl.costostock.usuarios.exception.ApiExceptions.InvalidCredentialsException;
import cl.costostock.usuarios.exception.ApiExceptions.NotFoundException;
import cl.costostock.usuarios.model.UsuarioInterno;
import cl.costostock.usuarios.repository.UsuarioInternoRepository;
import cl.costostock.usuarios.security.Rol;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class EquipoServiceTest {

    private final UsuarioInternoRepository repository = Mockito.mock(UsuarioInternoRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private EquipoService service;

    @BeforeEach
    void setUp() {
        service = new EquipoService(repository, encoder);
        when(repository.save(any(UsuarioInterno.class))).thenAnswer(inv -> {
            UsuarioInterno u = inv.getArgument(0);
            if (u.getId() == null) u.setId(3L);
            return u;
        });
    }

    @Test
    void loginDelEquipo() {
        UsuarioInterno admin = new UsuarioInterno("Admin", "admin@costostock.cl", encoder.encode("Admin1234"), Rol.ADMIN);
        admin.setId(1L);
        when(repository.findByEmail("admin@costostock.cl")).thenReturn(Optional.of(admin));

        assertEquals(Rol.ADMIN, service.login(new LoginRequest("ADMIN@costostock.cl", "Admin1234")).rol());
        assertThrows(InvalidCredentialsException.class, () -> service.login(new LoginRequest("admin@costostock.cl", "mala")));
        assertThrows(InvalidCredentialsException.class, () -> service.login(new LoginRequest("x@costostock.cl", "Admin1234")));
    }

    @Test
    void crearSoloSoporteOAdminYSinDuplicados() {
        assertEquals(Rol.SOPORTE, service.crear(new EquipoCrearRequest("Soporte", "s@costostock.cl", "Clave1234", Rol.SOPORTE)).rol());
        assertThrows(BadRequestException.class,
                () -> service.crear(new EquipoCrearRequest("X", "c@costostock.cl", "Clave1234", Rol.CLIENTE)));
        when(repository.existsByEmail("s@costostock.cl")).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> service.crear(new EquipoCrearRequest("Soporte", "s@costostock.cl", "Clave1234", Rol.SOPORTE)));
    }

    @Test
    void eliminarListarYSemilla() {
        UsuarioInterno u = new UsuarioInterno("Soporte", "s@costostock.cl", "hash", Rol.SOPORTE);
        u.setId(2L);
        when(repository.findById(2L)).thenReturn(Optional.of(u));
        when(repository.findAll()).thenReturn(List.of(u));

        assertThrows(BadRequestException.class, () -> service.eliminar(1L, 1L));
        assertThrows(NotFoundException.class, () -> service.eliminar(9L, 1L));
        service.eliminar(2L, 1L);
        verify(repository).delete(u);
        assertEquals(1, service.listar().size());

        when(repository.existsByEmail("admin@costostock.cl")).thenReturn(true);
        service.crearSiNoExiste("Admin", "admin@costostock.cl", "Admin1234", Rol.ADMIN);
        verify(repository, never()).save(Mockito.<UsuarioInterno>argThat(x -> "admin@costostock.cl".equals(x.getEmail())));
    }
}
