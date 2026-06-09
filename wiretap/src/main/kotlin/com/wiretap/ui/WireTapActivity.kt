package com.wiretap.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wiretap.ui.theme.WireTapTheme

class WireTapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WireTapTheme {
                WireTapNavHost()
            }
        }
    }
}
