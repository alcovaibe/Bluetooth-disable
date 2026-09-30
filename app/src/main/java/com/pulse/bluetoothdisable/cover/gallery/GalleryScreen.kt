package com.pulse.bluetoothdisable.cover.gallery

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pulse.bluetoothdisable.R
import com.pulse.bluetoothdisable.cover.coverRecoveryHold
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    recoveryEnabled: Boolean,
    onRecoveryHold: () -> Unit,
    onUnlock: () -> Unit,
    onAddPhotos: () -> Unit,
) {
    val state = viewModel.uiState
    val selected = state.selectedImageId?.let { id -> state.images.firstOrNull { it.id == id } }

    if (selected != null) {
        GalleryViewer(
            image = selected,
            images = state.images,
            viewModel = viewModel,
            recoveryEnabled = recoveryEnabled,
            onRecoveryHold = onRecoveryHold,
            onUnlock = onUnlock,
            onBack = viewModel::closeViewer,
        )
        return
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.launcher_name_gallery),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .weight(1f)
                        .coverRecoveryHold(recoveryEnabled, onRecoveryHold),
                )
                TextButton(onClick = onAddPhotos, enabled = !state.busy) {
                    Text(stringResource(R.string.gallery_add_photos))
                }
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.storageError -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.gallery_storage_error), color = MaterialTheme.colorScheme.error)
                }
                state.images.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.gallery_empty))
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onAddPhotos) { Text(stringResource(R.string.gallery_add_photos)) }
                    }
                }
                else -> GalleryGrid(state.images, viewModel)
            }
        }
    }
}

@Composable
private fun GalleryGrid(images: List<GalleryImage>, viewModel: GalleryViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(images.chunked(3), key = { row -> row.joinToString("|") { it.id } }) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(3) { column ->
                    val image = row.getOrNull(column)
                    if (image == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(132.dp)
                                .clickable { viewModel.select(image) },
                        ) {
                            GalleryStoredImage(
                                id = image.id,
                                thumbnail = true,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (image.favorite) {
                                Text(
                                    text = "♥",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                            MaterialTheme.shapes.small,
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryViewer(
    image: GalleryImage,
    images: List<GalleryImage>,
    viewModel: GalleryViewModel,
    recoveryEnabled: Boolean,
    onRecoveryHold: () -> Unit,
    onUnlock: () -> Unit,
    onBack: () -> Unit,
) {
    var showInfo by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var scale by remember(image.id) { mutableFloatStateOf(1f) }
    var translation by remember(image.id) { mutableStateOf(Offset.Zero) }
    var dragDistance by remember(image.id) { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val bitmap by galleryBitmap(viewModel, image.id, thumbnail = false)

    fun move(delta: Int) {
        val index = images.indexOfFirst { it.id == image.id }
        if (index < 0) return
        val target = (index + delta).coerceIn(0, images.lastIndex)
        if (target != index) viewModel.select(images[target])
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("‹") }
                Text(
                    stringResource(R.string.launcher_name_gallery),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .coverRecoveryHold(recoveryEnabled, onRecoveryHold),
                )
                Text("${images.indexOfFirst { it.id == image.id } + 1}/${images.size}")
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .onSizeChanged { containerSize = it }
                    .draggable(
                        state = rememberDraggableState { delta -> dragDistance += delta },
                        orientation = Orientation.Horizontal,
                        enabled = scale <= 1.01f,
                        onDragStarted = {
                            dragDistance = 0f
                            viewModel.resetSequence()
                        },
                        onDragStopped = {
                            if (abs(dragDistance) > 120f) move(if (dragDistance < 0f) 1 else -1)
                            dragDistance = 0f
                        },
                    )
                    .pointerInputTransform(
                        imageId = image.id,
                        scale = scale,
                        translation = translation,
                        onTransform = { nextScale, nextTranslation ->
                            scale = nextScale
                            translation = nextTranslation
                            viewModel.resetSequence()
                        },
                    )
                    .pointerInputTaps(
                        imageId = image.id,
                        bitmap = bitmap,
                        containerSize = containerSize,
                        scale = scale,
                        translation = translation,
                        onDoubleTap = {
                            scale = if (scale > 1.01f) 1f else 2f
                            translation = Offset.Zero
                            viewModel.resetSequence()
                        },
                        onZone = { zone ->
                            if (zone == null) viewModel.resetSequence()
                            else viewModel.tapZone(zone, onUnlock)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = translation.x
                                translationY = translation.y
                            },
                    )
                } ?: CircularProgressIndicator()
            }

            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GalleryToolbarAction(if (image.favorite) "♥" else "♡", R.string.gallery_favorite) {
                        viewModel.toggleFavorite(image)
                    }
                    GalleryToolbarAction("✎", R.string.gallery_edit) { showEdit = true }
                    GalleryToolbarAction("ⓘ", R.string.gallery_info) { showInfo = true }
                    GalleryToolbarAction("⌫", R.string.gallery_delete) { showDelete = true }
                }
            }
        }
    }

    if (showInfo) {
        GalleryInfoDialog(image = image, onDismiss = { showInfo = false })
    }
    if (showEdit) {
        GalleryEditDialog(
            onRotateLeft = {
                showEdit = false
                viewModel.rotate(image, -90)
            },
            onRotateRight = {
                showEdit = false
                viewModel.rotate(image, 90)
            },
            onCropSquare = {
                showEdit = false
                viewModel.cropSquare(image)
            },
            onDismiss = { showEdit = false },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.gallery_delete_title)) },
            text = { Text(stringResource(R.string.gallery_delete_message)) },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.delete(image)
                }) { Text(stringResource(R.string.gallery_delete)) }
            },
        )
    }
}

