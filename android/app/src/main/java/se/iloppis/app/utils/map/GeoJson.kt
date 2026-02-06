package se.iloppis.app.utils.map

import kotlinx.serialization.json.Json.Default.encodeToString
import org.maplibre.spatialk.geojson.GeoJson

/**
 * GeoJson simple marker object
 *
 * This will create a simple marker for
 * the [org.maplibre.compose.map.MaplibreMap]
 * with the position specified ([latitude], [longitude])
 */
fun GeoJson.json(data: GeoJsonDataObject) : String = encodeToString(data)



/**
 * GeoJson Data for [org.maplibre.geojson.GeoJson]
 */
data class GeoJsonDataObject(
    /**
     * Data type
     */
    val type: String = "Feature",

    /**
     * GeoJson data geometry
     */
    val geometry: GeoJsonGeometry = GeoJsonGeometry(),

    /**
     * GeoJson data property
     */
    val properties: GeoJsonProperty = GeoJsonProperty()
)

/**
 * GeoJson geometry data
 */
data class GeoJsonGeometry(
    /**
     * Geometry object type
     */
    val type: String = "Point",

    /**
     * Geometry coordinates
     */
    val coordinates: List<Double> = emptyList()
)

/**
 * GeoJson property data
 */
data class GeoJsonProperty(
    /**
     * Property name
     *
     * The name of the location
     */
    val name: String = "Event location"
)
