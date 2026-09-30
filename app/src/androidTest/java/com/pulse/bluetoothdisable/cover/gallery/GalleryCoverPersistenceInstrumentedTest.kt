package com.pulse.bluetoothdisable.cover.gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pulse.bluetoothdisable.cover.CoverMode
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.cover.calculator.CalculatorAccessCodeManager
import com.pulse.bluetoothdisable.launcher.LauncherIconController
import com.pulse.bluetoothdisable.launcher.LauncherStyle
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryCoverPersistenceInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = CoverModeManager(context)
    private val repository = GalleryRepository(context)
    private val access = GalleryAccessManager(context)

    @Before fun before() {
        manager.resetToDefault()
        repository.images().forEach { repository.delete(it.id) }
        access.clear()
    }

    @After fun after() {
        if (manager.activeMode() == CoverMode.GALLERY) manager.resetGalleryCover() else manager.resetToDefault()
        repository.images().forEach { repository.delete(it.id) }
        access.clear()
    }

    @Test fun importedPhotoIsSanitizedEncryptedAndPersistsAcrossCoverChanges() {
        val source = createJpegWithExif()
        val added = repository.importUris(context.contentResolver, listOf(Uri.fromFile(source)))
        assertEquals(1, added.size)
        val image = added.single()

        assertEquals(96, image.width)
        assertEquals(64, image.height)
        assertEquals(1, image.orientation)
        assertEquals(GalleryRepository.SOURCE_USER_PICKER, image.source)
        assertNotNull("Color information should be retained in Gallery DB", image.colorSpace)
        assertNotNull("Exposure parameters should be retained in Gallery DB", image.shooting["ExposureTime"])
        assertNotNull("ISO should be retained in Gallery DB", image.shooting["PhotographicSensitivity"])

        val sanitized = ExifInterface(ByteArrayInputStream(repository.imageBytes(image.id)))
        for (removed in listOf(
            "Make",
            "Model",
            "DateTime",
            "Software",
            "ImageDescription",
            "XResolution",
            "YResolution",
        )) {
            assertNull("$removed must not survive sanitized local copy", sanitized.getAttribute(removed))
        }
        assertNull(sanitized.getAttribute("GPSLatitude"))
        assertNull(sanitized.getAttribute("GPSLongitude"))

        val firstSequence = listOf(
            GalleryTapZone.TOP_LEFT,
            GalleryTapZone.CENTER,
            GalleryTapZone.BOTTOM_RIGHT,
        )
        manager.activateGallery(image.id, firstSequence)
        assertTrue(manager.isGalleryReady())
        assertTrue(access.matches(image.id, firstSequence))
        assertEquals(LauncherStyle.GALLERY, LauncherIconController(context).selectedStyle())

        manager.activateCalculator("58317")
        assertTrue(manager.isCalculatorReady())
        assertFalse(access.hasRule())
        assertTrue(CalculatorAccessCodeManager(context).verify("58317"))
        assertEquals(image.id, repository.images().single().id)

        val secondSequence = listOf(
            GalleryTapZone.BOTTOM_LEFT,
            GalleryTapZone.TOP_RIGHT,
            GalleryTapZone.CENTER,
        )
        manager.activateGallery(image.id, secondSequence)
        assertTrue(manager.isGalleryReady())
        assertTrue(access.matches(image.id, secondSequence))
        assertEquals(1, repository.images().size)

        repository.toggleFavorite(image.id)
        assertTrue(repository.image(image.id)!!.favorite)
        manager.resetGalleryCover()
        assertEquals(CoverMode.DEFAULT, manager.activeMode())
        assertFalse(access.hasRule())
        assertEquals(1, repository.images().size)
        assertTrue(repository.image(image.id)!!.favorite)
    }

    @Test fun deletingSecretImageInvalidatesAccessWithoutTouchingOtherPhotos() {
        val first = repository.importUris(context.contentResolver, listOf(Uri.fromFile(createPlainJpeg("first.jpg")))).single()
        val second = repository.importUris(context.contentResolver, listOf(Uri.fromFile(createPlainJpeg("second.jpg", Color.BLUE)))).single()
        val sequence = listOf(
            GalleryTapZone.TOP_RIGHT,
            GalleryTapZone.BOTTOM_LEFT,
            GalleryTapZone.CENTER,
        )
        manager.activateGallery(first.id, sequence)
        assertTrue(access.hasRule())

        repository.delete(first.id)
        if (access.isSecretImage(first.id)) access.clear()

        assertFalse(access.hasRule())
        assertEquals(listOf(second.id), repository.images().map { it.id })
        assertTrue(manager.isGalleryReady())
    }

    @Test fun appRequestsNoBroadPhotoLibraryPermission() {
        val requested = context.packageManager
            .getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.toSet()
            .orEmpty()
        assertFalse("android.permission.READ_MEDIA_IMAGES" in requested)
        assertFalse("android.permission.READ_EXTERNAL_STORAGE" in requested)
    }

    private fun createJpegWithExif(): File {
        val file = createPlainJpeg("gallery-source-exif.jpg")
        ExifInterface(file.absolutePath).apply {
            setAttribute("Make", "PrivateCameraMaker")
            setAttribute("Model", "PrivateCameraModel")
            setAttribute("Software", "PrivateEditor")
            setAttribute("DateTime", "2026:09:30 20:15:00")
            setAttribute("ImageDescription", "private description")
            setAttribute("XResolution", "300/1")
            setAttribute("YResolution", "300/1")
            setAttribute("ExposureTime", "1/125")
            setAttribute("FNumber", "18/10")
            setAttribute("PhotographicSensitivity", "200")
            saveAttributes()
        }
        return file
    }

    private fun createPlainJpeg(name: String, color: Int = Color.RED): File {
        val file = File(context.cacheDir, name)
        val bitmap = Bitmap.createBitmap(96, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        FileOutputStream(file).use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output))
        }
        bitmap.recycle()
        return file
    }
}
