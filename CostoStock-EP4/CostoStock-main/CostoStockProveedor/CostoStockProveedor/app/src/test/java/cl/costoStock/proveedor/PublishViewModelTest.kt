package cl.costoStock.proveedor

import cl.costoStock.proveedor.data.model.EconomicIndicators
import cl.costoStock.proveedor.data.model.ProductCondition
import cl.costoStock.proveedor.data.repository.IndicatorsRepository
import cl.costoStock.proveedor.data.repository.ProductRepository
import cl.costoStock.proveedor.testutil.MainDispatcherRule
import cl.costoStock.proveedor.testutil.indicators
import cl.costoStock.proveedor.testutil.product
import cl.costoStock.proveedor.ui.publish.PublishSteps
import cl.costoStock.proveedor.ui.publish.PublishViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PublishViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val productRepository = mockk<ProductRepository>(relaxed = true)
    private val indicatorsRepository = mockk<IndicatorsRepository>(relaxed = true)

    @Before
    fun setUp() {
        every { indicatorsRepository.indicators } returns MutableStateFlow<EconomicIndicators?>(indicators)
        coEvery { indicatorsRepository.refresh(any()) } returns true
    }

    private fun newViewModel() = PublishViewModel(productRepository, indicatorsRepository)

    /** Completa los 3 primeros pasos con datos válidos y deja el asistente en "Revisar". */
    private fun PublishViewModel.fillValid() {
        onNameChange("Lámpara de escritorio LED")
        onCategoryChange("Hogar")
        onConditionChange(ProductCondition.NEW)
        onDescriptionChange("Lámpara con tres niveles de luz y brazo flexible")
        next()
        addPhoto("/cache/a.jpg")
        next()
        onPriceChange("12990")
        onStockChange("4")
        onComunaChange("Maipú")
        next()
    }

    @Test
    fun cadaPasoValidaAntesDeAvanzar() = runTest {
        val vm = newViewModel()
        assertEquals(indicators, vm.indicators.value)
        vm.start(-1L)
        assertFalse(vm.state.value.isDirty)

        vm.next()
        val s = vm.state.value
        assertEquals(PublishSteps.PRODUCT, s.step)
        assertNotNull(s.nameError)
        assertNotNull(s.categoryError)
        assertNotNull(s.conditionError)
        assertNotNull(s.descriptionError)
        assertEquals(1, s.shakeCount)

        vm.onNameBlur()
        vm.onDescriptionBlur()
        vm.onNameChange("Lámpara de escritorio LED")
        vm.onDescriptionChange("Lámpara con tres niveles de luz y brazo flexible")
        vm.onCategoryChange("Hogar")
        vm.onConditionChange(ProductCondition.USED)
        assertNull(vm.state.value.nameError)
        assertTrue(vm.state.value.isDirty)
        vm.next()
        assertEquals(PublishSteps.PHOTOS, vm.state.value.step)

        vm.next()
        assertNotNull(vm.state.value.photosError)
        vm.addPhoto("/cache/a.jpg")
        vm.addPhoto("/cache/b.jpg")
        vm.addPhoto("/cache/c.jpg")
        vm.addPhoto("/cache/d.jpg") // supera el máximo: se descarta
        assertEquals(3, vm.state.value.photos.size)
        vm.makeCover(2)
        assertEquals("/cache/c.jpg", vm.state.value.photos[0])
        vm.makeCover(0)
        vm.makeCover(9)
        vm.removePhoto(1)
        vm.removePhoto(9)
        assertEquals(listOf("/cache/c.jpg", "/cache/b.jpg"), vm.state.value.photos)
        vm.next()
        assertEquals(PublishSteps.PRICE, vm.state.value.step)

        vm.next()
        assertNotNull(vm.state.value.priceError)
        assertNotNull(vm.state.value.stockError)
        assertNotNull(vm.state.value.comunaError)
        vm.onPriceBlur()
        vm.onStockBlur()
        vm.onPriceChange("12990")
        vm.onStockChange("4")
        vm.onComunaChange("Maipú")
        assertEquals(12990, vm.state.value.priceValue)
        assertEquals(4, vm.state.value.stockValue)
        vm.next()
        assertEquals(PublishSteps.REVIEW, vm.state.value.step)

        vm.previous()
        assertEquals(PublishSteps.PRICE, vm.state.value.step)
    }

    @Test
    fun publicarExitosoYFallido() = runTest {
        val vm = newViewModel()
        vm.start(0L)
        vm.fillValid()

        coEvery { productRepository.publish(any()) } returns null
        every { productRepository.lastError } returns "Sin conexión con el servidor"
        vm.next()
        assertEquals("Sin conexión con el servidor", vm.state.value.formError)
        assertFalse(vm.state.value.completed)

        coEvery { productRepository.publish(any()) } returns 10L
        vm.next()
        assertTrue(vm.state.value.completed)
        assertFalse(vm.state.value.isDirty)
        coVerify { productRepository.publish(match { it.price == 12990 && it.imagePaths == listOf("/cache/a.jpg") }) }

        vm.reset()
        assertEquals(PublishSteps.PRODUCT, vm.state.value.step)
        assertEquals("", vm.state.value.name)
    }

    @Test
    fun editarUnaPublicacionExistente() = runTest {
        coEvery { productRepository.getMyProduct(5L) } returns product(5L, price = 15000, stock = 2)
        coEvery { productRepository.update(5L, any()) } returns true
        val vm = newViewModel()

        vm.start(5L)
        vm.start(5L) // la segunda llamada se ignora
        val s = vm.state.value
        assertTrue(s.isEditing)
        assertFalse(s.isLoadingProduct)
        assertEquals("15000", s.price)
        assertEquals(listOf("/fotos/5.jpg"), s.photos)

        vm.next(); vm.next(); vm.next(); vm.next()

        assertTrue(vm.state.value.completed)
        coVerify { productRepository.update(5L, any()) }
    }

    @Test
    fun editarUnaPublicacionQueNoExisteVuelveAModoNuevo() = runTest {
        coEvery { productRepository.getMyProduct(7L) } returns null
        val vm = newViewModel()

        vm.start(7L)

        assertFalse(vm.state.value.isEditing)
        assertFalse(vm.state.value.isLoadingProduct)
    }
}
