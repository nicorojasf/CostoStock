package cl.costostock.usuarios.repository;

import cl.costostock.usuarios.model.CodigoRecuperacion;
import cl.costostock.usuarios.model.TipoCuenta;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {
    Optional<CodigoRecuperacion> findFirstByEmailAndTipoAndUsadoFalseOrderByIdDesc(String email, TipoCuenta tipo);
    void deleteByEmailAndTipo(String email, TipoCuenta tipo);
}
