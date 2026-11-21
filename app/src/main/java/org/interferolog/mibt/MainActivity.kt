package org.interferolog.mibt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import org.interferolog.mibt.navigation.MibtNav
import org.interferolog.mibt.ui.theme.MibtTheme

class MainActivity : ComponentActivity() {
    private var openToken by mutableIntStateOf(0)
    private var openTarget by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        accept(intent)
        setContent {
            MibtTheme {
                MibtNav(
                    viewModel = viewModel(),
                    openToken = openToken,
                    openTarget = openTarget,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        accept(intent)
    }

    private fun accept(intent: Intent?) {
        val target = intent?.getStringExtra("open") ?: return
        openTarget = target
        openToken += 1
    }
}
