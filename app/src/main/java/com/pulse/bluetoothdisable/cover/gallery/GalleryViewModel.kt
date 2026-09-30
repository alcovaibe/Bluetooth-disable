package com.pulse.bluetoothdisable.cover.gallery

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GalleryUiState(
    val images: List<GalleryImage> = emptyList(),
    val loading: Boolean = true,
    val storageError: Boolean = false,
    val selectedImageId: String? = null,
    val busy: Boolean = false,
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GalleryRepository(application)
    private val access = GalleryAccessManager(application)
    private val detector = GallerySequenceDetector()

    var uiState by mutableStateOf(GalleryUiState())
        private set

    init {
        refresh()
    }

    fun select(image: GalleryImage) {
        detector.reset()
        uiState = uiState.copy(selectedImageId = image.id)
    }

    fun closeViewer() {
        detector.reset()
        uiState = uiState.copy(selectedImageId = null)
    }

    fun resetSequence() {
        detector.reset()
    }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty() || uiState.busy) return
        mutate {
            repository.importUris(getApplication<Application>().contentResolver, uris)
        }
    }

    fun toggleFavorite(image: GalleryImage) = mutate {
        repository.toggleFavorite(image.id)
    }

    fun delete(image: GalleryImage) = mutate {
        repository.delete(image.id)
        if (access.isSecretImage(image.id)) access.clear()
    }

    fun rotate(image: GalleryImage, degrees: Int) = mutate {
        repository.rotate(image.id, degrees)
    }

    fun cropSquare(image: GalleryImage) = mutate {
        repository.cropCenterSquare(image.id)
    }

    fun tapZone(zone: GalleryTapZone, onUnlock: () -> Unit) {
        val imageId = uiState.selectedImageId ?: return
        if (uiState.busy) return
        val complete = detector.tap(zone, SystemClock.elapsedRealtime()) ?: return
        viewModelScope.launch {
            val matches = withContext(Dispatchers.IO) { access.matches(imageId, complete) }
            if (matches) onUnlock()
        }
    }

    suspend fun imageBytes(id: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { repository.imageBytes(id) }.getOrNull()
    }

    suspend fun thumbnailBytes(id: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { repository.thumbnailBytes(id) }.getOrNull()
    }

    fun refresh() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { repository.images() } }
            uiState = uiState.copy(
                images = result.getOrDefault(emptyList()),
                loading = false,
                storageError = result.isFailure,
                selectedImageId = uiState.selectedImageId?.takeIf { id ->
                    result.getOrDefault(emptyList()).any { it.id == id }
                },
            )
        }
    }

    fun resetTransientUi() {
        detector.reset()
        uiState = uiState.copy(selectedImageId = null)
    }

    private fun mutate(action: () -> Unit) {
        if (uiState.busy) return
        uiState = uiState.copy(busy = true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    action()
                    repository.images()
                }
            }
            val images = result.getOrDefault(uiState.images)
            uiState = uiState.copy(
                images = images,
                selectedImageId = uiState.selectedImageId?.takeIf { id -> images.any { it.id == id } },
                busy = false,
                storageError = result.isFailure,
            )
        }
    }
}
