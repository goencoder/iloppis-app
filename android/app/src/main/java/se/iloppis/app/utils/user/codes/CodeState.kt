package se.iloppis.app.utils.user.codes

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import se.iloppis.app.domain.model.Event
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.network.config.ClientConfig
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.ui.screens.ScreenModel
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction

/**
 * iLoppis code state
 */
class CodeState(
    val event: Event,
    val mode: CodeStateMode,
    val scope: CoroutineScope,
    val config: ClientConfig,
    val screen: ScreenModel
) {
    /**
     * Is state validating code status
     */
    var isValidating by mutableStateOf(false)
        internal set

    /**
     * State error message
     */
    var errorMessage by mutableStateOf<String?>(null)
        internal set



    /**
     * Validates code
     *
     * Validates [ScreenPage.Cashier] or [ScreenPage.Scanner] code.
     * If the code is valid the user will be navigated to one of
     * the two pages depending on the type of [se.iloppis.app.utils.user.codes.CodeState.mode]
     */
    fun validate(code: String) {
        isValidating = false /* Resets in case code is to short */
        errorMessage = null
        if(code.length < CODE_LENGTH) return

        scope.launch {
            try {
                isValidating = true
                val format = "${code.take(CODE_LENGTH / 2)}-${
                    code.substring(
                        CODE_LENGTH / 2,
                        CODE_LENGTH
                    )
                }".uppercase()
                val id = event.id

                Log.d(TAG, "Validating code: $format for event: $id")
                val res = validate(id, format) ?: return@launch

                Log.i(TAG, "Code validated successfully! Navigating to $mode")
                val page = when (mode) {
                    CodeStateMode.CASHIER -> ScreenPage.Cashier(event, res.apiKey)
                    CodeStateMode.SCANNER -> ScreenPage.Scanner(event, res.apiKey)
                }
                screen.onAction(ScreenAction.NavigateToPage(page, false))
                screen.onAction(ScreenAction.RemoveOverlay)
            }catch (e: HttpException) {
                handleHttpException(e)
            } catch (e: Exception) {
                Log.e(TAG, "Validation error: ${e.message}", e)
                errorMessage = "network_error"
            }
            isValidating = false
        }
    }



    /**
     * Handles HTTP exception
     */
    private fun handleHttpException(e: HttpException) {
        val error = e.response()?.errorBody()?.string()
        Log.e(TAG, "HTTP Error ${e.code()}: ${e.message()}")
        Log.e(TAG, "Error body: $error")

        val key = when (e.code()) {
            404 -> "not_found"
            401, 403 -> "unauthorized"
            else -> "invalid"
        }

        errorMessage = key
    }



    companion object {
        /**
         * Code state debug tag
         */
        val TAG: String = CodeState::class.java.simpleName

        /**
         * Code length
         */
        const val CODE_LENGTH: Int = 6
    }
}





/**
 * Creates and [remember] a [CodeState]
 *
 * The [CodeState] relies on [ClientConfig] for
 * [se.iloppis.app.network.ILoppisClient] communication
 * that is launched with [CoroutineScope].
 * If the key is valid [CodeState] will automatically
 * navigate to the appropriate screen using [ScreenModel]
 */
@Composable
fun rememberCodeState(
    event: Event,
    mode: CodeStateMode,
    config: ClientConfig = clientConfig(),
    scope: CoroutineScope = rememberCoroutineScope(),
    screen: ScreenModel = screenContext()
) : CodeState {
    return remember {
        CodeState(event, mode, scope, config, screen)
    }
}
