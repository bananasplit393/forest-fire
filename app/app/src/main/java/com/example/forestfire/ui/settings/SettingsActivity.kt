package com.example.forestfire.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.forestfire.ui.theme.ForestFireTheme
import com.example.forestfire.ui.theme.setupEdgeToEdge

/** Zone, firefighter ID and broker address. */
class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()

        setContent {
            ForestFireTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onSaved = { finish() },
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, SettingsActivity::class.java)
    }
}
