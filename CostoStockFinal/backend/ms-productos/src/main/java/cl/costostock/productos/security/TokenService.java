package cl.costostock.productos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tokens de sesión firmados con HMAC-SHA256 (mismo formato que un JWT simplificado):
 * {@code base64url(json) + "." + base64url(firma)}. Los tres microservicios comparten el secreto,
 * así que cualquiera puede verificar un token emitido por ms-usuarios sin consultar la base de datos.
 */
@Component
public class TokenService {

    public record Contenido(long id, Rol rol, long exp) {
    }

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper mapper = new ObjectMapper();
    private final byte[] secreto;
    private final long duracionMs;

    public TokenService(@Value("${costostock.seguridad.secreto}") String secreto,
                        @Value("${costostock.seguridad.horas:168}") long horas) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.duracionMs = horas * 3_600_000L;
    }

    public String emitir(long usuarioId, Rol rol) {
        try {
            byte[] json = mapper.writeValueAsBytes(
                    new Contenido(usuarioId, rol, System.currentTimeMillis() + duracionMs));
            String cuerpo = ENCODER.encodeToString(json);
            return cuerpo + "." + ENCODER.encodeToString(firmar(cuerpo));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo emitir el token", e);
        }
    }

    /** Valida firma y vencimiento; devuelve el perfil del rol correspondiente. */
    public PerfilAcceso verificar(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 2) {
                throw new NoAutenticadoException("Token mal formado");
            }
            byte[] firma = DECODER.decode(partes[1]);
            if (!MessageDigest.isEqual(firma, firmar(partes[0]))) {
                throw new NoAutenticadoException("Token inválido");
            }
            Contenido c = mapper.readValue(DECODER.decode(partes[0]), Contenido.class);
            if (c.exp() < System.currentTimeMillis()) {
                throw new NoAutenticadoException("Tu sesión expiró, vuelve a iniciar sesión");
            }
            return PerfilAcceso.de(c.rol(), c.id());
        } catch (NoAutenticadoException e) {
            throw e;
        } catch (Exception e) {
            throw new NoAutenticadoException("Token inválido");
        }
    }

    /** Lee el encabezado {@code Authorization: Bearer <token>}. Si falta, responde 401. */
    public PerfilAcceso desdeEncabezado(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new NoAutenticadoException("Debes iniciar sesión para realizar esta acción");
        }
        return verificar(authorization.substring(7).trim());
    }

    private byte[] firmar(String cuerpo) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
        return mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8));
    }
}
