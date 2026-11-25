package cl.costostock.usuarios.config;

import cl.costostock.usuarios.security.Rol;
import cl.costostock.usuarios.service.EquipoService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Cuentas iniciales del panel web (cámbienlas después de la defensa). */
@Component
@ConditionalOnProperty(name = "costostock.seed-equipo", havingValue = "true", matchIfMissing = true)
public class EquipoInicial implements CommandLineRunner {

    private final EquipoService equipo;

    public EquipoInicial(EquipoService equipo) {
        this.equipo = equipo;
    }

    @Override
    public void run(String... args) {
        equipo.crearSiNoExiste("Administrador CostoStock", "admin@costostock.cl", "Admin1234", Rol.ADMIN);
        equipo.crearSiNoExiste("Soporte CostoStock", "soporte@costostock.cl", "Soporte1234", Rol.SOPORTE);
    }
}
