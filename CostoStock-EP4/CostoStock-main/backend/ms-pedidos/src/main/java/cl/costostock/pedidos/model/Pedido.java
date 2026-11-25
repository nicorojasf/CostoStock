package cl.costostock.pedidos.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** Compra confirmada por un cliente. */
@Entity
@Table(name = "pedidos")
public class Pedido {

    public enum Estado { CONFIRMADO, CANCELADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "creado_en", nullable = false)
    private long creadoEn;

    /** Valor neto (sin IVA). */
    @Column(nullable = false)
    private int neto;

    @Column(nullable = false)
    private int iva;

    /** Total pagado (IVA incluido). */
    @Column(nullable = false)
    private int total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Estado estado = Estado.CONFIRMADO;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PedidoItem> items = new ArrayList<>();

    public void agregarItem(PedidoItem item) {
        item.setPedido(this);
        items.add(item);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public long getCreadoEn() { return creadoEn; }
    public void setCreadoEn(long creadoEn) { this.creadoEn = creadoEn; }
    public int getNeto() { return neto; }
    public void setNeto(int neto) { this.neto = neto; }
    public int getIva() { return iva; }
    public void setIva(int iva) { this.iva = iva; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public List<PedidoItem> getItems() { return items; }
}
