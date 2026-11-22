package com.example.forestfire.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.forestfire.data.model.FireAlert
import com.example.forestfire.ui.format.StripeState
import com.example.forestfire.ui.format.alertTimeLabel
import com.example.forestfire.ui.theme.tabular

/** The active alarm: the only raised element in the app, crossed by the reflective stripe. */
@Composable
fun AlarmCard(
    alert: FireAlert,
    onNavigate: () -> Unit,
    stripeState: StripeState,
    directionText: String?,
    sounding: Boolean,
    onSilence: () -> Unit,
    onOpenMaps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "Fire alarm, risk ${alert.riskPercent} percent"
                },
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "Fire alarm",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f).alignByBaseline(),
            )
            Row {
                Text(
                    "${alert.riskPercent}%",
                    style = MaterialTheme.typography.displayMedium.tabular(),
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    " risk",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        ReflectiveStripe(stripeState, thickness = 20.dp)
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (directionText != null) {
                Text(directionText, style = MaterialTheme.typography.titleLarge.tabular())
            }
            Text(
                "Updated ${alertTimeLabel(alert.ts)}",
                style = MaterialTheme.typography.bodyMedium.tabular(),
            )
            Button(
                onClick = onNavigate,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Text("Navigate to fire", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sounding) {
                    TextButton(
                        onClick = onSilence,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("Silence buzzer", style = MaterialTheme.typography.labelLarge) }
                }
                TextButton(
                    onClick = onOpenMaps,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Open in Google Maps", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}
