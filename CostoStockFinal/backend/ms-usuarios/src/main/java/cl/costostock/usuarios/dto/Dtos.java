package cl.costostock.usuarios.dto;

import cl.costostock.usuarios.model.TipoCuenta;
import cl.costostock.usuarios.security.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    // ----------------------------- Sesión -----------------------------

    /**
     * Respuesta de login y registro: datos del usuario, su rol y el token firmado que la app envía
     * en {@code Authorization: Bearer ...}. rut y telefono solo vienen para proveedores.
     */
    public record SesionResponse(Long id, String nombre, String email, String rut, String telefono,
                                 long creadoEn, Rol rol, String token) {
    }

    // ----------------------------- Equipo (Soporte / Admin) -----------------------------

    public record EquipoCrearRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres")
            String nombre,
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            String email,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
            String password,
            @NotNull(message = "El rol es obligatorio")
            Rol rol) {
    }

    public record EquipoResponse(Long id, String nombre, String email, Rol rol, long creadoEn) {
    }

    // ----------------------------- Recuperación de contraseña -----------------------------

    public record SolicitarCodigoRequest(
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            String email,
            @NotNull(message = "El tipo de cuenta es obligatorio")
            TipoCuenta tipo) {
    }

    public record RestablecerRequest(
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            String email,
            @NotNull(message = "El tipo de cuenta es obligatorio")
            TipoCuenta tipo,
            @NotBlank(message = "El código es obligatorio")
            @Pattern(regexp = "^\\d{6}$", message = "El código tiene 6 dígitos")
            String codigo,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
            String nuevaPassword) {
    }

    public record MensajeResponse(String mensaje) {
    }
}
