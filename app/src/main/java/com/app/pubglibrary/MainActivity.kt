package com.app.pubglibrary

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.app.pubglibrary.ui.DropZoneViewModel
import com.app.pubglibrary.ui.MainScreen
import com.app.pubglibrary.ui.theme.DropZoneTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DropZoneViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            DropZoneTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
