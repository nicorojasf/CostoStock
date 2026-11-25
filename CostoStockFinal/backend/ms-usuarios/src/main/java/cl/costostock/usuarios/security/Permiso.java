package cl.costostock.usuarios.security;

/** Acciones que se autorizan según el rol (ver {@link PerfilAcceso#permisos()}). */
public enum Permiso {
    /** Comprar y gestionar sus propios pedidos. */
    COMPRAR,
    /** Crear y administrar sus propias publicaciones. */
    PUBLICAR,
    /** Ver datos de todos los usuarios, publicaciones y pedidos. */
    VER_TODO,
    /** Pausar o activar cualquier publicación. */
    MODERAR_PUBLICACIONES,
    /** Cancelar cualquier pedido. */
    CANCELAR_PEDIDOS,
    /** Eliminar publicaciones, pedidos y cuentas de otros usuarios. */
    ELIMINAR_REGISTROS,
    /** Crear y eliminar cuentas de Soporte y Admin. */
    GESTIONAR_EQUIPO,
    /** Descontar y reponer stock (uso interno entre microservicios). */
    GESTIONAR_STOCK
}
