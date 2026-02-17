package se.iloppis.app.utils.user.codes

import android.util.Log
import se.iloppis.app.data.mappers.EventMapper.toDomain
import se.iloppis.app.domain.model.Event
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.events.EventAPI
import se.iloppis.app.network.keys.KeyAPI
import se.iloppis.app.network.keys.KeyApiResponse

/**
 * Validates code
 *
 * If the code is valid the response is
 * returned else a null value is returned.
 */
internal suspend fun CodeState.validateCode(code: String) : KeyApiResponse? {
    val api = ILoppisClient(config).create<KeyAPI>()
    val res = api.getApiKeyByAlias(code)

    Log.d(CodeState.TAG, "API Response - alias: ${res.alias}, isActive: ${res.isActive}, type: ${res.type}")

    if(!res.isActive) {
        Log.w(CodeState.TAG, "API key is not active")
        isValidating = false
        errorMessage = "inactive"
        return null
    }
    if(!validateResponse(res)) {
        Log.w(CodeState.TAG, "API key type mismatch. Expected type containing: $mode, Got: ${res.type?.uppercase()}")
        isValidating = false
        errorMessage = "wrong_type"
        return null
    }

    return res
}

/**
 * Gets event by ID
 */
internal suspend fun CodeState.getEventById(id: String) : Event {
    val api = ILoppisClient(config).create<EventAPI>()
    val res = api.get(id)

    Log.d(CodeState.TAG, "API Response - total: ${res.total}, events: ${res.events}")

    if(res.events.isEmpty()) {
        Log.w(CodeState.TAG, "API did not return any events, got: $res")
        isValidating = false
        errorMessage = "no_event"
        throw Exception("Could not get any event by specified id: $id")
    }

    return res.events.first().toDomain()
}



/**
 * Validates [KeyAPI] response
 *
 * Check if type matches mode ( if type is available )
 * API returns types like: API_KEY_TYPE_CASHIER, API_KEY_TYPE_WEB_CASHIER,
 * API_KEY_TYPE_SCANNER, API_KEY_TYPE_WEB_SCANNER
 */
private fun CodeState.validateResponse(response: KeyApiResponse) : Boolean {
    val type = response.type?.uppercase() ?: ""
    val valid = when(mode) {
        CodeStateMode.CASHIER -> type.contains("CASHIER")
        CodeStateMode.SCANNER -> type.contains("SCANNER")
    }
    return valid || type.isEmpty()
}
