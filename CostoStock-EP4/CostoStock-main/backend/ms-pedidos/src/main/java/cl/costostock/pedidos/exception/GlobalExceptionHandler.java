package cl.costostock.pedidos.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(int status, String error, String mensaje, Map<String, String> campos,
                                List<String> detalles, long timestamp) {
    }

    @ExceptionHandler(ApiExceptions.NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ApiExceptions.NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of(), List.of());
    }

    @ExceptionHandler(ApiExceptions.BadRequestException.class)
    public ResponseEntity<ErrorResponse> badRequest(ApiExceptions.BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of(), List.of());
    }

    @ExceptionHandler(ApiExceptions.ConflictException.class)
    public ResponseEntity<ErrorResponse> conflict(ApiExceptions.ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of(), List.of());
    }

    @ExceptionHandler(ApiExceptions.StockInsuficienteException.class)
    public ResponseEntity<ErrorResponse> stock(ApiExceptions.StockInsuficienteException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of(), ex.getProductos());
    }

    @ExceptionHandler(ApiExceptions.ServicioNoDisponibleException.class)
    public ResponseEntity<ErrorResponse> unavailable(ApiExceptions.ServicioNoDisponibleException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), Map.of(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud", campos, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido", Map.of(), List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String mensaje, Map<String, String> campos,
                                                List<String> detalles) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), status.getReasonPhrase(),
                mensaje, campos, detalles, System.currentTimeMillis()));
    }
}