@Composable
private fun GalleryToolbarAction(symbol: String, labelRes: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(symbol, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun GalleryInfoDialog(image: GalleryImage, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gallery_info)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Text("${image.width} × ${image.height}") }
                image.colorSpace?.let { color -> item { Text("Color space: $color") } }
                image.shooting.forEach { (key, value) -> item { Text("$key: $value") } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.gallery_close)) } },
    )
}

@Composable
private fun GalleryEditDialog(
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onCropSquare: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gallery_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRotateLeft, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.gallery_rotate_left))
                }
                Button(onClick = onRotateRight, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.gallery_rotate_right))
                }
                Button(onClick = onCropSquare, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.gallery_crop_square))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun GalleryStoredImage(
    id: String,
    thumbnail: Boolean,
    viewModel: GalleryViewModel,
    modifier: Modifier,
) {
    val bitmap by galleryBitmap(viewModel, id, thumbnail)
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun galleryBitmap(
    viewModel: GalleryViewModel,
    id: String,
    thumbnail: Boolean,
) = produceState<ImageBitmap?>(initialValue = null, id, thumbnail, viewModel.uiState.images) {
    val bytes = if (thumbnail) viewModel.thumbnailBytes(id) else viewModel.imageBytes(id)
    value = bytes?.let { data ->
        withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
        }
    }
}

private fun Modifier.pointerInputTransform(
    imageId: String,
    scale: Float,
    translation: Offset,
    onTransform: (Float, Offset) -> Unit,
): Modifier = androidx.compose.ui.input.pointer.pointerInput(imageId) {
    detectTransformGestures { _, pan, zoom, _ ->
        val nextScale = (scale * zoom).coerceIn(1f, 5f)
        val nextTranslation = if (nextScale <= 1.01f) Offset.Zero else translation + pan
        if (zoom != 1f || pan != Offset.Zero) onTransform(nextScale, nextTranslation)
    }
}

private fun Modifier.pointerInputTaps(
    imageId: String,
    bitmap: ImageBitmap?,
    containerSize: IntSize,
    scale: Float,
    translation: Offset,
    onDoubleTap: () -> Unit,
    onZone: (GalleryTapZone?) -> Unit,
): Modifier = androidx.compose.ui.input.pointer.pointerInput(
    imageId,
    bitmap,
    containerSize,
    scale,
    translation,
) {
    detectTapGestures(
        onDoubleTap = { onDoubleTap() },
        onTap = { tap ->
            if (scale > 1.01f || translation.getDistance() > 2f) {
                onZone(null)
                return@detectTapGestures
            }
            onZone(zoneForTap(tap, containerSize, bitmap))
        },
    )
}

private fun zoneForTap(
    tap: Offset,
    container: IntSize,
    bitmap: ImageBitmap?,
): GalleryTapZone? {
    if (bitmap == null || container.width <= 0 || container.height <= 0) return null
    val fit = minOf(
        container.width.toFloat() / bitmap.width,
        container.height.toFloat() / bitmap.height,
    )
    val renderedWidth = bitmap.width * fit
    val renderedHeight = bitmap.height * fit
    val left = (container.width - renderedWidth) / 2f
    val top = (container.height - renderedHeight) / 2f
    if (tap.x !in left..(left + renderedWidth) || tap.y !in top..(top + renderedHeight)) return null

    val x = (tap.x - left) / renderedWidth
    val y = (tap.y - top) / renderedHeight
    return when {
        x <= 0.30f && y <= 0.30f -> GalleryTapZone.TOP_LEFT
        x >= 0.70f && y <= 0.30f -> GalleryTapZone.TOP_RIGHT
        x in 0.35f..0.65f && y in 0.35f..0.65f -> GalleryTapZone.CENTER
        x <= 0.30f && y >= 0.70f -> GalleryTapZone.BOTTOM_LEFT
        x >= 0.70f && y >= 0.70f -> GalleryTapZone.BOTTOM_RIGHT
        else -> null
    }
}
