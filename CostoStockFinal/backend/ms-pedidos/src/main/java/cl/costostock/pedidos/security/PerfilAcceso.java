package cl.costostock.pedidos.security;

import java.util.EnumSet;
import java.util.Set;

/**
 * Perfil de acceso del usuario autenticado.
 *
 * POO: es una clase abstracta y cada rol es una subclase que redefine {@link #permisos()} y
 * {@link #descripcion()} (polimorfismo). {@link PerfilAdmin} hereda de {@link PerfilSoporte}
 * y amplía sus permisos llamando a {@code super.permisos()} (herencia multinivel).
 * Las reglas comunes ({@link #puede}, {@link #exigir}, {@link #exigirDuenoO}) viven aquí una sola vez.
 */
public abstract class PerfilAcceso {

    private final long usuarioId;

    protected PerfilAcceso(long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public long usuarioId() {
        return usuarioId;
    }

    public abstract Rol rol();

    public abstract Set<Permiso> permisos();

    public abstract String descripcion();

    public boolean puede(Permiso permiso) {
        return permisos().contains(permiso);
    }

    /** Lanza 403 si el rol no tiene el permiso. */
    public void exigir(Permiso permiso) {
        if (!puede(permiso)) {
            throw new AccesoDenegadoException(descripcion() + " no tiene permiso para esta acción");
        }
    }

    /** Es el dueño del recurso (mismo rol y mismo id). Los ids de clientes y proveedores se repiten. */
    public boolean esDueno(Rol rolDueno, Long duenoId) {
        return rol() == rolDueno && duenoId != null && duenoId == usuarioId;
    }

    /** Permite la acción al dueño del recurso o a quien tenga el permiso indicado; si no, 403. */
    public void exigirDuenoO(Rol rolDueno, Long duenoId, Permiso permiso) {
        if (!esDueno(rolDueno, duenoId) && !puede(permiso)) {
            throw new AccesoDenegadoException(descripcion() + " solo puede operar sobre sus propios datos");
        }
    }

    /** Fábrica: crea la subclase que corresponde al rol. */
    public static PerfilAcceso de(Rol rol, long usuarioId) {
        return switch (rol) {
            case CLIENTE -> new PerfilCliente(usuarioId);
            case PROVEEDOR -> new PerfilProveedor(usuarioId);
            case SOPORTE -> new PerfilSoporte(usuarioId);
            case ADMIN -> new PerfilAdmin(usuarioId);
            case SISTEMA -> new PerfilSistema();
        };
    }

    // ------------------------------- Subclases -------------------------------

    /** Comprador (app CostoStock). */
    public static class PerfilCliente extends PerfilAcceso {
        public PerfilCliente(long id) { super(id); }
        @Override public Rol rol() { return Rol.CLIENTE; }
        @Override public Set<Permiso> permisos() { return EnumSet.of(Permiso.COMPRAR); }
        @Override public String descripcion() { return "Un cliente"; }
    }

    /** Vendedor (app CostoStock Proveedores). */
    public static class PerfilProveedor extends PerfilAcceso {
        public PerfilProveedor(long id) { super(id); }
        @Override public Rol rol() { return Rol.PROVEEDOR; }
        @Override public Set<Permiso> permisos() { return EnumSet.of(Permiso.PUBLICAR); }
        @Override public String descripcion() { return "Un proveedor"; }
    }

    /** Soporte (panel web): ve todo y modera, pero no elimina ni gestiona el equipo. */
    public static class PerfilSoporte extends PerfilAcceso {
        public PerfilSoporte(long id) { super(id); }
        @Override public Rol rol() { return Rol.SOPORTE; }
        @Override public Set<Permiso> permisos() {
            return EnumSet.of(Permiso.VER_TODO, Permiso.MODERAR_PUBLICACIONES, Permiso.CANCELAR_PEDIDOS);
        }
        @Override public String descripcion() { return "Soporte"; }
    }

    /** Administrador (panel web): todo lo de Soporte más eliminar registros y gestionar el equipo. */
    public static class PerfilAdmin extends PerfilSoporte {
        public PerfilAdmin(long id) { super(id); }
        @Override public Rol rol() { return Rol.ADMIN; }
        @Override public Set<Permiso> permisos() {
            Set<Permiso> permisos = EnumSet.copyOf(super.permisos());
            permisos.add(Permiso.ELIMINAR_REGISTROS);
            permisos.add(Permiso.GESTIONAR_EQUIPO);
            return permisos;
        }
        @Override public String descripcion() { return "Un administrador"; }
    }

    /** Llamadas internas entre microservicios. */
    public static class PerfilSistema extends PerfilAcceso {
        public PerfilSistema() { super(0); }
        @Override public Rol rol() { return Rol.SISTEMA; }
        @Override public Set<Permiso> permisos() { return EnumSet.of(Permiso.GESTIONAR_STOCK); }
        @Override public String descripcion() { return "El sistema"; }
    }
}
