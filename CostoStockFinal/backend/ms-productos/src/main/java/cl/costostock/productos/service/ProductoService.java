package cl.costostock.productos.service;

import cl.costostock.productos.dto.Dtos.ProductoRequest;
import cl.costostock.productos.dto.Dtos.ProductoResponse;
import cl.costostock.productos.dto.Dtos.StockItem;
import cl.costostock.productos.exception.ApiExceptions.NotFoundException;
import cl.costostock.productos.exception.ApiExceptions.StockInsuficienteException;
import cl.costostock.productos.model.Producto;
import cl.costostock.productos.model.Producto.Condicion;
import cl.costostock.productos.model.Producto.Estado;
import cl.costostock.productos.repository.ProductoRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    static final String SEPARADOR = "|";

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    /**
     * Lista publicaciones. Sin filtros devuelve todo; con {@code soloDisponibles} devuelve solo las
     * activas y con stock (lo que ve el comprador).
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(Long proveedorId, String categoria, String comuna, boolean soloDisponibles) {
        List<Producto> base = proveedorId != null
                ? repository.findByProveedorIdOrderByCreadoEnDescIdDesc(proveedorId)
                : repository.findAllByOrderByIdAsc();
        return base.stream()
                .filter(p -> categoria == null || categoria.isBlank() || p.getCategoria().equalsIgnoreCase(categoria))
                .filter(p -> comuna == null || comuna.isBlank() || p.getComuna().equalsIgnoreCase(comuna))
                .filter(p -> !soloDisponibles || (p.getEstado() == Estado.ACTIVE && p.getStock() > 0))
                .map(ProductoService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto p = new Producto();
        long ahora = System.currentTimeMillis();
        p.setProveedorId(request.proveedorId());
        p.setCreadoEn(ahora);
        aplicar(p, request, ahora);
        p.setEstado(Estado.ACTIVE);
        return toResponse(repository.save(p));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto p = buscar(id);
        aplicar(p, request, System.currentTimeMillis());
        return toResponse(repository.save(p));
    }

    @Transactional
    public ProductoResponse cambiarEstado(Long id, Estado estado) {
        Producto p = buscar(id);
        p.setEstado(estado);
        p.setActualizadoEn(System.currentTimeMillis());
        return toResponse(repository.save(p));
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscar(id));
    }

    /**
     * Descuenta el stock de varios productos en una sola transacción (lo usa ms-pedidos al confirmar
     * una compra). Si alguno no alcanza o está pausado no se modifica nada y se informa cuáles fallaron.
     */
    @Transactional
    public List<ProductoResponse> descontarStock(List<StockItem> items) {
        Map<Long, Integer> cantidades = agrupar(items);
        List<Producto> productos = new ArrayList<>();
        List<String> sinStock = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            Producto p = buscar(e.getKey());
            if (p.getEstado() != Estado.ACTIVE || p.getStock() < e.getValue()) {
                sinStock.add(p.getNombre());
            }
            productos.add(p);
        }
        if (!sinStock.isEmpty()) {
            throw new StockInsuficienteException(sinStock);
        }
        long ahora = System.currentTimeMillis();
        for (Producto p : productos) {
            p.setStock(p.getStock() - cantidades.get(p.getId()));
            p.setActualizadoEn(ahora);
        }
        return repository.saveAll(productos).stream().map(ProductoService::toResponse).toList();
    }

    /** Devuelve stock (al cancelar un pedido). Los productos que ya no existen se ignoran. */
    @Transactional
    public void reponerStock(List<StockItem> items) {
        long ahora = System.currentTimeMillis();
        agrupar(items).forEach((id, cantidad) -> repository.findById(id).ifPresent(p -> {
            p.setStock(p.getStock() + cantidad);
            p.setActualizadoEn(ahora);
            repository.save(p);
        }));
    }

    private Producto buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el producto " + id));
    }

    private static Map<Long, Integer> agrupar(List<StockItem> items) {
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        items.forEach(i -> cantidades.merge(i.productoId(), i.cantidad(), Integer::sum));
        return cantidades;
    }

    private static void aplicar(Producto p, ProductoRequest r, long ahora) {
        p.setNombre(r.nombre().trim());
        p.setCategoria(r.categoria().trim());
        p.setDescripcion(r.descripcion().trim());
        p.setPrecio(r.precio());
        p.setStock(r.stock());
        p.setComuna(r.comuna().trim());
        p.setEmoji(r.emoji() == null || r.emoji().isBlank() ? emojiPorCategoria(r.categoria()) : r.emoji());
        p.setCondicion(r.condicion() == null ? Condicion.NEW : r.condicion());
        p.setImagenes(r.imagenes() == null ? "" : String.join(SEPARADOR, r.imagenes()));
        p.setActualizadoEn(ahora);
    }

    static String emojiPorCategoria(String categoria) {
        return switch (categoria == null ? "" : categoria) {
            case "Moda" -> "👗";
            case "Hogar" -> "🏠";
            case "Bebés y niños" -> "🍼";
            case "Tecnología" -> "💻";
            case "Mascotas" -> "🐾";
            case "Deportes" -> "⚽";
            case "Libros y hobbies" -> "📚";
            default -> "🛒";
        };
    }

    static ProductoResponse toResponse(Producto p) {
        List<String> imagenes = p.getImagenes() == null || p.getImagenes().isEmpty()
                ? List.of()
                : Arrays.asList(p.getImagenes().split("\\|"));
        return new ProductoResponse(p.getId(), p.getProveedorId(), p.getNombre(), p.getCategoria(),
                p.getDescripcion(), p.getPrecio(), p.getStock(), p.getComuna(), p.getEmoji(), p.getCondicion(),
                imagenes, p.getEstado(), p.getCreadoEn(), p.getActualizadoEn());
    }
}
