package se.iloppis.app.utils.map

import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.spatialk.geojson.GeoJson
import se.iloppis.app.domain.model.Event

/**
 * GeoJson simple marker object
 *
 * This will create a simple marker for
 * the [org.maplibre.compose.map.MaplibreMap]
 * with the position specified ([latitude], [longitude])
 */
fun GeoJson.fromEvent(event: Event) : FeatureCollection = FeatureCollection.fromFeature(
    Feature.fromGeometry(
        Point.fromLngLat(event.longitude ?: .0, event.latitude ?: .0)
    ).apply {
        addStringProperty("name", event.name)
    }
)
