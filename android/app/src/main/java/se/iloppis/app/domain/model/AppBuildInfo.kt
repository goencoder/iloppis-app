package se.iloppis.app.domain.model

import se.iloppis.app.BuildConfig

/** Immutable, code-owned metadata for the installed application artifact. */
data class AppBuildInfo(
    val environment: String,
    val versionName: String,
    val versionCode: Long,
    val apiBaseUrl: String,
) {
    val isStaging: Boolean
        get() = environment == STAGING_ENVIRONMENT

    val versionLabel: String
        get() = "$versionName ($versionCode)"

    companion object {
        private const val STAGING_ENVIRONMENT = "staging"

        fun current(): AppBuildInfo = AppBuildInfo(
            environment = BuildConfig.APP_ENVIRONMENT,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
            apiBaseUrl = BuildConfig.API_BASE_URL,
        )
    }
}
