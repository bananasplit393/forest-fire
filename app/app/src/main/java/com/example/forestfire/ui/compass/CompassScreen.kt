package com.example.forestfire.ui.compass

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.forestfire.data.model.FireAlert
import com.example.forestfire.ui.components.FireChevron
import com.example.forestfire.ui.components.ReflectiveStripe
import com.example.forestfire.ui.theme.ForestFireTheme
import com.example.forestfire.ui.theme.tabular
import java.util.Locale

@Composable
fun CompassScreen(
    viewModel: CompassViewModel,
    onEndResponse: () -> Unit,
    onBack: () -> Unit,
    onOpenMaps: (Double, Double) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CompassContent(state, onEndResponse, onBack, onOpenMaps)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompassContent(
    state: CompassUiState,
    onEndResponse: () -> Unit,
    onBack: () -> Unit,
    onOpenMaps: (Double, Double) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Navigate to fire") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val alert = state.alert
        if (alert == null) {
            AlarmEnded(Modifier.padding(padding), onBack)
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                AlarmHeader(alert)
                ReflectiveStripe(state.stripeState, thickness = 20.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 280.dp), contentAlignment = Alignment.Center) {
                        ArrowArea(state)
                    }
                    state.distanceText?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.displayLarge.tabular(),
                        )
                    }
                    state.bearingText?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleMedium.tabular(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.headingStatus != HeadingStatus.Unavailable) {
                        Text(
                            if (state.needsCalibration) "Compass inaccurate: move the phone in a figure-8" else "Hold the phone flat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        String.format(Locale.US, "Fire at %.5f, %.5f", alert.lat, alert.lon),
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { onOpenMaps(alert.lat, alert.lon) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .semantics { contentDescription = "Open in Google Maps" },
                    ) {
                        Icon(Icons.Filled.Place, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Google Maps", style = MaterialTheme.typography.titleSmall)
                    }
                    Button(
                        onClick = onEndResponse,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) { Text("End response", style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    }
}

@Composable
private fun AlarmHeader(alert: FireAlert) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "Fire alarm",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).alignByBaseline().semantics { heading() },
            )
            Row {
                Text(
                    "${alert.riskPercent}%",
                    style = MaterialTheme.typography.headlineMedium.tabular(),
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    " risk",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

@Composable
private fun ArrowArea(state: CompassUiState) {
    val rotation = state.arrowRotation
    when {
        state.waitingForGps -> Waiting("Waiting for GPS…")
        state.headingStatus == HeadingStatus.Unavailable -> Text(
            "No compass on this device.\nFollow the bearing below.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        rotation == null -> Waiting("Calibrating compass…")
        else -> FireChevron(
            rotation = rotation,
            modifier = Modifier.semantics { contentDescription = state.spokenDirection.orEmpty() },
        )
    }
}

@Composable
private fun Waiting(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AlarmEnded(modifier: Modifier, onBack: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Alarm ended", style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp).heightIn(min = 48.dp)) {
            Text("Back")
        }
    }
}

private val PreviewReady = CompassUiState(
    alert = FireAlert(risk = 0.91, lat = 56.172, lon = 10.19, ts = 1760870460),
    arrowRotation = 35f,
    distanceText = "1.3 km",
    bearingText = "NE · 42°",
    spokenDirection = "Fire is 1.3 km to the north-east",
    headingStatus = HeadingStatus.Ready,
)

@Preview(name = "Compass · day", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(
    name = "Compass · night",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun CompassReadyPreview() {
    ForestFireTheme { CompassContent(PreviewReady, {}, {}, { _, _ -> }) }
}

@Preview(name = "Compass · waiting for GPS", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CompassWaitingPreview() {
    ForestFireTheme {
        CompassContent(CompassUiState(alert = PreviewReady.alert, waitingForGps = true), {}, {}, { _, _ -> })
    }
}
