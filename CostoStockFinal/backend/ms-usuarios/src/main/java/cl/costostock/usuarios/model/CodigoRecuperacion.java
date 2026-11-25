package cl.costostock.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Código de 6 dígitos para restablecer la contraseña. Se guarda cifrado (BCrypt), no en claro. */
@Entity
@Table(name = "codigos_recuperacion")
public class CodigoRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoCuenta tipo;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "expira_en", nullable = false)
    private long expiraEn;

    @Column(nullable = false)
    private int intentos;

    @Column(nullable = false)
    private boolean usado;

    public CodigoRecuperacion() {
    }

    public CodigoRecuperacion(String email, TipoCuenta tipo, String codigoHash, long expiraEn) {
        this.email = email;
        this.tipo = tipo;
        this.codigoHash = codigoHash;
        this.expiraEn = expiraEn;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public TipoCuenta getTipo() { return tipo; }
    public String getCodigoHash() { return codigoHash; }
    public long getExpiraEn() { return expiraEn; }
    public void setExpiraEn(long expiraEn) { this.expiraEn = expiraEn; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int intentos) { this.intentos = intentos; }
    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }
}
