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
    val albums: List<GalleryAlbum> = emptyList(),
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

    fun createAlbum(name: String, image: GalleryImage? = null) = mutate {
        val album = repository.createAlbum(name)
        if (image != null) repository.addToAlbum(image.id, album.id)
    }

    fun addToAlbum(image: GalleryImage, album: GalleryAlbum) = mutate {
        repository.addToAlbum(image.id, album.id)
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
            val result = withContext(Dispatchers.IO) {
                runCatching { repository.images() to repository.albums() }
            }
            val currentImages = result.getOrNull()?.first ?: emptyList()
            val currentAlbums = result.getOrNull()?.second ?: emptyList()
            uiState = uiState.copy(
                images = currentImages,
                albums = currentAlbums,
                loading = false,
                storageError = result.isFailure,
                selectedImageId = uiState.selectedImageId?.takeIf { id ->
                    currentImages.any { it.id == id }
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
                    repository.images() to repository.albums()
                }
            }
            val images = result.getOrNull()?.first ?: uiState.images
            val albums = result.getOrNull()?.second ?: uiState.albums
            uiState = uiState.copy(
                images = images,
                albums = albums,
                selectedImageId = uiState.selectedImageId?.takeIf { id -> images.any { it.id == id } },
                busy = false,
                storageError = result.isFailure,
            )
        }
    }
}
