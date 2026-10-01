package com.pulse.bluetoothdisable.cover.gallery

enum class GalleryTapZone {
    TOP_LEFT,
    TOP_RIGHT,
    CENTER,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

data class GalleryImage(
    val id: String,
    val encryptedFileName: String,
    val thumbnailFileName: String,
    val width: Int,
    val height: Int,
    val orientation: Int,
    val colorSpace: String?,
    val importedAt: Long,
    val updatedAt: Long,
    val favorite: Boolean,
    val sha256: String,
    val mimeType: String,
    val source: String,
    val shooting: Map<String, String>,
    val capturedAt: Long? = null,
    val albumIds: List<String> = emptyList(),
)

data class GalleryAlbum(
    val id: String,
    val name: String,
    val createdAt: Long,
)

object GalleryAccessPolicy {
    const val SEQUENCE_LENGTH = 3
    const val TAP_TIMEOUT_MILLIS = 5_000L
    const val UNIQUE_SEQUENCE_COUNT = 60

    fun isValidSequence(sequence: List<GalleryTapZone>): Boolean =
        sequence.size == SEQUENCE_LENGTH && sequence.distinct().size == SEQUENCE_LENGTH
}

/**
 * Collects a three-zone sequence. A repeated zone, timeout, or explicit reset starts a new attempt.
 * The detector does not know the secret sequence; the complete attempt is verified by HMAC elsewhere.
 */
class GallerySequenceDetector(
    private val timeoutMillis: Long = GalleryAccessPolicy.TAP_TIMEOUT_MILLIS,
) {
    private val taps = mutableListOf<GalleryTapZone>()
    private var lastTapAt = 0L

    fun tap(zone: GalleryTapZone, nowMillis: Long): List<GalleryTapZone>? {
        if (lastTapAt != 0L && nowMillis - lastTapAt > timeoutMillis) {
            taps.clear()
        }
        lastTapAt = nowMillis

        if (zone in taps) {
            taps.clear()
            taps += zone
            return null
        }

        taps += zone
        if (taps.size != GalleryAccessPolicy.SEQUENCE_LENGTH) return null

        val complete = taps.toList()
        reset()
        return complete
    }

    fun reset() {
        taps.clear()
        lastTapAt = 0L
    }
}
