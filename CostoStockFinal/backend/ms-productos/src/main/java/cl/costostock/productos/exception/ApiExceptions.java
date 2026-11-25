package cl.costostock.productos.exception;

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

    /** 409: uno o más productos no tienen stock suficiente (o están pausados). */
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
}
