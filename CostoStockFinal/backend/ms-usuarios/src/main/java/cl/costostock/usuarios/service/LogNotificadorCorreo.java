package cl.costostock.usuarios.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Simula el correo escribiendo el código en la consola de ms-usuarios. */
@Component
public class LogNotificadorCorreo implements NotificadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(LogNotificadorCorreo.class);

    @Override
    public void enviarCodigo(String email, String codigo, int minutosVigencia) {
        log.warn("\n==================== CORREO SIMULADO ====================\n"
                + " Para: {}\n Asunto: Recupera tu contraseña de CostoStock\n"
                + " Tu código es: {}  (vence en {} minutos)\n"
                + "=========================================================", email, codigo, minutosVigencia);
    }
}
