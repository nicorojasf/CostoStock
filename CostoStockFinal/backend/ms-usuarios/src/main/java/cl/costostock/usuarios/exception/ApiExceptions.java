package cl.costostock.usuarios.exception;

/** Excepciones de negocio que el {@link GlobalExceptionHandler} traduce a códigos HTTP. */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 404 */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /** 409: el recurso ya existe (correo o RUT duplicado). */
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    /** 401: correo o contraseña incorrectos. */
    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Correo o contraseña incorrectos");
        }
    }

    /** 400: dato que pasa el formato pero no la regla de negocio (ej: dígito verificador del RUT). */
    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }
}
