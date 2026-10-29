package cl.costoStock.proveedor.ui.publish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.costoStock.proveedor.data.model.ProductCondition
import cl.costoStock.proveedor.data.model.ProductDraft
import cl.costoStock.proveedor.data.repository.ProductRepository
import cl.costoStock.proveedor.domain.validation.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

object PublishSteps {
    const val PRODUCT = 0
    const val PHOTOS = 1
    const val PRICE = 2
    const val REVIEW = 3
    val labels = listOf("Producto", "Fotos", "Precio", "Revisar")
    val last = REVIEW
}

data class PublishUiState(
    val isEditing: Boolean = false,
    val isLoadingProduct: Boolean = false,
    val step: Int = PublishSteps.PRODUCT,

    // Paso 1: producto
    val name: String = "",
    val category: String? = null,
    val condition: ProductCondition? = null,
    val description: String = "",
    val nameError: String? = null,
    val categoryError: String? = null,
    val conditionError: String? = null,
    val descriptionError: String? = null,

    // Paso 2: fotos (la primera es la portada)
    val photos: List<String> = emptyList(),
    val photosError: String? = null,

    // Paso 3: precio, stock y ubicación
    val price: String = "",
    val stock: String = "",
    val comuna: String? = null,
    val priceError: String? = null,
    val stockError: String? = null,
    val comunaError: String? = null,

    val isSaving: Boolean = false,
    val formError: String? = null,
    /** Se incrementa en cada intento fallido para disparar animación y vibración. */
    val shakeCount: Int = 0,
    val completed: Boolean = false
) {
    val priceValue: Int? get() = Validators.parsePrice(price)
    val stockValue: Int? get() = stock.trim().toIntOrNull()

    /** true si el usuario ya escribió algo (para preguntar antes de descartar). */
    val isDirty: Boolean
        get() = !completed && (name.isNotBlank() || description.isNotBlank() || photos.isNotEmpty() ||
            price.isNotBlank() || stock.isNotBlank() || category != null || comuna != null)
}

class PublishViewModel(private val productRepository: ProductRepository) : ViewModel() {

    private val _state = MutableStateFlow(PublishUiState())
    val state: StateFlow<PublishUiState> = _state.asStateFlow()

    private var started = false
    private var editingProductId: Long? = null

    /** Se llama al abrir la pantalla: si recibe el id de una publicación, la carga para editarla. */
    fun start(productId: Long) {
        if (started) return
        started = true
        if (productId <= 0) return
        editingProductId = productId
        _state.update { it.copy(isEditing = true, isLoadingProduct = true) }
        viewModelScope.launch {
            val product = productRepository.getMyProduct(productId)
            if (product == null) {
                editingProductId = null
                _state.update { it.copy(isEditing = false, isLoadingProduct = false) }
                return@launch
            }
            _state.update {
                it.copy(
                    isLoadingProduct = false,
                    name = product.name,
                    category = product.category,
                    condition = product.condition,
                    description = product.description,
                    photos = product.imagePaths,
                    price = product.price.toString(),
                    stock = product.stock.toString(),
                    comuna = product.comuna
                )
            }
        }
    }

    // ------------------------------ Paso 1 ------------------------------

    fun onNameChange(value: String) = _state.update {
        it.copy(name = value, formError = null,
            nameError = if (it.nameError != null) Validators.validateTitle(value) else null)
    }

    fun onCategoryChange(value: String) = _state.update {
        it.copy(category = value, categoryError = null, formError = null)
    }

    fun onConditionChange(value: ProductCondition) = _state.update {
        it.copy(condition = value, conditionError = null, formError = null)
    }

    fun onDescriptionChange(value: String) = _state.update {
        it.copy(description = value, formError = null,
            descriptionError = if (it.descriptionError != null) Validators.validateDescription(value) else null)
    }

    fun onNameBlur() = _state.update { it.copy(nameError = Validators.validateTitle(it.name)) }
    fun onDescriptionBlur() = _state.update {
        it.copy(descriptionError = Validators.validateDescription(it.description))
    }

    // ------------------------------ Paso 2 ------------------------------

    /** [path] es el archivo temporal que dejó la cámara. */
    fun addPhoto(path: String) {
        val current = _state.value
        if (current.photos.size >= Validators.MAX_PHOTOS) {
            File(path).delete()
            return
        }
        _state.update { it.copy(photos = it.photos + path, photosError = null, formError = null) }
    }

