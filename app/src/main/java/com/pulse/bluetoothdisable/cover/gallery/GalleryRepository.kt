package com.pulse.bluetoothdisable.cover.gallery

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class GalleryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.noBackupFilesDir, "gallery_v1")
    private val imagesDir = File(root, "images")
    private val thumbsDir = File(root, "thumbs")
    private val indexFile = File(root, "index.enc")
    private val lock = Any()

    init {
        check(imagesDir.mkdirs() || imagesDir.isDirectory) { "Unable to create gallery image directory" }
        check(thumbsDir.mkdirs() || thumbsDir.isDirectory) { "Unable to create gallery thumbnail directory" }
    }

    fun images(): List<GalleryImage> = synchronized(lock) {
        loadIndex().sortedWith(
            compareByDescending<GalleryImage> { it.favorite }
                .thenByDescending { it.importedAt },
        )
    }

    fun image(id: String): GalleryImage? = synchronized(lock) {
        loadIndex().firstOrNull { it.id == id }
    }

    fun imageBytes(id: String): ByteArray = synchronized(lock) {
        val item = loadIndex().firstOrNull { it.id == id } ?: error("Unknown gallery image")
        GalleryCipher.decrypt(File(imagesDir, item.encryptedFileName).readBytes(), "image:${item.id}")
    }

    fun thumbnailBytes(id: String): ByteArray = synchronized(lock) {
        val item = loadIndex().firstOrNull { it.id == id } ?: error("Unknown gallery image")
        GalleryCipher.decrypt(File(thumbsDir, item.thumbnailFileName).readBytes(), "thumb:${item.id}")
    }

    fun importUris(resolver: ContentResolver, uris: List<Uri>): List<GalleryImage> = synchronized(lock) {
        val current = loadIndex().toMutableList()
        val added = mutableListOf<GalleryImage>()
        for (uri in uris) {
            val source = readBounded(resolver, uri)
            val prepared = prepareImage(source)
            val duplicate = current.firstOrNull { it.sha256 == prepared.sha256 }
            if (duplicate != null) {
                prepared.bitmap.recycle()
                continue
            }

            val id = UUID.randomUUID().toString()
            val imageFileName = "$id.bin"
            val thumbFileName = "$id.bin"
            File(imagesDir, imageFileName).writeBytes(
                GalleryCipher.encrypt(prepared.encoded, "image:$id"),
            )
            File(thumbsDir, thumbFileName).writeBytes(
                GalleryCipher.encrypt(makeThumbnail(prepared.bitmap), "thumb:$id"),
            )

            val now = System.currentTimeMillis()
            val item = GalleryImage(
                id = id,
                encryptedFileName = imageFileName,
                thumbnailFileName = thumbFileName,
                width = prepared.bitmap.width,
                height = prepared.bitmap.height,
                orientation = 1,
                colorSpace = prepared.bitmap.colorSpace?.name,
                importedAt = now,
                updatedAt = now,
                favorite = false,
                sha256 = prepared.sha256,
                mimeType = prepared.mimeType,
                source = SOURCE_USER_PICKER,
                shooting = prepared.shooting,
            )
            prepared.bitmap.recycle()
            current += item
            added += item
        }
        saveIndex(current)
        added
    }

    fun toggleFavorite(id: String): GalleryImage? = synchronized(lock) {
        val current = loadIndex().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return@synchronized null
        val changed = current[index].copy(
            favorite = !current[index].favorite,
            updatedAt = System.currentTimeMillis(),
        )
        current[index] = changed
        saveIndex(current)
        changed
    }

    fun delete(id: String): Boolean = synchronized(lock) {
        val current = loadIndex().toMutableList()
        val item = current.firstOrNull { it.id == id } ?: return@synchronized false
        current.removeAll { it.id == id }
        saveIndex(current)
        File(imagesDir, item.encryptedFileName).delete()
        File(thumbsDir, item.thumbnailFileName).delete()
        true
    }

    fun rotate(id: String, degrees: Int): GalleryImage = synchronized(lock) {
        require(degrees % 90 == 0)
        edit(id) { source ->
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }
    }

    fun cropCenterSquare(id: String): GalleryImage = synchronized(lock) {
        edit(id) { source ->
            val size = minOf(source.width, source.height)
            val left = (source.width - size) / 2
            val top = (source.height - size) / 2
            Bitmap.createBitmap(source, left, top, size, size)
        }
    }

    private fun edit(id: String, transform: (Bitmap) -> Bitmap): GalleryImage {
        val current = loadIndex().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        require(index >= 0) { "Unknown gallery image" }
        val old = current[index]
        val originalBytes = GalleryCipher.decrypt(
            File(imagesDir, old.encryptedFileName).readBytes(),
            "image:${old.id}",
        )
        val source = checkNotNull(BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size))
        val edited = transform(source)
        if (edited !== source) source.recycle()

        val encoded = encodeSanitized(edited)
        val thumb = makeThumbnail(edited)
        File(imagesDir, old.encryptedFileName).writeBytes(
            GalleryCipher.encrypt(encoded.bytes, "image:${old.id}"),
        )
        File(thumbsDir, old.thumbnailFileName).writeBytes(
            GalleryCipher.encrypt(thumb, "thumb:${old.id}"),
        )
        val changed = old.copy(
            width = edited.width,
            height = edited.height,
            orientation = 1,
            colorSpace = edited.colorSpace?.name ?: old.colorSpace,
            updatedAt = System.currentTimeMillis(),
            sha256 = sha256(encoded.bytes),
            mimeType = encoded.mimeType,
        )
        edited.recycle()
        current[index] = changed
        saveIndex(current)
        return changed
    }

    private data class PreparedImage(
        val bitmap: Bitmap,
        val encoded: ByteArray,
        val sha256: String,
        val mimeType: String,
        val shooting: Map<String, String>,
    )

    private data class EncodedImage(val bytes: ByteArray, val mimeType: String)

    private fun prepareImage(source: ByteArray): PreparedImage {
        val exif = runCatching { ExifInterface(ByteArrayInputStream(source)) }.getOrNull()
        val orientation = exif?.getAttributeInt("Orientation", 1) ?: 1
        val shooting = readShootingMetadata(exif)

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image" }
        var sample = 1
        while ((bounds.outWidth.toLong() / sample) * (bounds.outHeight.toLong() / sample) > MAX_DECODE_PIXELS) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = checkNotNull(BitmapFactory.decodeByteArray(source, 0, source.size, options)) {
            "Unable to decode image"
        }
        val normalized = normalizeOrientation(decoded, orientation)
        if (normalized !== decoded) decoded.recycle()
        val encoded = encodeSanitized(normalized)
        return PreparedImage(
            bitmap = normalized,
            encoded = encoded.bytes,
            sha256 = sha256(encoded.bytes),
            mimeType = encoded.mimeType,
            shooting = shooting,
        )
    }

    private fun normalizeOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            2 -> matrix.setScale(-1f, 1f)
            3 -> matrix.setRotate(180f)
            4 -> matrix.setScale(1f, -1f)
            5 -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            6 -> matrix.setRotate(90f)
            7 -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            8 -> matrix.setRotate(270f)
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /** Re-encoding is intentional: source EXIF/XMP/IPTC/MakerNote/print blocks are not copied. */
    private fun encodeSanitized(bitmap: Bitmap): EncodedImage {
        val output = ByteArrayOutputStream()
        val alpha = bitmap.hasAlpha()
        val format = if (alpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        check(bitmap.compress(format, if (alpha) 100 else 96, output)) { "Unable to encode image" }
        return EncodedImage(
            bytes = output.toByteArray(),
            mimeType = if (alpha) "image/png" else "image/jpeg",
        )
    }

    private fun makeThumbnail(bitmap: Bitmap): ByteArray {
        val scale = minOf(1f, THUMBNAIL_SIZE.toFloat() / maxOf(bitmap.width, bitmap.height))
        val width = maxOf(1, (bitmap.width * scale).toInt())
        val height = maxOf(1, (bitmap.height * scale).toInt())
        val thumb = if (width == bitmap.width && height == bitmap.height) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        }
        val output = ByteArrayOutputStream()
        check(thumb.compress(Bitmap.CompressFormat.JPEG, 88, output)) { "Unable to encode thumbnail" }
        if (thumb !== bitmap) thumb.recycle()
        return output.toByteArray()
    }

    private fun readShootingMetadata(exif: ExifInterface?): Map<String, String> {
        if (exif == null) return emptyMap()
        val result = linkedMapOf<String, String>()
        SHOOTING_TAGS.forEach { tag ->
            exif.getAttribute(tag)?.takeIf { it.isNotBlank() }?.let { result[tag] = it }
        }
        if (result["PhotographicSensitivity"] == null) {
            exif.getAttribute("ISOSpeedRatings")?.takeIf { it.isNotBlank() }?.let {
                result["PhotographicSensitivity"] = it
            }
        }
        return result
    }

    private fun readBounded(resolver: ContentResolver, uri: Uri): ByteArray {
        val stream = checkNotNull(resolver.openInputStream(uri)) { "Unable to open selected image" }
        stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(32 * 1024)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_INPUT_BYTES) { "Selected image is too large" }
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }
    }

    private fun loadIndex(): List<GalleryImage> {
        if (!indexFile.exists()) return emptyList()
        val json = String(
            GalleryCipher.decrypt(indexFile.readBytes(), INDEX_PURPOSE),
            Charsets.UTF_8,
        )
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) add(fromJson(array.getJSONObject(i)))
        }
    }

    private fun saveIndex(items: List<GalleryImage>) {
        val array = JSONArray()
        items.forEach { array.put(toJson(it)) }
        val encrypted = GalleryCipher.encrypt(array.toString().toByteArray(Charsets.UTF_8), INDEX_PURPOSE)
        val temp = File(root, "index.tmp")
        temp.writeBytes(encrypted)
        if (indexFile.exists()) check(indexFile.delete()) { "Unable to replace gallery index" }
        check(temp.renameTo(indexFile)) { "Unable to commit gallery index" }
    }

    private fun toJson(item: GalleryImage) = JSONObject().apply {
        put("id", item.id)
        put("file", item.encryptedFileName)
        put("thumb", item.thumbnailFileName)
        put("width", item.width)
        put("height", item.height)
        put("orientation", item.orientation)
        put("colorSpace", item.colorSpace ?: JSONObject.NULL)
        put("importedAt", item.importedAt)
        put("updatedAt", item.updatedAt)
        put("favorite", item.favorite)
        put("sha256", item.sha256)
        put("mimeType", item.mimeType)
        put("source", item.source)
        put("shooting", JSONObject(item.shooting))
    }

    private fun fromJson(json: JSONObject): GalleryImage {
        val shootingJson = json.optJSONObject("shooting") ?: JSONObject()
        val shooting = linkedMapOf<String, String>()
        val keys = shootingJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            shooting[key] = shootingJson.getString(key)
        }
        return GalleryImage(
            id = json.getString("id"),
            encryptedFileName = json.getString("file"),
            thumbnailFileName = json.getString("thumb"),
            width = json.getInt("width"),
            height = json.getInt("height"),
            orientation = json.optInt("orientation", 1),
            colorSpace = json.optString("colorSpace").takeIf { it.isNotBlank() && it != "null" },
            importedAt = json.getLong("importedAt"),
            updatedAt = json.getLong("updatedAt"),
            favorite = json.optBoolean("favorite", false),
            sha256 = json.getString("sha256"),
            mimeType = json.optString("mimeType", "image/jpeg"),
            source = json.optString("source", SOURCE_USER_PICKER),
            shooting = shooting,
        )
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        const val SOURCE_USER_PICKER = "USER_PICKER"
        private const val INDEX_PURPOSE = "index"
        private const val MAX_INPUT_BYTES = 64 * 1024 * 1024
        private const val MAX_DECODE_PIXELS = 24_000_000L
        private const val THUMBNAIL_SIZE = 512

        private val SHOOTING_TAGS = listOf(
            "ExposureTime",
            "FNumber",
            "ApertureValue",
            "PhotographicSensitivity",
            "FocalLength",
            "Flash",
            "ExposureBiasValue",
            "ExposureMode",
            "ExposureProgram",
            "MeteringMode",
            "WhiteBalance",
            "LightSource",
            "SceneCaptureType",
            "DigitalZoomRatio",
            "FocalLengthIn35mmFilm",
            "Contrast",
            "Saturation",
            "Sharpness",
        )
    }
}
