package com.pulse.bluetoothdisable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.pulse.bluetoothdisable.ui.MainScreen
import com.pulse.bluetoothdisable.ui.MainViewModel
import com.pulse.bluetoothdisable.ui.theme.BluetoothDisableTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            BluetoothDisableTheme {
                MainScreen(
                    uiState = uiState,
                    onEnableProtection = viewModel::enableProtection,
                    onDisableProtection = viewModel::disableProtection,
                    onRefresh = viewModel::refresh,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refresh()
        }
    }
}
