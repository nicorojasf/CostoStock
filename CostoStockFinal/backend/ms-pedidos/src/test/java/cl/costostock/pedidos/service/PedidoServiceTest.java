package cl.costostock.pedidos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.costostock.pedidos.client.ProductosClient;
import cl.costostock.pedidos.client.ProductosClient.ProductoDto;
import cl.costostock.pedidos.client.ProductosClient.StockItem;
import cl.costostock.pedidos.dto.Dtos.ItemRequest;
import cl.costostock.pedidos.dto.Dtos.PedidoRequest;
import cl.costostock.pedidos.dto.Dtos.PedidoResponse;
import cl.costostock.pedidos.exception.ApiExceptions.ConflictException;
import cl.costostock.pedidos.exception.ApiExceptions.NotFoundException;
import cl.costostock.pedidos.exception.ApiExceptions.StockInsuficienteException;
import cl.costostock.pedidos.model.Pedido;
import cl.costostock.pedidos.model.Pedido.Estado;
import cl.costostock.pedidos.model.PedidoItem;
import cl.costostock.pedidos.repository.PedidoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PedidoServiceTest {

    private PedidoRepository repository;
    private ProductosClient client;
    private PedidoService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(PedidoRepository.class);
        client = Mockito.mock(ProductosClient.class);
        service = new PedidoService(repository, client);
        when(repository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });
    }

    @Test
    void crearCalculaTotalesConPreciosDeMsProductos() {
        when(client.descontarStock(anyList())).thenReturn(List.of(
                new ProductoDto(1L, "Lámpara", "💡", 1000, 4),
                new ProductoDto(2L, "Vasos", "🥛", 500, 9)));

        PedidoResponse r = service.crear(new PedidoRequest(7L, List.of(
                new ItemRequest(1L, 1), new ItemRequest(2L, 1), new ItemRequest(1L, 1))));

        assertEquals(2500, r.total());
        assertEquals(2101, r.neto());
        assertEquals(399, r.iva());
        assertEquals(3, r.cantidadTotal());
        assertEquals(Estado.CONFIRMADO, r.estado());
        verify(client).descontarStock(List.of(new StockItem(1L, 2), new StockItem(2L, 1)));
    }

    @Test
    void crearSinStockNoGuardaElPedido() {
        when(client.descontarStock(anyList())).thenThrow(new StockInsuficienteException(List.of("Lámpara")));

        assertThrows(StockInsuficienteException.class,
                () -> service.crear(new PedidoRequest(7L, List.of(new ItemRequest(1L, 9)))));
        verify(repository, never()).save(any());
    }

    @Test
    void siFallaElGuardadoSeDevuelveElStock() {
        when(client.descontarStock(anyList())).thenReturn(List.of(new ProductoDto(1L, "Lámpara", "💡", 1000, 4)));
        Mockito.doThrow(new RuntimeException("BD caída")).when(repository).save(any(Pedido.class));

        assertThrows(RuntimeException.class,
                () -> service.crear(new PedidoRequest(7L, List.of(new ItemRequest(1L, 1)))));
        verify(client).reponerStock(List.of(new StockItem(1L, 1)));
    }

    private Pedido pedido(Estado estado) {
        Pedido p = new Pedido();
        p.setId(3L);
        p.setClienteId(7L);
        p.setEstado(estado);
        p.agregarItem(new PedidoItem(1L, "Lámpara", "💡", 1000, 2));
        p.setTotal(2000);
        return p;
    }

    @Test
    void cancelarDevuelveStockYCambiaEstado() {
        Pedido p = pedido(Estado.CONFIRMADO);
        when(repository.findById(3L)).thenReturn(Optional.of(p));

        assertEquals(Estado.CANCELADO, service.cancelar(3L).estado());
        verify(client).reponerStock(List.of(new StockItem(1L, 2)));
        assertThrows(ConflictException.class, () -> service.cancelar(3L));
    }

    @Test
    void soloSeEliminanPedidosCancelados() {
        Pedido confirmado = pedido(Estado.CONFIRMADO);
        when(repository.findById(3L)).thenReturn(Optional.of(confirmado));
        assertThrows(ConflictException.class, () -> service.eliminar(3L));

        confirmado.setEstado(Estado.CANCELADO);
        service.eliminar(3L);
        verify(repository).delete(confirmado);
    }

    @Test
    void listarYObtener() {
        when(repository.findByClienteIdOrderByCreadoEnDescIdDesc(7L)).thenReturn(List.of(pedido(Estado.CONFIRMADO)));
        when(repository.findAllByOrderByCreadoEnDescIdDesc()).thenReturn(List.of());
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertEquals(1, service.listar(7L).size());
        assertEquals(0, service.listar(null).size());
        assertThrows(NotFoundException.class, () -> service.obtener(99L));
    }

    @Test
    void netoSiempreSumaElTotalConIva() {
        assertEquals(1084, PedidoService.neto(1290));
        assertEquals(0, PedidoService.neto(0));
    }
}
