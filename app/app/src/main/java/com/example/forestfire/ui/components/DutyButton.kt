package com.example.forestfire.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The one primary action of the main screen, sized for the thumb. */
@Composable
fun DutyButton(
    onDuty: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = CircleShape,
    ) {
        Text(
            if (onDuty) "Go off duty" else "Go on duty",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
