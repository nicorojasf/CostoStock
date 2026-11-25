package cl.costostock.productos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.costostock.productos.dto.Dtos.ProductoRequest;
import cl.costostock.productos.dto.Dtos.ProductoResponse;
import cl.costostock.productos.dto.Dtos.StockItem;
import cl.costostock.productos.exception.ApiExceptions.NotFoundException;
import cl.costostock.productos.exception.ApiExceptions.StockInsuficienteException;
import cl.costostock.productos.model.Producto;
import cl.costostock.productos.model.Producto.Condicion;
import cl.costostock.productos.model.Producto.Estado;
import cl.costostock.productos.repository.ProductoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProductoServiceTest {

    private ProductoRepository repository;
    private ProductoService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ProductoRepository.class);
        service = new ProductoService(repository);
        when(repository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            if (p.getId() == null) p.setId(10L);
            return p;
        });
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Producto producto(long id, int stock, Estado estado) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre("Producto " + id);
        p.setCategoria("Hogar");
        p.setDescripcion("desc");
        p.setPrecio(1000);
        p.setStock(stock);
        p.setComuna("Santiago");
        p.setEmoji("🏠");
        p.setEstado(estado);
        return p;
    }

    private ProductoRequest request(String emoji) {
        return new ProductoRequest(3L, " Lámpara ", "Hogar", "Luz LED", 12990, 5, "Maipú", emoji,
                Condicion.USED, List.of("/a.jpg", "/b.jpg"));
    }

    @Test
    void crearAsignaEmojiPorCategoriaYGuardaFotos() {
        ProductoResponse r = service.crear(request(null));

        assertEquals(10L, r.id());
        assertEquals("Lámpara", r.nombre());
        assertEquals("🏠", r.emoji());
        assertEquals(List.of("/a.jpg", "/b.jpg"), r.imagenes());
        assertEquals(Estado.ACTIVE, r.estado());
        assertEquals(Condicion.USED, r.condicion());
    }

    @Test
    void listarSoloDisponiblesFiltraPausadosYSinStock() {
        when(repository.findAllByOrderByIdAsc()).thenReturn(List.of(
                producto(1, 5, Estado.ACTIVE), producto(2, 0, Estado.ACTIVE), producto(3, 5, Estado.PAUSED)));

        assertEquals(1, service.listar(null, null, null, true).size());
        assertEquals(3, service.listar(null, "hogar", "SANTIAGO", false).size());
        assertEquals(0, service.listar(null, "Moda", null, false).size());
    }

    @Test
    void listarPorProveedorUsaSuConsulta() {
        when(repository.findByProveedorIdOrderByCreadoEnDescIdDesc(3L)).thenReturn(List.of(producto(1, 5, Estado.ACTIVE)));

        assertEquals(1, service.listar(3L, null, null, false).size());
    }

    @Test
    void actualizarCambiarEstadoYEliminar() {
        Producto p = producto(1, 5, Estado.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(p));

        assertEquals("Lámpara", service.actualizar(1L, request("💡")).nombre());
        assertEquals("💡", p.getEmoji());
        assertEquals(Estado.PAUSED, service.cambiarEstado(1L, Estado.PAUSED).estado());
        service.eliminar(1L);
        verify(repository).delete(p);
    }

    @Test
    void obtenerInexistenteLanza404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.obtener(99L));
    }

    @Test
    void descontarStockAgrupaCantidadesYDescuenta() {
        Producto p = producto(1, 5, Estado.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(p));

        service.descontarStock(List.of(new StockItem(1L, 2), new StockItem(1L, 1)));

        assertEquals(2, p.getStock());
    }

    @Test
    void descontarStockInsuficienteNoModificaNada() {
        Producto conStock = producto(1, 5, Estado.ACTIVE);
        Producto sinStock = producto(2, 1, Estado.ACTIVE);
        Producto pausado = producto(3, 9, Estado.PAUSED);
        when(repository.findById(1L)).thenReturn(Optional.of(conStock));
        when(repository.findById(2L)).thenReturn(Optional.of(sinStock));
        when(repository.findById(3L)).thenReturn(Optional.of(pausado));

        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class, () ->
                service.descontarStock(List.of(new StockItem(1L, 1), new StockItem(2L, 2), new StockItem(3L, 1))));

        assertEquals(List.of("Producto 2", "Producto 3"), ex.getProductos());
        assertEquals(5, conStock.getStock());
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void reponerStockSumaEIgnoraInexistentes() {
        Producto p = producto(1, 5, Estado.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        when(repository.findById(2L)).thenReturn(Optional.empty());

        service.reponerStock(List.of(new StockItem(1L, 3), new StockItem(2L, 1)));

        assertEquals(8, p.getStock());
    }

    @Test
    void emojiPorDefecto() {
        assertEquals("🛒", ProductoService.emojiPorCategoria("Otra"));
        assertTrue(ProductoService.toResponse(producto(1, 1, Estado.ACTIVE)).imagenes().isEmpty());
    }
}
