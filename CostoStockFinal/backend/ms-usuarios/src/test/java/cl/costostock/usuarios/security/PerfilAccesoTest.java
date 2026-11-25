package cl.costostock.usuarios.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PerfilAccesoTest {

    private final TokenService tokens = new TokenService("secreto-de-prueba", 1);

    @Test
    void cadaRolTienePermisosDistintos() {
        PerfilAcceso cliente = PerfilAcceso.de(Rol.CLIENTE, 1);
        PerfilAcceso proveedor = PerfilAcceso.de(Rol.PROVEEDOR, 1);
        PerfilAcceso soporte = PerfilAcceso.de(Rol.SOPORTE, 1);
        PerfilAcceso admin = PerfilAcceso.de(Rol.ADMIN, 1);

        assertTrue(cliente.puede(Permiso.COMPRAR));
        assertFalse(cliente.puede(Permiso.PUBLICAR));
        assertTrue(proveedor.puede(Permiso.PUBLICAR));
        assertTrue(soporte.puede(Permiso.MODERAR_PUBLICACIONES));
        assertFalse(soporte.puede(Permiso.ELIMINAR_REGISTROS));
        // El admin hereda todo lo de soporte y suma permisos.
        assertInstanceOf(PerfilAcceso.PerfilSoporte.class, admin);
        assertTrue(admin.permisos().containsAll(soporte.permisos()));
        assertTrue(admin.puede(Permiso.ELIMINAR_REGISTROS));
        assertTrue(PerfilAcceso.de(Rol.SISTEMA, 0).puede(Permiso.GESTIONAR_STOCK));
    }

    @Test
    void duenoORolConPermiso() {
        PerfilAcceso cliente = PerfilAcceso.de(Rol.CLIENTE, 5);
        cliente.exigirDuenoO(Rol.CLIENTE, 5L, Permiso.VER_TODO);
        // Mismo id pero distinto rol no es dueño.
        assertThrows(AccesoDenegadoException.class, () -> cliente.exigirDuenoO(Rol.PROVEEDOR, 5L, Permiso.VER_TODO));
        assertThrows(AccesoDenegadoException.class, () -> cliente.exigirDuenoO(Rol.CLIENTE, 6L, Permiso.VER_TODO));
        PerfilAcceso.de(Rol.SOPORTE, 9).exigirDuenoO(Rol.CLIENTE, 6L, Permiso.VER_TODO);
        assertThrows(AccesoDenegadoException.class, () -> cliente.exigir(Permiso.PUBLICAR));
    }

    @Test
    void tokenFirmadoSeVerificaYDetectaAlteraciones() {
        String token = tokens.emitir(7, Rol.PROVEEDOR);
        PerfilAcceso perfil = tokens.desdeEncabezado("Bearer " + token);
        assertEquals(7, perfil.usuarioId());
        assertEquals(Rol.PROVEEDOR, perfil.rol());

        String alterado = token.substring(0, token.length() - 2) + "xx";
        assertThrows(NoAutenticadoException.class, () -> tokens.verificar(alterado));
        assertThrows(NoAutenticadoException.class, () -> new TokenService("otro-secreto", 1).verificar(token));
        assertThrows(NoAutenticadoException.class, () -> tokens.desdeEncabezado(null));
        assertThrows(NoAutenticadoException.class, () -> tokens.verificar("basura"));
        assertThrows(NoAutenticadoException.class, () -> new TokenService("secreto-de-prueba", -1)
                .verificar(new TokenService("secreto-de-prueba", -1).emitir(1, Rol.CLIENTE)));
    }
}
