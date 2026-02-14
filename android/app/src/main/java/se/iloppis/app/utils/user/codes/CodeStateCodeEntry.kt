package se.iloppis.app.utils.user.codes

import android.util.Log
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.keys.KeyAPI
import se.iloppis.app.network.keys.KeyApiResponse

/**
 * Validates code
 *
 * If the code is valid the response is
 * returned else a null value is returned.
 */
internal suspend fun CodeState.validate(event: String, code: String) : KeyApiResponse? {
    val api = ILoppisClient(config).create<KeyAPI>()
    val res = api.getApiKeyByAlias(event, code)

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
