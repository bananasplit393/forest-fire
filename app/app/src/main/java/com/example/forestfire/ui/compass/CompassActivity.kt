package com.example.forestfire.ui.compass

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.forestfire.data.DutyState
import com.example.forestfire.service.DutyService
import com.example.forestfire.ui.components.openWalkingDirections
import com.example.forestfire.ui.theme.ForestFireTheme
import com.example.forestfire.ui.theme.setupEdgeToEdge
import kotlinx.coroutines.launch

/** Arrow pointing to the fire. Opened from the alarm notification or the main screen. */
class CompassActivity : ComponentActivity() {

    private val viewModel: CompassViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        // The firefighter is walking with the phone in hand; don't let the screen sleep.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // The compass on screen counts as acknowledged, also for an alarm that arrives while it is open.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                DutyState.alarmSounding.collect { sounding -> if (sounding) DutyService.silence(this@CompassActivity) }
            }
        }

        setContent {
            ForestFireTheme {
                CompassScreen(
                    viewModel = viewModel,
                    onEndResponse = {
                        DutyService.endResponse(this)
                        finish()
                    },
                    onBack = { finish() },
                    onOpenMaps = { lat, lon -> openWalkingDirections(lat, lon) },
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, CompassActivity::class.java)
    }
}
