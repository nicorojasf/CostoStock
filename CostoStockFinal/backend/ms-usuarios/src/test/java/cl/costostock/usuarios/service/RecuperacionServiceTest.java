package cl.costostock.usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.costostock.usuarios.exception.ApiExceptions.BadRequestException;
import cl.costostock.usuarios.model.Cliente;
import cl.costostock.usuarios.model.CodigoRecuperacion;
import cl.costostock.usuarios.model.Proveedor;
import cl.costostock.usuarios.model.TipoCuenta;
import cl.costostock.usuarios.repository.ClienteRepository;
import cl.costostock.usuarios.repository.CodigoRecuperacionRepository;
import cl.costostock.usuarios.repository.ProveedorRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class RecuperacionServiceTest {

    private final ClienteRepository clientes = Mockito.mock(ClienteRepository.class);
    private final ProveedorRepository proveedores = Mockito.mock(ProveedorRepository.class);
    private final CodigoRecuperacionRepository codigos = Mockito.mock(CodigoRecuperacionRepository.class);
    private final NotificadorCorreo notificador = Mockito.mock(NotificadorCorreo.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private RecuperacionService service;

    @BeforeEach
    void setUp() {
        service = new RecuperacionService(clientes, proveedores, codigos, encoder, notificador);
    }

    @Test
    void solicitarGeneraCodigoDe6DigitosYLoEnvia() {
        when(clientes.existsByEmail("ana@mail.cl")).thenReturn(true);

        service.solicitar(" ANA@mail.cl ", TipoCuenta.CLIENTE);

        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(notificador).enviarCodigo(Mockito.eq("ana@mail.cl"), codigo.capture(), anyInt());
        assertTrue(codigo.getValue().matches("\\d{6}"));
        verify(codigos).deleteByEmailAndTipo("ana@mail.cl", TipoCuenta.CLIENTE);
        verify(codigos).save(Mockito.<CodigoRecuperacion>argThat(c -> encoder.matches(codigo.getValue(), c.getCodigoHash())));
    }

    @Test
    void correoInexistenteNoEnviaNada() {
        service.solicitar("nadie@mail.cl", TipoCuenta.PROVEEDOR);
        verify(notificador, never()).enviarCodigo(anyString(), anyString(), anyInt());
    }

    private CodigoRecuperacion codigo(String plano, long expira) {
        return new CodigoRecuperacion("ana@mail.cl", TipoCuenta.CLIENTE, encoder.encode(plano), expira);
    }

    @Test
    void restablecerConCodigoCorrectoCambiaLaContrasena() {
        Cliente cliente = new Cliente("Ana", "ana@mail.cl", "hash-viejo");
        CodigoRecuperacion c = codigo("123456", System.currentTimeMillis() + 60_000);
        when(codigos.findFirstByEmailAndTipoAndUsadoFalseOrderByIdDesc("ana@mail.cl", TipoCuenta.CLIENTE)).thenReturn(Optional.of(c));
        when(clientes.findByEmail("ana@mail.cl")).thenReturn(Optional.of(cliente));

        service.restablecer("ana@mail.cl", TipoCuenta.CLIENTE, "123456", "Nueva1234");

        assertTrue(c.isUsado());
        assertTrue(encoder.matches("Nueva1234", cliente.getPasswordHash()));
    }

    @Test
    void restablecerProveedor() {
        Proveedor p = new Proveedor("Tienda", "12345678-5", "ana@mail.cl", "+56912345678", "hash");
        CodigoRecuperacion c = new CodigoRecuperacion("ana@mail.cl", TipoCuenta.PROVEEDOR, encoder.encode("654321"),
                System.currentTimeMillis() + 60_000);
        when(codigos.findFirstByEmailAndTipoAndUsadoFalseOrderByIdDesc("ana@mail.cl", TipoCuenta.PROVEEDOR)).thenReturn(Optional.of(c));
        when(proveedores.findByEmail("ana@mail.cl")).thenReturn(Optional.of(p));

        service.restablecer("ana@mail.cl", TipoCuenta.PROVEEDOR, "654321", "Nueva1234");

        assertTrue(encoder.matches("Nueva1234", p.getPasswordHash()));
    }

    @Test
    void codigoIncorrectoVencidoOAgotado() {
        CodigoRecuperacion c = codigo("123456", System.currentTimeMillis() + 60_000);
        when(codigos.findFirstByEmailAndTipoAndUsadoFalseOrderByIdDesc("ana@mail.cl", TipoCuenta.CLIENTE)).thenReturn(Optional.of(c));

        assertThrows(BadRequestException.class, () -> service.restablecer("ana@mail.cl", TipoCuenta.CLIENTE, "000000", "Nueva1234"));
        assertEquals(1, c.getIntentos());

        c.setIntentos(RecuperacionService.MAX_INTENTOS);
        BadRequestException agotado = assertThrows(BadRequestException.class,
                () -> service.restablecer("ana@mail.cl", TipoCuenta.CLIENTE, "123456", "Nueva1234"));
        assertTrue(agotado.getMessage().contains("Demasiados intentos"));

        c.setIntentos(0);
        c.setExpiraEn(System.currentTimeMillis() - 1);
        assertThrows(BadRequestException.class, () -> service.restablecer("ana@mail.cl", TipoCuenta.CLIENTE, "123456", "Nueva1234"));

        when(codigos.findFirstByEmailAndTipoAndUsadoFalseOrderByIdDesc("x@mail.cl", TipoCuenta.CLIENTE)).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> service.restablecer("x@mail.cl", TipoCuenta.CLIENTE, "123456", "Nueva1234"));
        verify(clientes, never()).save(any());
    }
}
