package se.iloppis.app.ui.screens.events

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import retrofit2.HttpException
import se.iloppis.app.R
import se.iloppis.app.data.mappers.EventMapper.toDomain
import se.iloppis.app.navigation.ScreenPage
import se.iloppis.app.network.ILoppisClient
import se.iloppis.app.network.config.clientConfig
import se.iloppis.app.network.events.EventAPI
import se.iloppis.app.network.keys.KeyAPI
import se.iloppis.app.ui.screens.screenContext
import se.iloppis.app.ui.states.ScreenAction
import se.iloppis.app.ui.theme.AppColors

private const val TAG = "CodeEntryScreen"

@OptIn(ExperimentalMaterial3Api::class)

/**
 * Screen for direct code entry to access Cashier/Scanner modes.
 *
 * User enters code in XXX-YYY format (auto-formatted).
 * On verify: resolves alias via API, validates type/active,
 * then navigates to CodeConfirmScreen on success.
 */
@Composable
fun CodeEntryScreen(mode: String) {
    val screen = screenContext()
    var rawCode by remember { mutableStateOf("") } // Up to 6 alphanumeric chars
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Format for display: XXX-YYY
    val displayCode = if (rawCode.length > 3) {
        "${rawCode.substring(0, 3)}-${rawCode.substring(3)}"
    } else {
        rawCode
    }

    val isCodeComplete = rawCode.length == 6

    fun onCodeChange(input: String) {
        // Strip dashes, whitespace, and non-alphanumeric, limit to 6
        val cleaned = input.replace("-", "").replace(" ", "")
            .filter { it.isLetterOrDigit() }
            .take(6)
            .uppercase()
        rawCode = cleaned
        errorMessage = null
    }

    fun verifyCode() {
        if (!isCodeComplete || isLoading) return
        val formattedAlias = "${rawCode.substring(0, 3)}-${rawCode.substring(3, 6)}"
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val keyApi = ILoppisClient(clientConfig()).create<KeyAPI>()
                val response = keyApi.getApiKeyByAlias(formattedAlias)
                Log.d(TAG, "Alias resolved: eventId=${response.eventId}, type=${response.type}, active=${response.isActive}")

                // Validate isActive
                if (!response.isActive) {
                    errorMessage = "inactive"
                    isLoading = false
                    return@launch
                }

                // Validate type matches mode
                val responseType = response.type?.uppercase() ?: ""
                val isValidType = when (mode) {
                    "CASHIER" -> responseType.contains("CASHIER")
                    "SCANNER" -> responseType.contains("SCANNER")
                    else -> true
                }
                if (responseType.isNotEmpty() && !isValidType) {
                    errorMessage = if (mode == "CASHIER") "wrong_type_cashier" else "wrong_type_scanner"
                    isLoading = false
                    return@launch
                }

                // Fetch the event to show in confirmation
                val eventApi = ILoppisClient(clientConfig()).create<EventAPI>()
                val eventResponse = eventApi.get(response.eventId)
                val event = eventResponse.events.firstOrNull()?.toDomain()

                if (event == null) {
                    errorMessage = "not_found"
                    isLoading = false
                    return@launch
                }

                // Navigate to Code Confirm
                screen.onAction(
                    ScreenAction.NavigateToPage(
                        ScreenPage.CodeConfirm(
                            event = event,
                            apiKey = response.apiKey,
                            mode = mode
                        )
                    )
                )
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP Error ${e.code()}", e)
                errorMessage = when (e.code()) {
                    404 -> "not_found"
                    else -> "network"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying code", e)
                errorMessage = "network"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.code_entry_label))
                },
                navigationIcon = {
                    IconButton(onClick = { screen.popPage() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode hint
            Text(
                text = when (mode) {
                    "CASHIER" -> stringResource(R.string.code_entry_cashier_hint)
                    "SCANNER" -> stringResource(R.string.code_entry_scanner_hint)
                    else -> stringResource(R.string.code_entry_label)
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Code input field with XXX-YYY auto-formatting
            OutlinedTextField(
                value = displayCode,
                onValueChange = { onCodeChange(it) },
                label = { Text(stringResource(R.string.code_entry_label)) },
                placeholder = { Text("XXX-YYY") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.Characters
                ),
                keyboardActions = KeyboardActions(
                    onDone = { verifyCode() }
                ),
                singleLine = true
            )

            // Error message
            val errorText = when (errorMessage) {
                "inactive" -> stringResource(R.string.code_entry_error_inactive)
                "wrong_type_cashier" -> stringResource(R.string.code_entry_error_wrong_type_cashier)
                "wrong_type_scanner" -> stringResource(R.string.code_entry_error_wrong_type_scanner)
                "not_found" -> stringResource(R.string.code_entry_error_not_found)
                "network" -> stringResource(R.string.code_entry_error_network)
                else -> null
            }
            if (errorText != null) {
                Text(
                    text = errorText,
                    color = AppColors.TextError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            // Verify button
            Button(
                onClick = { verifyCode() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                enabled = isCodeComplete && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.verify_button))
            }

            // Cancel button
            Button(
                onClick = { screen.popPage() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Background
                )
            ) {
                Text(
                    stringResource(R.string.cancel_button),
                    color = AppColors.TextPrimary
                )
            }
        }
    }
}

