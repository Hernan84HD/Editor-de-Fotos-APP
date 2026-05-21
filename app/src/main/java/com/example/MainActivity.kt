package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.model.TextOverlay
import com.example.ui.InteractiveCropOverlay
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FilterType
import com.example.util.ImageUtils
import com.example.viewmodel.EditorState
import com.example.viewmodel.EditorTab
import com.example.viewmodel.ImageEditorViewModel
import com.example.viewmodel.PresetImage
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// Elegant Slate dark color theme properties to prioritize photo aesthetic fidelity
val StudioDarkBg = Color(0xFF0D0F16)
val StudioDarkSurface = Color(0xFF161925)
val StudioDarkSurfaceCard = Color(0xFF1F2336)
val ActivePillColor = Color(0xFF10B981) // High contrast gorgeous Emerald Mint accent

class MainActivity : ComponentActivity() {

    private val viewModel: ImageEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StudioDarkBg),
                    containerColor = StudioDarkBg
                ) { innerPadding ->
                    ImageEditorScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ImageEditorScreen(
    viewModel: ImageEditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()

    // 1. Gallery Launcher selection activity
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri).use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        viewModel.loadCustomImage(bitmap)
                    } else {
                        Toast.makeText(context, "No se pudo decodificar la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Export executor utility writing output files cleanly
    val onExportRequest: () -> Unit = {
        viewModel.saveRenderedImage { renderedBitmap ->
            try {
                // Store safely in Sandbox Pictures directory - needs absolutely no permissions!
                val folder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val file = File(folder, "fotoedit_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    renderedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                viewModel.notifyImageSaved(file.absolutePath)
            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar archivo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
    ) {
        // A. Premium top header toolbar bar containing Undo, Redo, Logo & Save pills
        TopBar(
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            isSaving = state.isSaving,
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onExport = onExportRequest,
            onLoadGallery = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        )

        // B. Unified visual canvas workspace featuring auto-fitted ratios and custom clipping bounds
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = ActivePillColor, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Cargando lienzo...",
                        color = Color.White.copy(0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                state.currentBitmap?.let { bmp ->
                    // Workspace auto proportion mathematically aligned to image density bounds
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val containerWidth = maxWidth
                        val containerHeight = maxHeight

                        val imageWidth = bmp.width.toFloat()
                        val imageHeight = bmp.height.toFloat()
                        val imageRatio = imageWidth / imageHeight
                        val containerRatio = containerWidth.value / containerHeight.value

                        val (drawnWidth, drawnHeight) = if (imageRatio > containerRatio) {
                            Pair(containerWidth, containerWidth / imageRatio)
                        } else {
                            Pair(containerHeight * imageRatio, containerHeight)
                        }

                        // Beautiful shadow-framed container
                        Box(
                            modifier = Modifier
                                .size(drawnWidth, drawnHeight)
                                .shadow(12.dp, RoundedCornerShape(4.dp))
                                .background(Color.Black)
                                .clipToBounds()
                                .testTag("work_canvas_box")
                        ) {
                            // Compute realtime GPU filters matrix preview to maintain 60FPS fluid sliders
                            val filterMatrixRaw = ImageUtils.getFilterMatrix(state.activeFilter)
                            val adjustMatrixRaw = ImageUtils.createAdjustmentMatrix(
                                state.brightness,
                                state.contrast,
                                state.saturation
                            )
                            val combinedMatrixRaw = ImageUtils.multiplyMatrices(adjustMatrixRaw, filterMatrixRaw)
                            val composeColorFilter = ColorFilter.colorMatrix(ColorMatrix(combinedMatrixRaw))

                            // Draw visual background image
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Lienzo de edición",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds,
                                colorFilter = composeColorFilter
                            )

                            // Render and position text layers dynamically with manual offset math
                            TextOverlaysWorkspace(
                                texts = state.texts,
                                selectedId = state.selectedTextId,
                                onSelect = { viewModel.selectText(it) },
                                onDrag = { id, dx, dy -> viewModel.dragText(id, dx, dy) },
                                onRemove = { viewModel.removeText(it) }
                            )

                            // Apply crop handles visual bounds over standard layout
                            if (state.activeTab == EditorTab.CROP) {
                                InteractiveCropOverlay(
                                    cropLeft = state.cropLeft,
                                    cropTop = state.cropTop,
                                    cropRight = state.cropRight,
                                    cropBottom = state.cropBottom,
                                    onCropBoundsChanged = { l, t, r, b ->
                                        viewModel.updateCropBounds(l, t, r, b)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // C. Clean control settings & panel depending on current tab context
        ControlSettingsBox(
            state = state,
            viewModel = viewModel,
            onPresetClick = { preset ->
                if (preset.url == null) {
                    viewModel.loadPreset(preset, null)
                } else {
                    // Fetch preset URL using Coil request
                    val loader = ImageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(preset.url)
                        .allowHardware(false) // CRITICAL: Disable hardware compression to enable custom pixels cropping/drawing downstream
                        .build()
                    coroutineScope.launch {
                        val loadRes = loader.execute(req)
                        val draw = loadRes.drawable
                        if (draw is BitmapDrawable) {
                            viewModel.loadPreset(preset, draw.bitmap)
                        } else {
                            Toast.makeText(context, "Error cargando preset del servidor", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )

        // D. Sleek dark bottom standard navigation bar
        NavigationBar(
            containerColor = StudioDarkSurface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigationBarItem(
                selected = state.activeTab == EditorTab.FILTERS,
                onClick = { viewModel.setTab(EditorTab.FILTERS) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Filtros") },
                label = { Text("Filtros", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = StudioDarkBg,
                    selectedTextColor = ActivePillColor,
                    indicatorColor = ActivePillColor,
                    unselectedIconColor = Color.White.copy(0.5f),
                    unselectedTextColor = Color.White.copy(0.5f)
                ),
                modifier = Modifier.testTag("nav_tab_filters")
            )
            NavigationBarItem(
                selected = state.activeTab == EditorTab.ADJUST,
                onClick = { viewModel.setTab(EditorTab.ADJUST) },
                icon = { Icon(Icons.Default.Tune, contentDescription = "Ajustar") },
                label = { Text("Ajustar", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = StudioDarkBg,
                    selectedTextColor = ActivePillColor,
                    indicatorColor = ActivePillColor,
                    unselectedIconColor = Color.White.copy(0.5f),
                    unselectedTextColor = Color.White.copy(0.5f)
                ),
                modifier = Modifier.testTag("nav_tab_adjust")
            )
            NavigationBarItem(
                selected = state.activeTab == EditorTab.CROP,
                onClick = { viewModel.setTab(EditorTab.CROP) },
                icon = { Icon(Icons.Default.Crop, contentDescription = "Recortar") },
                label = { Text("Recortar", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = StudioDarkBg,
                    selectedTextColor = ActivePillColor,
                    indicatorColor = ActivePillColor,
                    unselectedIconColor = Color.White.copy(0.5f),
                    unselectedTextColor = Color.White.copy(0.5f)
                ),
                modifier = Modifier.testTag("nav_tab_crop")
            )
            NavigationBarItem(
                selected = state.activeTab == EditorTab.TEXT,
                onClick = { viewModel.setTab(EditorTab.TEXT) },
                icon = { Icon(Icons.Default.TextFields, contentDescription = "Texto") },
                label = { Text("Texto", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = StudioDarkBg,
                    selectedTextColor = ActivePillColor,
                    indicatorColor = ActivePillColor,
                    unselectedIconColor = Color.White.copy(0.5f),
                    unselectedTextColor = Color.White.copy(0.5f)
                ),
                modifier = Modifier.testTag("nav_tab_text")
            )
        }
    }

    // Export success dialog with copy options
    if (state.showSaveSuccessDialog) {
        Dialog(onDismissRequest = { viewModel.dismissSaveSuccess() }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ActivePillColor.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Done,
                            contentDescription = "Completado",
                            tint = ActivePillColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Imagen Exportada",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "La foto se procesó con todas las modificaciones de filtros, textos e inyecciones colorimétricas y se guardó en:",
                        color = Color.White.copy(0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(0.3f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.savedImagePath ?: "",
                            color = ActivePillColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 2
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ruta_foto", state.savedImagePath ?: "")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copiar ruta",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.dismissSaveSuccess() },
                        colors = ButtonDefaults.buttonColors(containerColor = ActivePillColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Aceptar", color = StudioDarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    isSaving: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExport: () -> Unit,
    onLoadGallery: () -> Unit
) {
    Surface(
        color = StudioDarkSurface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo design
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ActivePillColor.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ActivePillColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "FotoEdit",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "Editor Creativo",
                        color = Color.White.copy(0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Central undo/redo stack selectors
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(0.25f)
                    ),
                    modifier = Modifier.testTag("undo_button")
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Deshacer")
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(0.25f)
                    ),
                    modifier = Modifier.testTag("redo_button")
                ) {
                    Icon(Icons.Default.Redo, contentDescription = "Rehacer")
                }
            }

            // Launchers and triggers
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onLoadGallery,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .testTag("import_button")
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Cargar Galería",
                        tint = Color.White.copy(0.85f)
                    )
                }

                Button(
                    onClick = onExport,
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActivePillColor,
                        disabledContainerColor = ActivePillColor.copy(0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("save_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = StudioDarkBg,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                tint = StudioDarkBg,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Guardar",
                                color = StudioDarkBg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextOverlaysWorkspace(
    texts: List<TextOverlay>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDrag: (id: String, deltaX: Float, deltaY: Float) -> Unit,
    onRemove: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Clicking base canvas dismisses selector focus
                detectDragGestures(
                    onDragStart = { onSelect(null) },
                    onDrag = { _, _ -> },
                    onDragEnd = {}
                )
            }
    ) {
        texts.forEach { overlay ->
            val isSelected = overlay.id == selectedId

            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val containerWidth = constraints.maxWidth
                        val containerHeight = constraints.maxHeight

                        val x = (overlay.xNormalized * containerWidth) - (placeable.width / 2)
                        val y = (overlay.yNormalized * containerHeight) - (placeable.height / 2)

                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(
                                x.toInt().coerceIn(0, containerWidth - placeable.width),
                                y.toInt().coerceIn(0, containerHeight - placeable.height)
                            )
                        }
                    }
                    .pointerInput(overlay.id) {
                        detectDragGestures(
                            onDragStart = { onSelect(overlay.id) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val parentW = size.width.toFloat()
                                val parentH = size.height.toFloat()
                                if (parentW > 0 && parentH > 0) {
                                    onDrag(
                                        overlay.id,
                                        dragAmount.x / parentW,
                                        dragAmount.y / parentH
                                    )
                                }
                            }
                        )
                    }
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelect(overlay.id) }
                    .border(
                        BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) ActivePillColor else Color.Transparent
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = overlay.text,
                        style = TextStyle(
                            color = Color(overlay.color),
                            fontSize = overlay.fontSizeSp.sp,
                            fontWeight = FontWeight.ExtraBold,
                            shadow = Shadow(
                                color = Color.Black.copy(0.75f),
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        )
                    )

                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                                .clickable { onRemove(overlay.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Eliminar",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ControlSettingsBox(
    state: EditorState,
    viewModel: ImageEditorViewModel,
    onPresetClick: (PresetImage) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (state.activeTab) {
                EditorTab.FILTERS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Presets Rápidos",
                            color = Color.White.copy(0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            viewModel.presets.forEach { preset ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = StudioDarkSurfaceCard),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(0.1f)),
                                    modifier = Modifier
                                        .width(100.dp)
                                        .clickable { onPresetClick(preset) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ActivePillColor.copy(0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Image,
                                                contentDescription = null,
                                                tint = ActivePillColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            preset.name,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Filtros de Color",
                            color = Color.White.copy(0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Scrollable Filters List
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .testTag("filters_row")
                        ) {
                            val filterList = listOf(
                                FilterType.NONE to "Original",
                                FilterType.GRAYSCALE to "Monocromo",
                                FilterType.SEPIA to "Sepia",
                                FilterType.INVERT to "Invertido",
                                FilterType.VINTAGE to "Vintage",
                                FilterType.COOL to "Frío",
                                FilterType.WARM to "Cálido",
                                FilterType.CONTRAST to "Contraste"
                            )

                            filterList.forEach { (type, name) ->
                                val isSelected = state.activeFilter == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) ActivePillColor else StudioDarkSurfaceCard)
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (isSelected) Color.Transparent else Color.White.copy(0.1f)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.setFilter(type) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        name,
                                        color = if (isSelected) StudioDarkBg else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                EditorTab.ADJUST -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Modulación del Color",
                                color = Color.White.copy(0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { viewModel.resetAdjustments() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Limpiar moduladores",
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Brightness Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Brightness6,
                                        contentDescription = null,
                                        tint = Color.White.copy(0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Brillo", color = Color.White.copy(0.7f), fontSize = 11.sp)
                                }
                                Text(
                                    "${state.brightness.toInt()}",
                                    color = ActivePillColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = state.brightness,
                                onValueChange = { viewModel.updateBrightness(it) },
                                valueRange = -100f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ActivePillColor,
                                    activeTrackColor = ActivePillColor,
                                    inactiveTrackColor = Color.White.copy(0.1f)
                                ),
                                modifier = Modifier.testTag("slider_brightness")
                            )
                        }

                        // Contrast Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Contrast,
                                        contentDescription = null,
                                        tint = Color.White.copy(0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Contraste", color = Color.White.copy(0.7f), fontSize = 11.sp)
                                }
                                Text(
                                    String.format("%.1fx", state.contrast),
                                    color = ActivePillColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = state.contrast,
                                onValueChange = { viewModel.updateContrast(it) },
                                valueRange = 0.4f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ActivePillColor,
                                    activeTrackColor = ActivePillColor,
                                    inactiveTrackColor = Color.White.copy(0.1f)
                                ),
                                modifier = Modifier.testTag("slider_contrast")
                            )
                        }

                        // Saturation Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White.copy(0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Saturación", color = Color.White.copy(0.7f), fontSize = 11.sp)
                                }
                                Text(
                                    String.format("%.1fx", state.saturation),
                                    color = ActivePillColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = state.saturation,
                                onValueChange = { viewModel.updateSaturation(it) },
                                valueRange = 0.0f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ActivePillColor,
                                    activeTrackColor = ActivePillColor,
                                    inactiveTrackColor = Color.White.copy(0.1f)
                                ),
                                modifier = Modifier.testTag("slider_saturation")
                            )
                        }

                        // Bake button
                        Button(
                            onClick = { viewModel.bakeFiltersToBitmap() },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioDarkSurfaceCard),
                            border = BorderStroke(1.dp, ActivePillColor.copy(0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("bake_filters_button")
                        ) {
                            Text("Consolidar Ajustes en Lienzo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                EditorTab.CROP -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Proporciones y Orientación",
                            color = Color.White.copy(0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Presets of crop aspects
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            val crops = listOf(
                                Triple("Libre", null as Float?, "Custom"),
                                Triple("1:1", 1.0f, "Cuadrado"),
                                Triple("4:3", 1.333f, "Clásico"),
                                Triple("16:9", 1.777f, "Cine")
                            )

                            crops.forEach { (label, ratio, subtitle) ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = StudioDarkSurfaceCard),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(0.1f)),
                                    modifier = Modifier
                                        .clickable { viewModel.applyPresetCropRatio(ratio) }
                                        .width(76.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(label, color = ActivePillColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        Text(subtitle, color = Color.White.copy(0.5f), fontSize = 8.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Rotation
                            Button(
                                onClick = { viewModel.rotate90() },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioDarkSurfaceCard),
                                border = BorderStroke(1.dp, Color.White.copy(0.12f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("rotate_90_button")
                            ) {
                                Icon(Icons.Default.RotateRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Girar 90º", color = Color.White, fontSize = 10.sp)
                            }
                            // Mirror H
                            Button(
                                onClick = { viewModel.flipHorizontal() },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioDarkSurfaceCard),
                                border = BorderStroke(1.dp, Color.White.copy(0.12f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("flip_h_button")
                            ) {
                                Icon(Icons.Default.Flip, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Espejo H", color = Color.White, fontSize = 10.sp)
                            }
                            // Mirror V
                            Button(
                                onClick = { viewModel.flipVertical() },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioDarkSurfaceCard),
                                border = BorderStroke(1.dp, Color.White.copy(0.12f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("flip_v_button")
                            ) {
                                Icon(Icons.Default.Flip, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Espejo V", color = Color.White, fontSize = 10.sp)
                            }
                        }

                        // Apply Crop
                        Button(
                            onClick = { viewModel.applyCrop() },
                            colors = ButtonDefaults.buttonColors(containerColor = ActivePillColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("apply_crop_button")
                        ) {
                            Text("Recortar Sección", color = StudioDarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                EditorTab.TEXT -> {
                    var textInput by remember { mutableStateOf("") }
                    var textColorSelected by remember { mutableStateOf(android.graphics.Color.WHITE) }
                    var fontSizeSelected by remember { mutableStateOf(24f) }
                    val focusManager = LocalFocusManager.current

                    val selectedId = state.selectedTextId
                    val hasSelection = selectedId != null

                    // Sync textInput with selection if needed
                    val currentSelection = state.texts.find { it.id == selectedId }
                    val labelTitle = if (hasSelection) "Modificar Texto Seleccionado" else "Añadir Capa de Texto"

                    val colorsPalette = listOf(
                        android.graphics.Color.WHITE to Color.White,
                        android.graphics.Color.BLACK to Color.Black,
                        android.graphics.Color.RED to Color.Red,
                        android.graphics.Color.GREEN to Color.Green,
                        android.graphics.Color.BLUE to Color.Blue,
                        android.graphics.Color.YELLOW to Color.Yellow,
                        0xFFFF5722.toInt() to Color(0xFFFF5722), // Deep Orange
                        0xFFFF007F.toInt() to Color(0xFFFF007F), // Fuchsia
                        0xFF00FFFF.toInt() to Color(0xFF00FFFF), // Cyan
                        0xFF9C27B0.toInt() to Color(0xFF9C27B0)  // Purple
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (hasSelection) ActivePillColor else Color.White)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    labelTitle,
                                    color = Color.White.copy(0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (hasSelection) {
                                TextButton(
                                    onClick = { viewModel.selectText(null) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(0.4f)),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Crear Nuevo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // TextField
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = {
                                textInput = it
                                if (hasSelection) {
                                    viewModel.updateSelectedTextProps(it, textColorSelected, fontSizeSelected)
                                }
                            },
                            placeholder = { Text("Escribe algo...", color = Color.White.copy(0.35f), fontSize = 12.sp) },
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ActivePillColor,
                                unfocusedBorderColor = Color.White.copy(0.12f),
                                focusedContainerColor = StudioDarkSurfaceCard,
                                unfocusedContainerColor = StudioDarkSurfaceCard
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("text_input_field")
                        )

                        // Sizing slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tamaño de Letra", color = Color.White.copy(0.6f), fontSize = 10.sp)
                                Text("${fontSizeSelected.toInt()}px", color = ActivePillColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = fontSizeSelected,
                                onValueChange = {
                                    fontSizeSelected = it
                                    if (hasSelection) {
                                        viewModel.updateSelectedTextProps(textInput, textColorSelected, it)
                                    }
                                },
                                valueRange = 12f..72f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ActivePillColor,
                                    activeTrackColor = ActivePillColor,
                                    inactiveTrackColor = Color.White.copy(0.1f)
                                )
                            )
                        }

                        // Color selection
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            colorsPalette.forEach { (argb, color) ->
                                val rgbSelected = textColorSelected == argb
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            BorderStroke(
                                                2.dp,
                                                if (rgbSelected) ActivePillColor else Color.White.copy(0.15f)
                                            ),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            textColorSelected = argb
                                            if (hasSelection) {
                                                viewModel.updateSelectedTextProps(
                                                    textInput,
                                                    argb,
                                                    fontSizeSelected
                                                )
                                            }
                                        }
                                ) {
                                    if (rgbSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Elegido",
                                            tint = if (color == Color.White || color == Color.Yellow) Color.Black else Color.White,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }

                        // Create trigger
                        if (!hasSelection) {
                            Button(
                                onClick = {
                                    if (textInput.isNotBlank()) {
                                        viewModel.addText(textInput, textColorSelected, fontSizeSelected)
                                        textInput = ""
                                        focusManager.clearFocus()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ActivePillColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("add_text_button")
                            ) {
                                Text("Añadir al Lienzo", color = StudioDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Apply changes & delete buttons side by side
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        viewModel.selectText(null)
                                        textInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioDarkSurfaceCard),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Text("Listo", color = Color.White, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.removeText(selectedId)
                                        textInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Eliminar", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Pre-fill fields on selection changes
                    if (currentSelection != null && textInput != currentSelection.text) {
                        textInput = currentSelection.text
                        textColorSelected = currentSelection.color
                        fontSizeSelected = currentSelection.fontSizeSp
                    }
                }
            }
        }
    }
}
