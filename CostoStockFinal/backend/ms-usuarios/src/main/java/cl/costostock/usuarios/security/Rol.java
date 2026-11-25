package cl.costostock.usuarios.security;

/** Roles del sistema. SISTEMA es interno: lo usa ms-pedidos para hablar con ms-productos. */
public enum Rol {
    CLIENTE, PROVEEDOR, SOPORTE, ADMIN, SISTEMA
}
