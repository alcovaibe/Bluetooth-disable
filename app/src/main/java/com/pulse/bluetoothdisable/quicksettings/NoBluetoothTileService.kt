package com.pulse.bluetoothdisable.quicksettings

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.pulse.bluetoothdisable.MainActivity

class NoBluetoothTileService : TileService() {
    override fun onTileAdded() {
        super.onTileAdded()
        TileStateStore.setAdded(this, true)
    }

    override fun onStartListening() {
        super.onStartListening()
        // Also heals stale state after app updates or process recreation when the tile
        // already existed before TileStateStore was introduced.
        TileStateStore.setAdded(this, true)
    }

    override fun onTileRemoved() {
        TileStateStore.setAdded(this, false)
        super.onTileRemoved()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        unlockAndRun {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                startActivityAndCollapse(launchIntent)
            }
        }
    }
}
