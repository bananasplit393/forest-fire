package com.example.forestfire.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.forestfire.data.ConnectionState
import com.example.forestfire.data.model.FireAlert
import com.example.forestfire.data.model.Settings
import com.example.forestfire.ui.compass.CompassContent
import com.example.forestfire.ui.compass.CompassUiState
import com.example.forestfire.ui.compass.HeadingStatus
import com.example.forestfire.ui.main.MainContent
import com.example.forestfire.ui.main.MainUiState
import com.example.forestfire.ui.settings.SettingsContent
import com.example.forestfire.ui.settings.SettingsForm
import com.example.forestfire.ui.theme.ForestFireTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone

/**
 * Renders each screen in day and night themes to .impeccable/review/ for visual review.
 * It writes review images and asserts nothing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Before fun pinTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    private val alert = FireAlert(risk = 0.91, lat = 56.172, lon = 10.19, ts = 1760870460)

    private fun capture(name: String, dark: Boolean, content: @Composable () -> Unit) {
        compose.setContent { ForestFireTheme(darkTheme = dark) { content() } }
        compose.onRoot().captureRoboImage("../.impeccable/review/$name.png")
    }

    @Test fun mainAlarmDay() = capture("main-alarm-day", false) { MainContent(mainAlarm, {}, {}, {}, {}, {}, { _, _ -> }) }
    @Test fun mainAlarmNight() = capture("main-alarm-night", true) { MainContent(mainAlarm, {}, {}, {}, {}, {}, { _, _ -> }) }
    @Test fun mainIdleDay() = capture("main-idle-day", false) {
        MainContent(mainAlarm.copy(alert = null), {}, {}, {}, {}, {}, { _, _ -> })
    }
    @Test fun mainOffDutyNight() = capture("main-offduty-night", true) {
        MainContent(
            mainAlarm.copy(onDuty = false, connection = ConnectionState.Disconnected, alert = null),
            {}, {}, {}, {}, {}, { _, _ -> },
        )
    }
    @Test fun compassDay() = capture("compass-day", false) { CompassContent(compassReady, {}, {}, { _, _ -> }) }
    @Test fun compassNight() = capture("compass-night", true) { CompassContent(compassReady, {}, {}, { _, _ -> }) }
    @Test fun settingsDay() = capture("settings-day", false) {
        SettingsContent(SettingsForm.from(Settings.DEFAULT).copy(brokerPort = "70000"), true, {}, {}, {})
    }

    private val mainAlarm = MainUiState(
        onDuty = true,
        connection = ConnectionState.Connected,
        zone = "zone1",
        alert = alert,
        permissionsDenied = false,
        alertDirection = "1.3 km · NE",
        alarmSounding = true,
    )

    private val compassReady = CompassUiState(
        alert = alert,
        arrowRotation = 35f,
        distanceText = "1.3 km",
        bearingText = "NE · 42°",
        spokenDirection = "Fire is 1.3 km to the north-east",
        headingStatus = HeadingStatus.Ready,
    )
}
