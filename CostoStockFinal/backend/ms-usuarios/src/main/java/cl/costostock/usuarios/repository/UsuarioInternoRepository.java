package cl.costostock.usuarios.repository;

import cl.costostock.usuarios.model.UsuarioInterno;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioInternoRepository extends JpaRepository<UsuarioInterno, Long> {
    Optional<UsuarioInterno> findByEmail(String email);
    boolean existsByEmail(String email);
}
