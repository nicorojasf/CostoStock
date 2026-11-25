package cl.costostock.pedidos.exception;

import java.util.List;

public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 404 */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /** 400 */
    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }

    /** 409: operación no permitida en el estado actual del pedido. */
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    /** 409: ms-productos informó que no hay stock suficiente. */
    public static class StockInsuficienteException extends RuntimeException {
        private final List<String> productos;

        public StockInsuficienteException(List<String> productos) {
            super("Stock insuficiente");
            this.productos = productos;
        }

        public List<String> getProductos() {
            return productos;
        }
    }

    /** 503: ms-productos no responde. */
    public static class ServicioNoDisponibleException extends RuntimeException {
        public ServicioNoDisponibleException() {
            super("El servicio de productos no está disponible. Inténtalo más tarde");
        }
    }
}
