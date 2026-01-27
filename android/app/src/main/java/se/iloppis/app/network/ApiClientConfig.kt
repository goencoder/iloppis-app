package se.iloppis.app.network

/**
 * API Client configuration
 */
data class ApiClientConfig(
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
data class ClientConnectionConfiguration(
    /**
     * Connection timeout
     */
    val timeout: Int
)

/**
 * Client reading configuration
 */
data class ClientReadingConfiguration(
    /**
     * Reading timeout
     */
    val timeout: Int
)

/**
 * Client writing configuration
 */
data class ClientWritingConfiguration(
    /**
     * Writing timeout
     */
    val timeout: Int
)
