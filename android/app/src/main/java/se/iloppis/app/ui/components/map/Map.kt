package se.iloppis.app.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.GeoJson
import org.maplibre.spatialk.geojson.Position
import se.iloppis.app.R
import se.iloppis.app.domain.model.Event
import se.iloppis.app.utils.context.currentContext
import se.iloppis.app.utils.map.loadStyle
import se.iloppis.app.utils.map.simpleMarker

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
    style: Int = R.raw.maps,
    zoom: Double = 1.0
) {
    val baseStyle = BaseStyle.loadStyle(currentContext(), style)
    val camera = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(
                latitude = event.latitude ?: .0,
                longitude = event.longitude ?: .0
            ),
            zoom = zoom
        )
    )

    MaplibreMap(
        modifier = modifier,
        cameraState = camera,
        baseStyle = baseStyle
    ) {
        val source = rememberGeoJsonSource(
            data = GeoJsonData.JsonString(
                GeoJson.simpleMarker(event.latitude ?: .0, event.longitude ?: .0)
            )
        )
        CircleLayer(
            id = "events-waypoint",
            source = source
        )
    }
}
