package com.pulse.bluetoothdisable.transfer

import android.content.Context
import android.provider.Settings
import com.pulse.bluetoothdisable.launcher.LauncherIconController
import java.io.File
import java.security.MessageDigest

/**
 * Last-resort protection against OEM migration tools that copy app-private data
 * even when Android backup / D2D rules exclude it.
 *
 * The marker contains only a SHA-256 hash of the app-scoped ANDROID_ID. If copied
 * to another device, the hash changes and all local Bluetooth Disabler state is
 * reset before the rest of the app reads it.
 */
object DeviceTransferGuard {
    private const val IDENTITY_PREFERENCES = "device_identity_preferences"
    private const val KEY_DEVICE_ID_HASH = "device_id_hash"

    private val knownPreferences = setOf(
        IDENTITY_PREFERENCES,
        "theme_preferences",
        "language_preferences",
        "quick_settings_tile_state",
    )

    fun enforce(context: Context) {
        val currentHash = currentDeviceHash(context) ?: return
        val identityPreferences = context.getSharedPreferences(
            IDENTITY_PREFERENCES,
            Context.MODE_PRIVATE,
        )
        val storedHash = identityPreferences.getString(KEY_DEVICE_ID_HASH, null)

        if (storedHash == null) {
            identityPreferences.edit()
                .putString(KEY_DEVICE_ID_HASH, currentHash)
                .commit()
            return
        }

        if (storedHash == currentHash) return

        clearMigratedLocalState(context)

        context.getSharedPreferences(IDENTITY_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DEVICE_ID_HASH, currentHash)
            .commit()
    }

    private fun currentDeviceHash(context: Context): String? {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ) ?: return null

        return MessageDigest.getInstance("SHA-256")
            .digest(androidId.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun clearMigratedLocalState(context: Context) {
        clearAllSharedPreferences(context)

        context.databaseList().forEach { databaseName ->
            context.deleteDatabase(databaseName)
        }

        clearDirectoryContents(context.filesDir)
        clearDirectoryContents(context.noBackupFilesDir)
        clearDirectoryContents(context.cacheDir)
        clearDirectoryContents(context.codeCacheDir)

        context.getExternalFilesDirs(null)
            .filterNotNull()
            .forEach(::clearDirectoryContents)
        context.externalCacheDirs
            .filterNotNull()
            .forEach(::clearDirectoryContents)

        clearDirectDataFiles(context)

        // Component enabled state is managed by PackageManager rather than app files.
        // Restore the launcher to the clean-install state as well.
        LauncherIconController(context).show()
    }

    private fun clearAllSharedPreferences(context: Context) {
        val preferencesDirectory = File(context.applicationInfo.dataDir, "shared_prefs")
        val preferenceNames = preferencesDirectory.listFiles()
            ?.asSequence()
            ?.filter { file -> file.isFile && file.name.endsWith(".xml") }
            ?.map { file -> file.name.removeSuffix(".xml") }
            ?.toMutableSet()
            ?: mutableSetOf()

        preferenceNames += knownPreferences

        preferenceNames.forEach { preferenceName ->
            context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
    }

    private fun clearDirectoryContents(directory: File?) {
        directory?.listFiles()?.forEach { child ->
            child.deleteRecursively()
        }
    }

    private fun clearDirectDataFiles(context: Context) {
        File(context.applicationInfo.dataDir)
            .listFiles()
            ?.filter { child -> child.isFile }
            ?.forEach { child -> child.delete() }
    }
}
