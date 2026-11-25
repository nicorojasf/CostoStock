package cl.costostock.usuarios.service;

import cl.costostock.usuarios.exception.ApiExceptions.BadRequestException;
import cl.costostock.usuarios.model.CodigoRecuperacion;
import cl.costostock.usuarios.model.TipoCuenta;
import cl.costostock.usuarios.repository.ClienteRepository;
import cl.costostock.usuarios.repository.CodigoRecuperacionRepository;
import cl.costostock.usuarios.repository.ProveedorRepository;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperación de contraseña con un código de 6 dígitos que vence en 10 minutos y admite
 * hasta 5 intentos. El código se guarda cifrado y se "envía" con {@link NotificadorCorreo}.
 */
@Service
public class RecuperacionService {

    static final int MINUTOS_VIGENCIA = 10;
    static final int MAX_INTENTOS = 5;
    static final String CODIGO_INVALIDO = "El código no es válido o ya venció. Solicita uno nuevo";

    private final ClienteRepository clientes;
    private final ProveedorRepository proveedores;
    private final CodigoRecuperacionRepository codigos;
    private final PasswordEncoder encoder;
    private final NotificadorCorreo notificador;
    private final SecureRandom random = new SecureRandom();

    public RecuperacionService(ClienteRepository clientes, ProveedorRepository proveedores,
                               CodigoRecuperacionRepository codigos, PasswordEncoder encoder,
                               NotificadorCorreo notificador) {
        this.clientes = clientes;
        this.proveedores = proveedores;
        this.codigos = codigos;
        this.encoder = encoder;
        this.notificador = notificador;
    }

    /**
     * Genera y envía un código. Si el correo no existe no hace nada, pero responde igual para no
     * revelar qué correos están registrados.
     */
    @Transactional
    public void solicitar(String emailIngresado, TipoCuenta tipo) {
        String email = normalizar(emailIngresado);
        boolean existe = tipo == TipoCuenta.CLIENTE ? clientes.existsByEmail(email) : proveedores.existsByEmail(email);
        if (!existe) {
            return;
        }
        codigos.deleteByEmailAndTipo(email, tipo);
        String codigo = String.format("%06d", random.nextInt(1_000_000));
        long expira = System.currentTimeMillis() + MINUTOS_VIGENCIA * 60_000L;
        codigos.save(new CodigoRecuperacion(email, tipo, encoder.encode(codigo), expira));
        notificador.enviarCodigo(email, codigo, MINUTOS_VIGENCIA);
    }

    /**
     * Cambia la contraseña si el código es correcto. noRollbackFor: el contador de intentos fallidos
     * se guarda aunque la operación termine con error.
     */
    @Transactional(noRollbackFor = BadRequestException.class)
    public void restablecer(String emailIngresado, TipoCuenta tipo, String codigo, String nuevaPassword) {
        String email = normalizar(emailIngresado);
        CodigoRecuperacion c = codigos.findFirstByEmailAndTipoAndUsadoFalseOrderByIdDesc(email, tipo)
                .orElseThrow(() -> new BadRequestException(CODIGO_INVALIDO));
        if (c.getExpiraEn() < System.currentTimeMillis()) {
            throw new BadRequestException(CODIGO_INVALIDO);
        }
        if (c.getIntentos() >= MAX_INTENTOS) {
            throw new BadRequestException("Demasiados intentos fallidos. Solicita un código nuevo");
        }
        if (!encoder.matches(codigo, c.getCodigoHash())) {
            c.setIntentos(c.getIntentos() + 1);
            codigos.save(c);
            throw new BadRequestException("Código incorrecto. Te quedan " + (MAX_INTENTOS - c.getIntentos()) + " intentos");
        }
        c.setUsado(true);
        codigos.save(c);
        String hash = encoder.encode(nuevaPassword);
        if (tipo == TipoCuenta.CLIENTE) {
            clientes.findByEmail(email).ifPresent(cliente -> {
                cliente.setPasswordHash(hash);
                clientes.save(cliente);
            });
        } else {
            proveedores.findByEmail(email).ifPresent(proveedor -> {
                proveedor.setPasswordHash(hash);
                proveedores.save(proveedor);
            });
        }
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
