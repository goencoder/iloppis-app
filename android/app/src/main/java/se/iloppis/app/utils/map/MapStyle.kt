package se.iloppis.app.utils.map

import android.content.Context
import org.maplibre.compose.style.BaseStyle
import se.iloppis.app.R

/**
 * Loads map style from local resource
 *
 * Using the android [Context] to load
 * the resource from [R] and using it
 * as a json style object for [org.maplibre.compose.map.MaplibreMap]
 */
fun BaseStyle.Companion.loadStyle(context: Context, style: Int = R.raw.map) : BaseStyle {
    context.resources.openRawResource(style).use {
        val json = it.readBytes().decodeToString()
        return BaseStyle.Json(json)
    }
}
