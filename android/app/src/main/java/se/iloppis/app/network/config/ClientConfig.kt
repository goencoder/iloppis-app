package se.iloppis.app.network.config

import kotlinx.serialization.Serializable

/**
 * API Client configuration
 */
@Serializable
data class ClientConfig(
    /**
     * API URL endpoint
     */
    val url: String,

    /**
     * HTTP client debug
     */
    val debug: Boolean = false,

    /**
     * Connection configuration
     */
    val connection: ClientConnectionConfiguration = ClientConnectionConfiguration(),

    /**
     * Reading configuration
     */
    val reading: ClientReadingConfiguration = ClientReadingConfiguration(),

    /**
     * Writing configuration
     */
    val writing: ClientWritingConfiguration = ClientWritingConfiguration(),
)



/**
 * Client connection configuration
 */
@Serializable
data class ClientConnectionConfiguration(
    /**
     * Connection timeout
     */
    val timeout: Long = 30
)

/**
 * Client reading configuration
 */
@Serializable
data class ClientReadingConfiguration(
    /**
     * Reading timeout
     */
    val timeout: Long = 60
)

/**
 * Client writing configuration
 */
@Serializable
data class ClientWritingConfiguration(
    /**
     * Writing timeout
     */
    val timeout: Long = 60
)
