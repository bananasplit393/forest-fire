package com.example.forestfire.ui.compass

import android.app.Application
import android.hardware.GeomagneticField
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.forestfire.data.ConnectionState
import com.example.forestfire.data.DutyState
import com.example.forestfire.data.Heading
import com.example.forestfire.data.HeadingSource
import com.example.forestfire.data.LocationSource
import com.example.forestfire.data.isFresh
import com.example.forestfire.data.model.FireAlert
import com.example.forestfire.nav.Bearing
import com.example.forestfire.ui.format.bearingLabel
import com.example.forestfire.ui.format.bearingSpoken
import com.example.forestfire.ui.format.StripeState
import com.example.forestfire.ui.format.stripeStateFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** Waiting = sensor exists but no reading yet (calibrating); Unavailable = no sensor at all. */
enum class HeadingStatus { Waiting, Ready, Unavailable }

data class CompassUiState(
    val alert: FireAlert? = null,
    /** Degrees to rotate the arrow; null until both a fix and a heading exist. */
    val arrowRotation: Float? = null,
    val distanceText: String? = null,
    /** "NE · 42°" */
    val bearingText: String? = null,
    /** "Fire is 1.3 km to the north-east", for TalkBack. */
    val spokenDirection: String? = null,
    val waitingForGps: Boolean = false,
    val headingStatus: HeadingStatus = HeadingStatus.Waiting,
    val needsCalibration: Boolean = false,
    /** Duty state shown by the stripe: it always means duty, never the alarm. */
    val stripeState: StripeState = StripeState.Lit,
)

/** Combines the active alarm, the phone's position and its heading into what the arrow shows. */
class CompassViewModel(application: Application) : AndroidViewModel(application) {

    private val headingSource = HeadingSource(application)
    private val hasCompass = headingSource.isAvailable

    // Start with null so the screen renders before the first fix / sensor reading arrives.
    private val locations: Flow<Location?> = flow {
        emit(null)
        emitAll(LocationSource(application).updates(LOCATION_INTERVAL_MS))
    }
    private val headings: Flow<Heading?> = flow {
        emit(null)
        emitAll(headingSource.updates())
    }

    val uiState: StateFlow<CompassUiState> =
        combine(
            DutyState.currentAlert,
            locations,
            headings,
            DutyState.onDuty,
            DutyState.connection,
        ) { alert, location, heading, onDuty, connection ->
            buildState(alert, location, heading, stripeStateFor(onDuty, connection))
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CompassUiState(alert = DutyState.currentAlert.value),
        )

    private fun buildState(
        alert: FireAlert?,
        location: Location?,
        heading: Heading?,
        stripeState: StripeState,
    ): CompassUiState {
        if (alert == null) return CompassUiState()
        val headingStatus = when {
            !hasCompass -> HeadingStatus.Unavailable
            heading == null -> HeadingStatus.Waiting
            else -> HeadingStatus.Ready
        }
        val needsCalibration = heading?.lowAccuracy == true
        val here = location?.takeIf { it.isFresh() }
            ?: return CompassUiState(
                alert = alert,
                waitingForGps = true,
                headingStatus = headingStatus,
                needsCalibration = needsCalibration,
                stripeState = stripeState,
            )
        val fire = Location("fire").apply {
            latitude = alert.lat
            longitude = alert.lon
        }
        val bearing = here.bearingTo(fire)
        val distanceText = Bearing.formatDistance(here.distanceTo(fire))
        // The rotation sensor measures from magnetic north; bearingTo() is from true north.
        val trueAzimuth = heading?.let { it.azimuth + declination(here) }
        return CompassUiState(
            alert = alert,
            arrowRotation = trueAzimuth?.let { Bearing.arrowRotation(bearing, it) },
            distanceText = distanceText,
            bearingText = bearingLabel(bearing),
            spokenDirection = "Fire is $distanceText to the ${bearingSpoken(bearing)}",
            headingStatus = headingStatus,
            needsCalibration = needsCalibration,
            stripeState = stripeState,
        )
    }

    private fun declination(at: Location): Float =
        GeomagneticField(
            at.latitude.toFloat(),
            at.longitude.toFloat(),
            at.altitude.toFloat(),
            System.currentTimeMillis(),
        ).declination

    private companion object {
        const val LOCATION_INTERVAL_MS = 2_000L
    }
}
