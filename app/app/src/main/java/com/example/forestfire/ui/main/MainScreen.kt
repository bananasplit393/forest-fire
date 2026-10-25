package com.example.forestfire.ui.main

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.forestfire.data.ConnectionState
import com.example.forestfire.data.model.FireAlert
import com.example.forestfire.ui.components.AlarmCard
import com.example.forestfire.ui.components.DutyButton
import com.example.forestfire.ui.components.ReflectiveStripe
import com.example.forestfire.ui.components.StatusChip
import com.example.forestfire.ui.format.StripeState
import com.example.forestfire.ui.format.stripeStateFor
import com.example.forestfire.ui.theme.ForestFireTheme
import com.example.forestfire.ui.theme.LocalReducedMotion
import com.example.forestfire.ui.theme.Motion

internal data class MainUiState(
    val onDuty: Boolean,
    val connection: ConnectionState,
    val zone: String?,
    val alert: FireAlert?,
    val permissionsDenied: Boolean,
    val alertDirection: String?,
    val alarmSounding: Boolean = false,
)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onToggleDuty: () -> Unit,
    onOpenCompass: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenMaps: (Double, Double) -> Unit,
) {
    val onDuty by viewModel.onDuty.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val zone by viewModel.zone.collectAsStateWithLifecycle()
    val alert by viewModel.currentAlert.collectAsStateWithLifecycle()
    val alertDirection by viewModel.alertDirection.collectAsStateWithLifecycle()
    val alarmSounding by viewModel.alarmSounding.collectAsStateWithLifecycle()
    MainContent(
        state = MainUiState(onDuty, connection, zone, alert, viewModel.permissionsDenied, alertDirection, alarmSounding = alarmSounding),
        onToggleDuty = onToggleDuty,
        onOpenCompass = onOpenCompass,
        onOpenSettings = onOpenSettings,
        onOpenAppSettings = onOpenAppSettings,
        onSilence = viewModel::silence,
        onOpenMaps = onOpenMaps,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainContent(
    state: MainUiState,
    onToggleDuty: () -> Unit,
    onOpenCompass: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onSilence: () -> Unit,
    onOpenMaps: (Double, Double) -> Unit,
) {
    val stripeState = stripeStateFor(state.onDuty, state.connection)
    // Glint only on real transitions: alarm raised or going on duty.
    val alarmActive = state.alert != null
    var glintTrigger by remember { mutableIntStateOf(0) }
    var wasAlarm by remember { mutableStateOf(alarmActive) }
    var wasOnDuty by remember { mutableStateOf(state.onDuty) }
    LaunchedEffect(alarmActive, state.onDuty) {
        if ((alarmActive && !wasAlarm) || (state.onDuty && !wasOnDuty)) glintTrigger++
        wasAlarm = alarmActive
        wasOnDuty = state.onDuty
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Forest Fire") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ReflectiveStripe(
                state = stripeState,
                glintKey = glintTrigger.takeIf { it > 0 },
                stateDescription = stripeDescription(stripeState),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatusBlock(state, stripeState)
                AlarmArea(state, stripeState, onOpenCompass, onSilence, onOpenMaps)
                if (state.permissionsDenied) PermissionNotice(onOpenAppSettings)
            }
            DutyButton(
                onDuty = state.onDuty,
                onClick = onToggleDuty,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

private fun stripeDescription(state: StripeState): String = when (state) {
    StripeState.Lit -> "On duty, connected"
    StripeState.Pending -> "On duty, connecting"
    StripeState.Fault -> "On duty, connection error"
    StripeState.Off -> "Off duty"
}

@Composable
private fun StatusBlock(state: MainUiState, stripeState: StripeState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (state.onDuty) "On duty" else "Off duty",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.semantics { heading() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.zone == null) {
                ZoneSkeleton()
            } else {
                StatusChip(state.zone)
            }
            val stripe = ForestFireTheme.stripe
            StatusChip(
                text = if (state.onDuty) state.connection.label else "Offline",
                dotColor = when (stripeState) {
                    StripeState.Lit -> stripe.lime
                    StripeState.Pending -> stripe.amber
                    StripeState.Fault -> MaterialTheme.colorScheme.error
                    StripeState.Off -> stripe.dull
                },
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun ZoneSkeleton() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(Modifier.width(72.dp).height(36.dp))
    }
}

@Composable
private fun AlarmArea(
    state: MainUiState,
    stripeState: StripeState,
    onOpenCompass: () -> Unit,
    onSilence: () -> Unit,
    onOpenMaps: (Double, Double) -> Unit,
) {
    val reduced = LocalReducedMotion.current
    AnimatedContent(
        targetState = state.alert,
        contentKey = { it != null },
        transitionSpec = {
            if (reduced) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                (fadeIn(tween(Motion.MEDIUM)) +
                    slideInVertically(tween(Motion.MEDIUM, easing = Motion.EmphasizedDecelerate)) { it / 4 }) togetherWith
                    fadeOut(tween(Motion.SHORT))
            }
        },
        label = "alarm",
    ) { alert ->
        when {
            alert != null -> AlarmCard(
                alert = alert,
                onNavigate = onOpenCompass,
                stripeState = stripeState,
                directionText = state.alertDirection,
                sounding = state.alarmSounding,
                onSilence = onSilence,
                onOpenMaps = { onOpenMaps(alert.lat, alert.lon) },
            )
            state.onDuty -> Text(
                "No active alarm in ${state.zone ?: "your zone"}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> Spacer(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PermissionNotice(onOpenAppSettings: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                "Precise location and notifications are needed to go on duty.",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onOpenAppSettings) { Text("Open settings") }
        }
    }
}

private val PreviewAlarm = MainUiState(
    onDuty = true,
    connection = ConnectionState.Connected,
    zone = "zone1",
    alert = FireAlert(risk = 0.91, lat = 56.172, lon = 10.19, ts = 1760870460),
    permissionsDenied = false,
    alertDirection = "1.3 km · NE",
    alarmSounding = true,
)

@Preview(name = "Alarm · day", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(
    name = "Alarm · night",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun MainAlarmPreview() {
    ForestFireTheme { MainContent(PreviewAlarm, {}, {}, {}, {}, {}, { _, _ -> }) }
}

@Preview(name = "Off duty · day", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MainOffDutyPreview() {
    ForestFireTheme {
        MainContent(
            PreviewAlarm.copy(onDuty = false, connection = ConnectionState.Disconnected, alert = null),
            {}, {}, {}, {}, {}, { _, _ -> },
        )
    }
}
