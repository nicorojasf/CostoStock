package cl.costoStock.myapplication.data.repository

import cl.costoStock.myapplication.data.model.EconomicIndicators
import cl.costoStock.myapplication.data.remote.IndicadoresApi
import cl.costoStock.myapplication.data.remote.toIndicators
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Indicadores económicos desde la API externa mindicador.cl (consumida con Retrofit).
 * Es independiente de los microservicios: si falla, la app sigue funcionando sin conversiones.
 */
class IndicatorsRepository(
    private val api: IndicadoresApi,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val _indicators = MutableStateFlow<EconomicIndicators?>(null)
    val indicators: StateFlow<EconomicIndicators?> = _indicators.asStateFlow()

    private var lastFetch = 0L

    /** Descarga los indicadores (con caché de [CACHE_MS]). Devuelve true si hay datos disponibles. */
    suspend fun refresh(force: Boolean = false): Boolean = withContext(io) {
        val cached = _indicators.value
        if (!force && cached != null && clock() - lastFetch < CACHE_MS) return@withContext true
        try {
            val result = api.obtener().toIndicators()
            if (result != null) {
                _indicators.value = result
                lastFetch = clock()
            }
            _indicators.value != null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _indicators.value != null
        }
    }

    companion object {
        const val CACHE_MS = 30 * 60 * 1000L
    }
}