    fun removePhoto(index: Int) = _state.update {
        if (index !in it.photos.indices) it else it.copy(photos = it.photos.filterIndexed { i, _ -> i != index })
    }

    fun makeCover(index: Int) = _state.update {
        if (index !in it.photos.indices || index == 0) it
        else it.copy(photos = listOf(it.photos[index]) + it.photos.filterIndexed { i, _ -> i != index })
    }

    // ------------------------------ Paso 3 ------------------------------

    fun onPriceChange(value: String) = _state.update {
        it.copy(price = value, formError = null,
            priceError = if (it.priceError != null) Validators.validatePrice(value) else null)
    }

    fun onStockChange(value: String) = _state.update {
        it.copy(stock = value, formError = null,
            stockError = if (it.stockError != null) Validators.validateStock(value) else null)
    }

    fun onComunaChange(value: String) = _state.update {
        it.copy(comuna = value, comunaError = null, formError = null)
    }

    fun onPriceBlur() = _state.update { it.copy(priceError = Validators.validatePrice(it.price)) }
    fun onStockBlur() = _state.update { it.copy(stockError = Validators.validateStock(it.stock)) }

    // ------------------------------ Navegación del asistente ------------------------------

    fun previous() = _state.update {
        if (it.step > 0) it.copy(step = it.step - 1, formError = null) else it
    }

    /** Valida el paso actual; si está bien avanza, y en el último paso publica. */
    fun next() {
        val s = _state.value
        if (s.isSaving) return
        if (!validateStep(s.step)) return
        if (s.step < PublishSteps.last) {
            _state.update { it.copy(step = it.step + 1, formError = null) }
        } else {
            save()
        }
    }

    /** Vuelve a mostrar el formulario vacío para publicar otro artículo. */
    fun reset() {
        started = true
        editingProductId = null
        _state.value = PublishUiState()
    }

    // ------------------------------ Internos ------------------------------

    private fun validateStep(step: Int): Boolean {
        val s = _state.value
        return when (step) {
            PublishSteps.PRODUCT -> {
                val nameError = Validators.validateTitle(s.name)
                val categoryError = Validators.validateCategory(s.category)
                val conditionError = Validators.validateCondition(s.condition?.name)
                val descriptionError = Validators.validateDescription(s.description)
                val ok = listOf(nameError, categoryError, conditionError, descriptionError).all { it == null }
                if (!ok) _state.update {
                    it.copy(
                        nameError = nameError, categoryError = categoryError,
                        conditionError = conditionError, descriptionError = descriptionError,
                        shakeCount = it.shakeCount + 1
                    )
                }
                ok
            }
            PublishSteps.PHOTOS -> {
                val error = Validators.validatePhotos(s.photos.size)
                if (error != null) _state.update { it.copy(photosError = error, shakeCount = it.shakeCount + 1) }
                error == null
            }
            PublishSteps.PRICE -> {
                val priceError = Validators.validatePrice(s.price)
                val stockError = Validators.validateStock(s.stock)
                val comunaError = Validators.validateComuna(s.comuna)
                val ok = listOf(priceError, stockError, comunaError).all { it == null }
                if (!ok) _state.update {
                    it.copy(
                        priceError = priceError, stockError = stockError, comunaError = comunaError,
                        shakeCount = it.shakeCount + 1
                    )
                }
                ok
            }
            else -> true
        }
    }

    private fun save() {
        val s = _state.value
        val draft = ProductDraft(
            name = s.name.trim(),
            category = s.category ?: return,
            description = s.description.trim(),
            price = s.priceValue ?: return,
            stock = s.stockValue ?: return,
            comuna = s.comuna ?: return,
            condition = s.condition ?: return,
            imagePaths = s.photos
        )
        _state.update { it.copy(isSaving = true, formError = null) }
        viewModelScope.launch {
            val editingId = editingProductId
            val ok = if (editingId != null) {
                productRepository.update(editingId, draft)
            } else {
                productRepository.publish(draft) != null
            }
            _state.update {
                if (ok) it.copy(isSaving = false, completed = true)
                else it.copy(
                    isSaving = false,
                    formError = "No se pudo guardar la publicación. Inténtalo nuevamente",
                    shakeCount = it.shakeCount + 1
                )
            }
        }
    }
}
