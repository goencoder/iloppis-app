package se.iloppis.app.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import se.iloppis.app.domain.model.Event

/**
 * Events location map
 *
 * Shows a map of where the event location
 * is taking place.
 */
@Composable
fun Map(
    event: Event,
    modifier: Modifier = Modifier,
    zoom: Float = 17f
) {
    val location = LatLng(event.latitude ?: .0, event.longitude ?: .0)
    val marker = rememberUpdatedMarkerState(position = location)
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, zoom)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera
    ) {
        Marker(
            state = marker,
            title = event.name,
            snippet = event.location
        )
    }
}
