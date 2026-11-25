package cl.costostock.usuarios.security;

/** 401: falta el token o no es válido / venció. */
public class NoAutenticadoException extends RuntimeException {
    public NoAutenticadoException(String message) {
        super(message);
    }
}
