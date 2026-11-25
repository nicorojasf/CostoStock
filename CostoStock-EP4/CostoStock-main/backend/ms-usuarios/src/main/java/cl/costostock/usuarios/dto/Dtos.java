package cl.costostock.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTOs de entrada y salida del microservicio (records inmutables). */
public final class Dtos {

    private Dtos() {
    }

    // ----------------------------- Clientes -----------------------------

    public record ClienteRegistroRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres")
            String nombre,
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            String email,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
            String password) {
    }

    public record ClienteUpdateRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres")
            String nombre) {
    }

    public record ClienteResponse(Long id, String nombre, String email, long creadoEn) {
    }

    // ----------------------------- Proveedores -----------------------------

    public record ProveedorRegistroRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres")
            String nombre,
            @NotBlank(message = "El RUT es obligatorio")
            @Pattern(regexp = "^\\d{7,8}-[\\dkK]$", message = "El RUT debe tener el formato 12345678-5")
            String rut,
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            String email,
            @NotBlank(message = "El teléfono es obligatorio")
            @Pattern(regexp = "^\\+569\\d{8}$", message = "El teléfono debe tener el formato +569XXXXXXXX")
            String telefono,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
            String password) {
    }

    public record ProveedorUpdateRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres")
            String nombre,
            @NotBlank(message = "El teléfono es obligatorio")
            @Pattern(regexp = "^\\+569\\d{8}$", message = "El teléfono debe tener el formato +569XXXXXXXX")
            String telefono) {
    }

    public record ProveedorResponse(Long id, String nombre, String rut, String email, String telefono,
                                    long creadoEn) {
    }

    // ----------------------------- Común -----------------------------

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            String email,
            @NotBlank(message = "La contraseña es obligatoria")
            String password) {
    }
}
