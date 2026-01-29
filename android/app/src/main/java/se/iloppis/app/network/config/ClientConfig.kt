package se.iloppis.app.network

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
     * Connection configuration
     */
    val connection: ClientConnectionConfiguration,

    /**
     * Reading configuration
     */
    val reading: ClientReadingConfiguration,

    /**
     * Writing configuration
     */
    val writing: ClientWritingConfiguration,
)



/**
 * Client connection configuration
 */
@Serializable
data class ClientConnectionConfiguration(
    /**
     * Connection timeout
     */
    val timeout: Long
)

/**
 * Client reading configuration
 */
@Serializable
data class ClientReadingConfiguration(
    /**
     * Reading timeout
     */
    val timeout: Long
)

/**
 * Client writing configuration
 */
@Serializable
data class ClientWritingConfiguration(
    /**
     * Writing timeout
     */
    val timeout: Long
)
