package cl.costostock.pedidos.repository;

import cl.costostock.pedidos.model.Pedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteIdOrderByCreadoEnDescIdDesc(Long clienteId);
    List<Pedido> findAllByOrderByCreadoEnDescIdDesc();
}
