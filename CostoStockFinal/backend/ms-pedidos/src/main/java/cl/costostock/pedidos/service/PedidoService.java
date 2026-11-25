package cl.costostock.pedidos.service;

import cl.costostock.pedidos.client.ProductosClient;
import cl.costostock.pedidos.client.ProductosClient.ProductoDto;
import cl.costostock.pedidos.client.ProductosClient.StockItem;
import cl.costostock.pedidos.dto.Dtos.ItemResponse;
import cl.costostock.pedidos.dto.Dtos.PedidoRequest;
import cl.costostock.pedidos.dto.Dtos.PedidoResponse;
import cl.costostock.pedidos.exception.ApiExceptions.ConflictException;
import cl.costostock.pedidos.exception.ApiExceptions.NotFoundException;
import cl.costostock.pedidos.model.Pedido;
import cl.costostock.pedidos.model.Pedido.Estado;
import cl.costostock.pedidos.model.PedidoItem;
import cl.costostock.pedidos.repository.PedidoRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    static final int IVA_PERCENT = 19;

    private final PedidoRepository repository;
    private final ProductosClient productosClient;

    public PedidoService(PedidoRepository repository, ProductosClient productosClient) {
        this.repository = repository;
        this.productosClient = productosClient;
    }

    /**
     * Confirma una compra: pide a ms-productos que descuente el stock (y obtiene los precios vigentes),
     * calcula Neto / IVA / Total y guarda el pedido. Si el guardado falla, devuelve el stock.
     */
    @Transactional
    public PedidoResponse crear(PedidoRequest request) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        request.items().forEach(i -> cantidades.merge(i.productoId(), i.cantidad(), Integer::sum));
        List<StockItem> stockItems = cantidades.entrySet().stream()
                .map(e -> new StockItem(e.getKey(), e.getValue()))
                .toList();

        List<ProductoDto> productos = productosClient.descontarStock(stockItems);
        try {
            Map<Long, ProductoDto> porId = productos.stream()
                    .collect(Collectors.toMap(ProductoDto::id, Function.identity(), (a, b) -> a));
            Pedido pedido = new Pedido();
            pedido.setClienteId(request.clienteId());
            pedido.setCreadoEn(System.currentTimeMillis());
            cantidades.forEach((id, cantidad) -> {
                ProductoDto p = porId.get(id);
                if (p == null) {
                    throw new IllegalStateException("ms-productos no devolvió el producto " + id);
                }
                pedido.agregarItem(new PedidoItem(id, p.nombre(), p.emoji() == null ? "🛒" : p.emoji(),
                        p.precio(), cantidad));
            });
            int total = pedido.getItems().stream().mapToInt(PedidoItem::getSubtotal).sum();
            int neto = neto(total);
            pedido.setTotal(total);
            pedido.setNeto(neto);
            pedido.setIva(total - neto);
            return toResponse(repository.save(pedido));
        } catch (RuntimeException e) {
            // Compensación: si no se pudo guardar el pedido, el stock vuelve a su valor.
            productosClient.reponerStock(stockItems);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(Long clienteId) {
        List<Pedido> pedidos = clienteId == null
                ? repository.findAllByOrderByCreadoEnDescIdDesc()
                : repository.findByClienteIdOrderByCreadoEnDescIdDesc(clienteId);
        return pedidos.stream().map(PedidoService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    /** Anula una compra y devuelve el stock a ms-productos. */
    @Transactional
    public PedidoResponse cancelar(Long id) {
        Pedido pedido = buscar(id);
        if (pedido.getEstado() == Estado.CANCELADO) {
            throw new ConflictException("El pedido ya está cancelado");
        }
        productosClient.reponerStock(pedido.getItems().stream()
                .map(i -> new StockItem(i.getProductoId(), i.getCantidad()))
                .toList());
        pedido.setEstado(Estado.CANCELADO);
        return toResponse(repository.save(pedido));
    }

    /** Solo se pueden eliminar del historial los pedidos cancelados. */
    @Transactional
    public void eliminar(Long id) {
        Pedido pedido = buscar(id);
        if (pedido.getEstado() != Estado.CANCELADO) {
            throw new ConflictException("Solo se pueden eliminar pedidos cancelados");
        }
        repository.delete(pedido);
    }

    private Pedido buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("No existe el pedido " + id));
    }

    /** Neto = total / 1,19 redondeado al peso más cercano (misma regla que las apps). */
    static int neto(int total) {
        return (int) ((total * 100L + 59) / (100 + IVA_PERCENT));
    }

    static PedidoResponse toResponse(Pedido p) {
        List<ItemResponse> items = p.getItems().stream()
                .map(i -> new ItemResponse(i.getProductoId(), i.getNombreProducto(), i.getEmoji(),
                        i.getPrecioUnitario(), i.getCantidad(), i.getSubtotal()))
                .toList();
        int cantidad = items.stream().mapToInt(ItemResponse::cantidad).sum();
        return new PedidoResponse(p.getId(), p.getClienteId(), p.getCreadoEn(), p.getNeto(), p.getIva(),
                p.getTotal(), p.getEstado(), cantidad, items);
    }
}
