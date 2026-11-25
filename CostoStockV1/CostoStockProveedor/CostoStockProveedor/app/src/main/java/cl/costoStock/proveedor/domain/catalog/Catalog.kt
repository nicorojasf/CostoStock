package cl.costoStock.proveedor.domain.catalog

/** Listas cerradas para el formulario. Las 5 primeras categorías coinciden con la app de cliente. */
object Catalog {
    val categories = listOf(
        "Moda", "Hogar", "Bebés y niños", "Tecnología", "Mascotas",
        "Deportes", "Libros y hobbies", "Otros"
    )

    val comunas = listOf(
        "Cerrillos", "Conchalí", "El Bosque", "Estación Central", "Huechuraba",
        "Independencia", "La Cisterna", "La Florida", "La Granja", "La Reina",
        "Las Condes", "Lo Barnechea", "Lo Espejo", "Macul", "Maipú", "Ñuñoa",
        "Pedro Aguirre Cerda", "Peñalolén", "Providencia", "Pudahuel", "Puente Alto",
        "Quilicura", "Quinta Normal", "Recoleta", "Renca", "San Bernardo",
        "San Miguel", "Santiago", "Vitacura"
    )

    fun emoji(category: String): String = when (category) {
        "Moda" -> "👗"
        "Hogar" -> "🏠"
        "Bebés y niños" -> "🍼"
        "Tecnología" -> "💻"
        "Mascotas" -> "🐾"
        "Deportes" -> "⚽"
        "Libros y hobbies" -> "📚"
        else -> "🛒"
    }
}
