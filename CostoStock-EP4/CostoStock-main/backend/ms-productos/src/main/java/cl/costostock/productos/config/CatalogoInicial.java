package cl.costostock.productos.config;

import cl.costostock.productos.model.Producto;
import cl.costostock.productos.repository.ProductoRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Carga el catálogo de ejemplo la primera vez que arranca el microservicio. */
@Component
@ConditionalOnProperty(name = "costostock.seed", havingValue = "true")
public class CatalogoInicial implements CommandLineRunner {

    private final ProductoRepository repository;

    public CatalogoInicial(ProductoRepository repository) {
        this.repository = repository;
    }

    private record Semilla(String nombre, String categoria, String descripcion, int precio, int stock,
                           String comuna, String emoji) {
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        List<Semilla> semillas = List.of(
                new Semilla("Sombrero de paja con cinta animal print", "Moda", "Sombrero de ala ancha, ideal para el verano", 19037, 8, "Providencia", "👒"),
                new Semilla("Chaqueta de mezclilla talla M", "Moda", "Corte clásico, excelente estado", 24990, 5, "Ñuñoa", "🧥"),
                new Semilla("Zapatillas urbanas blancas N°40", "Moda", "Suela antideslizante, poco uso", 29990, 3, "Santiago", "👟"),
                new Semilla("Botella de aluminio blanca 750 ml", "Hogar", "Ideal para sublimar, tapa con mosquetón", 4990, 60, "Maipú", "🍶"),
                new Semilla("Lámpara de escritorio LED", "Hogar", "3 niveles de luz, brazo flexible", 12990, 14, "La Florida", "💡"),
                new Semilla("Set de 6 vasos de vidrio", "Hogar", "Vidrio templado de 350 ml", 6990, 25, "Puente Alto", "🥛"),
                new Semilla("Conejo de peluche 40 cm", "Bebés y niños", "Suave y lavable, hipoalergénico", 8990, 22, "Las Condes", "🐰"),
                new Semilla("Silla de auto reclinable 0-36 kg", "Bebés y niños", "Giratoria 360°, con arnés de 5 puntos", 89990, 4, "Providencia", "💺"),
                new Semilla("Mordedor de silicona y madera", "Bebés y niños", "Libre de BPA, para dentición", 5490, 40, "Ñuñoa", "🧸"),
                new Semilla("Audífonos bluetooth", "Tecnología", "Batería de 20 horas, con micrófono", 19990, 15, "Santiago", "🎧"),
                new Semilla("Cargador rápido USB-C 20 W", "Tecnología", "Compatible con celulares y tablets", 9990, 30, "Quilicura", "🔌"),
                new Semilla("Mouse inalámbrico", "Tecnología", "Receptor USB, silencioso", 7990, 0, "Maipú", "🖱️"),
                new Semilla("Cojín para gato color chocolate", "Mascotas", "Relleno suave, funda removible", 14990, 12, "La Florida", "🐱"),
                new Semilla("Rascador de cartón", "Mascotas", "Incluye hierba gatera", 7490, 18, "Las Condes", "🐾"));

        long ahora = System.currentTimeMillis();
        semillas.forEach(s -> {
            Producto p = new Producto();
            p.setNombre(s.nombre());
            p.setCategoria(s.categoria());
            p.setDescripcion(s.descripcion());
            p.setPrecio(s.precio());
            p.setStock(s.stock());
            p.setComuna(s.comuna());
            p.setEmoji(s.emoji());
            p.setCreadoEn(ahora);
            p.setActualizadoEn(ahora);
            repository.save(p);
        });
    }
}
