package com.pulse.bluetoothdisable.cover.calendar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.pulse.bluetoothdisable.MainActivity
import com.pulse.bluetoothdisable.cover.CoverMode
import com.pulse.bluetoothdisable.cover.CoverModeManager
import com.pulse.bluetoothdisable.cover.CoverModeNavigator
import com.pulse.bluetoothdisable.localization.LanguageManager

class CalendarCoverActivity : ComponentActivity() {
    private lateinit var viewModel: CalendarViewModel

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!CoverModeManager(this).isCalendarReady()) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            finish()
            return
        }
        enableEdgeToEdge()
        viewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
        setContent {
            CalendarCoverTheme(this) {
                CalendarScreen(viewModel) {
                    CoverModeNavigator.openMainFromCover(this, CoverMode.CALENDAR)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::viewModel.isInitialized) {
            viewModel.closeEditor()
            viewModel.today()
            viewModel.refresh()
        }
    }
}
