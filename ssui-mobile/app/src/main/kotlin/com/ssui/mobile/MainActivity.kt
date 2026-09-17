package com.ssui.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ssui.mobile.ui.screen.ScreenRoute
import com.ssui.mobile.ui.theme.SsuiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SsuiTheme {
                ScreenRoute()
            }
        }
    }
}
