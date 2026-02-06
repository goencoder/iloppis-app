package se.iloppis.app.utils.map

import org.maplibre.spatialk.geojson.GeoJson

/**
 * GeoJson simple marker object
 *
 * This will create a simple marker for
 * the [org.maplibre.compose.map.MaplibreMap]
 * with the position specified ([latitude], [longitude])
 */
fun GeoJson.simpleMarker(latitude: Double, longitude: Double) : String =
            "\"sources\": {\n" +
            "    \"geojson-marker\": {\n" +
            "        \"type\": \"geojson\",\n" +
            "        \"data\": {\n" +
            "            \"type\": \"Feature\",\n" +
            "            \"geometry\": {\n" +
            "                \"type\": \"Point\",\n" +
            "                \"coordinates\": [$latitude, $longitude]\n" +
            "            },\n" +
            "            \"properties\": {\n" +
            "                \"title\": \"Somewhere\",\n" +
            "                \"marker-symbol\": \"monument\"\n" +
            "            }\n" +
            "        }\n" +
            "    },\n" +
            "    \"geojson-lines\": {\n" +
            "        \"type\": \"geojson\",\n" +
            "        \"data\": \"./lines.geojson\"\n" +
            "    }\n" +
            "}"
