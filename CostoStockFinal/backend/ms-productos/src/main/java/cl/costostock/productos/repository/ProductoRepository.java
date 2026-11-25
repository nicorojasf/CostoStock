package cl.costostock.productos.repository;

import cl.costostock.productos.model.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findAllByOrderByIdAsc();
    List<Producto> findByProveedorIdOrderByCreadoEnDescIdDesc(Long proveedorId);
}
