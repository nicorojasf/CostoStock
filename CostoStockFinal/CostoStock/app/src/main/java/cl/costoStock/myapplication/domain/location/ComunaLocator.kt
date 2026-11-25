package cl.costoStock.myapplication.domain.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Convierte coordenadas GPS en la comuna más cercana de la Región Metropolitana, comparando con el
 * centro aproximado de cada comuna (distancia de Haversine). Funciona sin internet.
 */
object ComunaLocator {

    data class Punto(val lat: Double, val lon: Double)

    /** Más lejos que esto de todas las comunas se considera fuera de la RM. */
    const val RADIO_MAXIMO_KM = 40.0

    private const val RADIO_TIERRA_KM = 6371.0

    val centroides: Map<String, Punto> = mapOf(
        "Cerrillos" to Punto(-33.4920, -70.7160),
        "Cerro Navia" to Punto(-33.4250, -70.7350),
        "Conchalí" to Punto(-33.3830, -70.6750),
        "El Bosque" to Punto(-33.5620, -70.6760),
        "Estación Central" to Punto(-33.4590, -70.6980),
        "Huechuraba" to Punto(-33.3670, -70.6330),
        "Independencia" to Punto(-33.4170, -70.6650),
        "La Cisterna" to Punto(-33.5300, -70.6640),
        "La Florida" to Punto(-33.5227, -70.5980),
        "La Granja" to Punto(-33.5360, -70.6250),
        "La Pintana" to Punto(-33.5830, -70.6340),
        "La Reina" to Punto(-33.4450, -70.5350),
        "Las Condes" to Punto(-33.4080, -70.5670),
        "Lo Barnechea" to Punto(-33.3500, -70.5180),
        "Lo Espejo" to Punto(-33.5210, -70.6910),
        "Lo Prado" to Punto(-33.4440, -70.7240),
        "Macul" to Punto(-33.4920, -70.5980),
        "Maipú" to Punto(-33.5110, -70.7580),
        "Ñuñoa" to Punto(-33.4569, -70.5977),
        "Pedro Aguirre Cerda" to Punto(-33.4930, -70.6780),
        "Peñalolén" to Punto(-33.4850, -70.5400),
        "Providencia" to Punto(-33.4314, -70.6093),
        "Pudahuel" to Punto(-33.4400, -70.7600),
        "Puente Alto" to Punto(-33.6117, -70.5758),
        "Quilicura" to Punto(-33.3550, -70.7290),
        "Quinta Normal" to Punto(-33.4280, -70.6990),
        "Recoleta" to Punto(-33.4060, -70.6400),
        "Renca" to Punto(-33.4060, -70.7280),
        "San Bernardo" to Punto(-33.5920, -70.7000),
        "San Joaquín" to Punto(-33.4960, -70.6280),
        "San Miguel" to Punto(-33.4970, -70.6510),
        "Santiago" to Punto(-33.4489, -70.6693),
        "Vitacura" to Punto(-33.3900, -70.5720)
    )

    fun distanciaKm(a: Punto, b: Punto): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
        return 2 * RADIO_TIERRA_KM * asin(sqrt(h))
    }

    /**
     * Comuna más cercana entre [candidatas] (por defecto todas). Devuelve null si ninguna está a
     * menos de [maxKm] o si ninguna candidata tiene coordenadas conocidas.
     */
    fun nearest(
        lat: Double,
        lon: Double,
        candidatas: Collection<String> = centroides.keys,
        maxKm: Double = RADIO_MAXIMO_KM
    ): String? {
        val origen = Punto(lat, lon)
        return candidatas
            .mapNotNull { comuna -> centroides[comuna]?.let { comuna to distanciaKm(origen, it) } }
            .filter { it.second <= maxKm }
            .minByOrNull { it.second }
            ?.first
    }
}
