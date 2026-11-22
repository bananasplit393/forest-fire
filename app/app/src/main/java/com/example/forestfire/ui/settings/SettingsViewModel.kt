package com.example.forestfire.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.forestfire.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Loads the saved settings once into an editable form. Saving while on duty makes DutyService
 * reconnect automatically, because it watches the settings Flow.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    var form by mutableStateOf(SettingsForm())
        private set

    var loaded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            form = SettingsForm.from(repository.settings.first())
            loaded = true
        }
    }

    fun onFormChange(newForm: SettingsForm) {
        form = newForm
    }

    fun save(onSaved: () -> Unit) {
        if (!form.isValid) return
        viewModelScope.launch {
            repository.save(form.toSettings())
            onSaved()
        }
    }
}
