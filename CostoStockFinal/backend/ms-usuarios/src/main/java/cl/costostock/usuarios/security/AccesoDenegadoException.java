package cl.costostock.usuarios.security;

/** 403: el usuario está autenticado pero su rol no permite la acción. */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String message) {
        super(message);
    }
}
