package cl.costostock.usuarios.service;

/**
 * Envío del código de recuperación. Es una interfaz para poder cambiar la implementación
 * (polimorfismo): hoy {@link LogNotificadorCorreo} lo escribe en la consola del microservicio;
 * en producción se reemplazaría por una que use SMTP o un servicio de correo.
 */
public interface NotificadorCorreo {
    void enviarCodigo(String email, String codigo, int minutosVigencia);
}
