package com.example.forestfire.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.forestfire.data.model.Settings
import com.example.forestfire.ui.theme.ForestFireTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsContent(
        form = viewModel.form,
        enabled = viewModel.loaded,
        onFormChange = viewModel::onFormChange,
        onSave = { viewModel.save(onSaved) },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    form: SettingsForm,
    enabled: Boolean,
    onFormChange: (SettingsForm) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = enabled && form.isValid) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionHeading("Duty")
                Field("Zone", form.zone, form.zoneError, enabled) { onFormChange(form.copy(zone = it)) }
                Field("Firefighter ID", form.firefighterId, form.idError, enabled) {
                    onFormChange(form.copy(firefighterId = it))
                }
                SectionHeading("Broker", Modifier.padding(top = 16.dp))
                Field("Host", form.brokerHost, form.hostError, enabled, KeyboardType.Uri) {
                    onFormChange(form.copy(brokerHost = it))
                }
                Field("Port (TLS)", form.brokerPort, form.portError, enabled, KeyboardType.Number, ImeAction.Done) {
                    onFormChange(form.copy(brokerPort = it))
                }
                Text(
                    "Saving reconnects right away if you are on duty.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    error: String?,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onValueChange: (String) -> Unit,
) {
    val showError = enabled && error != null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        isError = showError,
        supportingText = if (showError) ({ Text(error.orEmpty()) }) else null,
        // Errors stay in the text colour: red is reserved for the fire alarm. The icon is the non-colour cue.
        trailingIcon = if (showError) ({ Icon(Icons.Filled.Warning, contentDescription = null) }) else null,
        colors = OutlinedTextFieldDefaults.colors(
            errorBorderColor = MaterialTheme.colorScheme.onSurface,
            errorLabelColor = MaterialTheme.colorScheme.onSurface,
            errorSupportingTextColor = MaterialTheme.colorScheme.onSurface,
            errorCursorColor = MaterialTheme.colorScheme.onSurface,
            errorTrailingIconColor = MaterialTheme.colorScheme.onSurface,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(name = "Settings · day", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(
    name = "Settings · night",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun SettingsPreview() {
    ForestFireTheme {
        SettingsContent(
            form = SettingsForm.from(Settings.DEFAULT).copy(brokerPort = "70000"),
            enabled = true,
            onFormChange = {},
            onSave = {},
            onBack = {},
        )
    }
}
