package com.example.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.TextOverlay
import com.example.util.FilterType
import com.example.util.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditorTab {
    FILTERS, ADJUST, CROP, TEXT
}

data class PresetImage(
    val id: String,
    val name: String,
    val url: String?
)

data class EditorState(
    val currentBitmap: Bitmap? = null,
    val activeTab: EditorTab = EditorTab.FILTERS,
    val activeFilter: FilterType = FilterType.NONE,
    
    // Adjustments sliders
    val brightness: Float = 0f, // -150f to 150f color offset
    val contrast: Float = 1.0f,     // 0.4f to 2.2f scale multiplier
    val saturation: Float = 1.0f,   // 0.0f to 2.2f sat scale

    // Cropping bounds (normalized 0.0 to 1.0)
    val cropLeft: Float = 0.0f,
    val cropTop: Float = 0.0f,
    val cropRight: Float = 1.0f,
    val cropBottom: Float = 1.0f,

    // Overlaid texts
    val texts: List<TextOverlay> = emptyList(),
    val selectedTextId: String? = null,

    // Dialog & UI Status indicators
    val isSaving: Boolean = false,
    val showSaveSuccessDialog: Boolean = false,
    val savedImagePath: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // Undo / Redo active positions
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

class ImageEditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    // Bounded historical records to preserve memory safely (Max 8 high-res Bitmaps)
    private val historyList = mutableListOf<Bitmap>()
    private var historyIndex = -1

    // Pre-configured online gallery presets for seamless testing & local fallback
    val presets = listOf(
        PresetImage("fallback", "Atardecer Artístico", null),
        PresetImage("mountain", "Montañas", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&auto=format&fit=crop&q=80"),
        PresetImage("cat", "Gatito", "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=800&auto=format&fit=crop&q=80"),
        PresetImage("cyberpunk", "Cyberpunk", "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=800&auto=format&fit=crop&q=80"),
        PresetImage("desert", "Desierto", "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=800&auto=format&fit=crop&q=80")
    )

    init {
        // Load default beautiful landscape bitmap immediately
        loadDefaultBitmap()
    }

    private fun loadDefaultBitmap() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val bitmap = ImageUtils.generateSunsetFallback()
                setNewWorkingBitmap(bitmap, clearHistory = true)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Error al crear imagen por defecto: ${e.localizedMessage}") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadPreset(preset: PresetImage, loadedBitmap: Bitmap?) {
        if (preset.url == null) {
            loadDefaultBitmap()
            return
        }
        if (loadedBitmap != null) {
            val resized = ImageUtils.getResizedBitmap(loadedBitmap, 1200)
            setNewWorkingBitmap(resized, clearHistory = true)
        } else {
            _state.update { it.copy(errorMessage = "No se pudo cargar la imagen del preset. Intente cargando desde galería.") }
        }
    }

    fun loadCustomImage(bitmap: Bitmap) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val resized = ImageUtils.getResizedBitmap(bitmap, 1200)
                setNewWorkingBitmap(resized, clearHistory = true)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Error al cargar imagen custom: ${e.localizedMessage}") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun setNewWorkingBitmap(bitmap: Bitmap, clearHistory: Boolean = false) {
        if (clearHistory) {
            historyList.clear()
            historyList.add(bitmap)
            historyIndex = 0
        } else {
            // Cut off forward redo elements if we make custom modifications
            if (historyIndex < historyList.lastIndex) {
                val itemsToRemove = historyList.size - 1 - historyIndex
                repeat(itemsToRemove) {
                    historyList.removeAt(historyList.lastIndex)
                }
            }
            historyList.add(bitmap)
            if (historyList.size > 8) {
                historyList.removeAt(0)
            }
            historyIndex = historyList.lastIndex
        }

        _state.update {
            it.copy(
                currentBitmap = bitmap,
                canUndo = historyIndex > 0,
                canRedo = historyIndex < historyList.lastIndex,
                // Automatically reset crop bounding selector for newly sized bitmap boundaries
                cropLeft = 0.05f,
                cropTop = 0.05f,
                cropRight = 0.95f,
                cropBottom = 0.95f
            )
        }
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            val bitmap = historyList[historyIndex]
            _state.update {
                it.copy(
                    currentBitmap = bitmap,
                    canUndo = historyIndex > 0,
                    canRedo = historyIndex < historyList.lastIndex
                )
            }
        }
    }

    fun redo() {
        if (historyIndex < historyList.lastIndex) {
            historyIndex++
            val bitmap = historyList[historyIndex]
            _state.update {
                it.copy(
                    currentBitmap = bitmap,
                    canUndo = historyIndex > 0,
                    canRedo = historyIndex < historyList.lastIndex
                )
            }
        }
    }

    fun setTab(tab: EditorTab) {
        _state.update { it.copy(activeTab = tab, selectedTextId = null) }
    }

    // SLIDERS & ADJUSTMENTS
    fun updateBrightness(v: Float) {
        _state.update { it.copy(brightness = v) }
    }

    fun updateContrast(v: Float) {
        _state.update { it.copy(contrast = v) }
    }

    fun updateSaturation(v: Float) {
        _state.update { it.copy(saturation = v) }
    }

    fun setFilter(filter: FilterType) {
        _state.update { it.copy(activeFilter = filter) }
    }

    fun resetAdjustments() {
        _state.update {
            it.copy(
                brightness = 0f,
                contrast = 1.0f,
                saturation = 1.0f,
                activeFilter = FilterType.NONE
            )
        }
    }

    // CROPPING ACTIONS
    fun updateCropBounds(left: Float, top: Float, right: Float, bottom: Float) {
        _state.update {
            it.copy(
                cropLeft = left,
                cropTop = top,
                cropRight = right,
                cropBottom = bottom
            )
        }
    }

    fun applyPresetCropRatio(ratio: Float?) {
        val left: Float
        val top: Float
        val right: Float
        val bottom: Float

        if (ratio == null) {
            // Reset to default safe margins
            left = 0.05f
            top = 0.05f
            right = 0.95f
            bottom = 0.95f
        } else {
            // Format crop box coordinates symmetrically around center matching designated ratio
            // Ratio = Width / Height. In normalized coordinates, size is width and height.
            // Let's assume height is 0.8f. Width then is 0.8f * ratio.
            val targetHeight = 0.7f
            val targetWidth = targetHeight * ratio

            if (targetWidth <= 0.9f) {
                left = 0.5f - (targetWidth / 2f)
                right = 0.5f + (targetWidth / 2f)
                top = 0.5f - (targetHeight / 2f)
                bottom = 0.5f + (targetHeight / 2f)
            } else {
                // If width overflows, constrain width instead
                val altWidth = 0.9f
                val altHeight = altWidth / ratio
                left = 0.05f
                right = 0.95f
                top = 0.5f - (altHeight / 2f)
                bottom = 0.5f + (altHeight / 2f)
            }
        }
        updateCropBounds(left.coerceIn(0f, 0.45f), top.coerceIn(0f, 0.45f), right.coerceIn(0.55f, 1f), bottom.coerceIn(0.55f, 1f))
    }

    fun applyCrop() {
        val bitmap = _state.value.currentBitmap ?: return
        val cropped = ImageUtils.cropBitmap(
            bitmap,
            _state.value.cropLeft,
            _state.value.cropTop,
            _state.value.cropRight,
            _state.value.cropBottom
        )
        setNewWorkingBitmap(cropped)
    }

    fun rotate90() {
        val bitmap = _state.value.currentBitmap ?: return
        val rotated = ImageUtils.rotateBitmap(bitmap, 90f)
        setNewWorkingBitmap(rotated)
    }

    fun flipHorizontal() {
        val bitmap = _state.value.currentBitmap ?: return
        val flipped = ImageUtils.flipBitmap(bitmap, horizontal = true, vertical = false)
        setNewWorkingBitmap(flipped)
    }

    fun flipVertical() {
        val bitmap = _state.value.currentBitmap ?: return
        val flipped = ImageUtils.flipBitmap(bitmap, horizontal = false, vertical = true)
        setNewWorkingBitmap(flipped)
    }

    // TEXTS MANAGEMENT
    fun addText(text: String, color: Int, fontSize: Float) {
        if (text.isBlank()) return
        val newText = TextOverlay(
            text = text,
            color = color,
            fontSizeSp = fontSize,
            xNormalized = 0.5f,
            yNormalized = 0.5f
        )
        _state.update {
            it.copy(
                texts = it.texts + newText,
                selectedTextId = newText.id
            )
        }
    }

    fun selectText(id: String?) {
        _state.update { it.copy(selectedTextId = id) }
    }

    fun dragText(id: String, deltaX: Float, deltaY: Float) {
        _state.update { s ->
            s.copy(
                texts = s.texts.map { item ->
                    if (item.id == id) {
                        item.copy(
                            xNormalized = (item.xNormalized + deltaX).coerceIn(0.01f, 0.99f),
                            yNormalized = (item.yNormalized + deltaY).coerceIn(0.01f, 0.99f)
                        )
                    } else item
                }
            )
        }
    }

    fun updateSelectedTextProps(text: String, color: Int, fontSize: Float) {
        val selectedId = _state.value.selectedTextId ?: return
        _state.update { s ->
            s.copy(
                texts = s.texts.map { item ->
                    if (item.id == selectedId) {
                        item.copy(text = text, color = color, fontSizeSp = fontSize)
                    } else item
                }
            )
        }
    }

    fun removeText(id: String) {
        _state.update { s ->
            s.copy(
                texts = s.texts.filter { it.id != id },
                selectedTextId = if (s.selectedTextId == id) null else s.selectedTextId
            )
        }
    }

    fun dismissSaveSuccess() {
        _state.update { it.copy(showSaveSuccessDialog = false) }
    }

    // EXPORT & SAVE RENDERED DESIGN
    // We compose everything down to the underlying bitmap on back-threads
    fun saveRenderedImage(onSaveComplete: (Bitmap) -> Unit) {
        val bitmap = _state.value.currentBitmap ?: return
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                // Apply color filters + brightness sliders + text overlays into a single heavy high-res file
                val rendered = ImageUtils.renderFinalBitmap(
                    baseBitmap = bitmap,
                    filterType = _state.value.activeFilter,
                    brightness = _state.value.brightness,
                    contrast = _state.value.contrast,
                    saturation = _state.value.saturation,
                    texts = _state.value.texts
                )
                // Invoke callback to pass the bitmap to MainActivity for local hardware media-store output saving file
                onSaveComplete(rendered)
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Error al exportar: ${e.localizedMessage}") }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    fun notifyImageSaved(absolutePath: String) {
        _state.update {
            it.copy(
                showSaveSuccessDialog = true,
                savedImagePath = absolutePath
            )
        }
    }

    // Commit filters permanent on direct bitmap (called when they click Apply of adjustments)
    fun bakeFiltersToBitmap() {
        val bitmap = _state.value.currentBitmap ?: return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val baked = ImageUtils.renderFinalBitmap(
                    baseBitmap = bitmap,
                    filterType = _state.value.activeFilter,
                    brightness = _state.value.brightness,
                    contrast = _state.value.contrast,
                    saturation = _state.value.saturation,
                    texts = emptyList() // Preserve texts editable separate, only bake color transformations
                )
                setNewWorkingBitmap(baked)
                // Reset adjustment sliders to neutral since we committed them into the physical layout of currentBitmap
                resetAdjustments()
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "Error al aplicar filtros: ${e.localizedMessage}") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
