package cl.costostock.usuarios.repository;

import cl.costostock.usuarios.model.Proveedor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Optional<Proveedor> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRut(String rut);
}
