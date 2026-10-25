package com.example.forestfire.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.forestfire.ui.components.openWalkingDirections
import com.example.forestfire.ui.compass.CompassActivity
import com.example.forestfire.ui.settings.SettingsActivity
import com.example.forestfire.ui.theme.ForestFireTheme
import com.example.forestfire.ui.theme.setupEdgeToEdge

/** Duty toggle, connection status and the current alarm. Permission and intent plumbing only. */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onPermissionsResult() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()

        setContent {
            ForestFireTheme {
                MainScreen(
                    viewModel = viewModel,
                    onToggleDuty = {
                        if (viewModel.onDuty.value) viewModel.goOffDuty() else requestPermissionsAndGoOnDuty()
                    },
                    onOpenCompass = { startActivity(CompassActivity.newIntent(this)) },
                    onOpenSettings = { startActivity(SettingsActivity.newIntent(this)) },
                    onOpenAppSettings = {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
                        )
                    },
                    onOpenMaps = { lat, lon -> openWalkingDirections(lat, lon) },
                )
            }
        }
    }

    private fun requestPermissionsAndGoOnDuty() {
        val missing = requiredPermissions.filter { !isGranted(it) }
        if (missing.isEmpty()) viewModel.goOnDuty() else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun onPermissionsResult() {
        // Re-check rather than trusting the result map: "approximate only" leaves FINE denied.
        val allGranted = requiredPermissions.all(::isGranted)
        viewModel.permissionsDenied = !allGranted
        if (allGranted) viewModel.goOnDuty()
    }

    private fun isGranted(permission: String) =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private val requiredPermissions: List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }
}
