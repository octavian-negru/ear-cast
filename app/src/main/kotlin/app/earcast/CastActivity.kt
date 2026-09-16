package app.earcast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.earcast.ui.EarCastApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CastActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EarCastApp()
        }
    }
}
