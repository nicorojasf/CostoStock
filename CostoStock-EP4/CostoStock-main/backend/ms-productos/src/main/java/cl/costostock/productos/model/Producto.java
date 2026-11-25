package cl.costostock.productos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Publicación de un proveedor. Las columnas coinciden con el contrato de datos de las apps
 * (ver CostoStockProveedor/docs/contrato-datos.md).
 */
@Entity
@Table(name = "productos")
public class Producto {

    public enum Estado { ACTIVE, PAUSED }

    public enum Condicion { NEW, USED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Proveedor dueño de la publicación (null para el catálogo inicial de CostoStock). */
    @Column(name = "proveedor_id")
    private Long proveedorId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String categoria;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    /** Precio final en CLP (IVA incluido). */
    @Column(nullable = false)
    private int precio;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false, length = 40)
    private String comuna;

    @Column(nullable = false, length = 16)
    private String emoji;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Condicion condicion = Condicion.NEW;

    /** Rutas o URLs de las fotos separadas por "|" (la primera es la portada). */
    @Column(nullable = false, length = 2000)
    private String imagenes = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Estado estado = Estado.ACTIVE;

    @Column(name = "creado_en", nullable = false)
    private long creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private long actualizadoEn;

    public Producto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProveedorId() { return proveedorId; }
    public void setProveedorId(Long proveedorId) { this.proveedorId = proveedorId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public int getPrecio() { return precio; }
    public void setPrecio(int precio) { this.precio = precio; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getComuna() { return comuna; }
    public void setComuna(String comuna) { this.comuna = comuna; }
    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public Condicion getCondicion() { return condicion; }
    public void setCondicion(Condicion condicion) { this.condicion = condicion; }
    public String getImagenes() { return imagenes; }
    public void setImagenes(String imagenes) { this.imagenes = imagenes; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public long getCreadoEn() { return creadoEn; }
    public void setCreadoEn(long creadoEn) { this.creadoEn = creadoEn; }
    public long getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(long actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
