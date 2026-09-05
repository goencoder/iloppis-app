package se.iloppis.app.data.models

import kotlinx.serialization.Serializable

/** Persistable counterpart of the backend's sold-item error code. */
@Serializable
enum class SerializableSoldItemErrorCode {
    UNSPECIFIED,
    INVALID_SELLER,
    DUPLICATE_RECEIPT;
    
    companion object {
        /** Maps a backend protobuf number; unknown and null values become [UNSPECIFIED]. */
        fun fromProtoNumber(protoNumber: Int?): SerializableSoldItemErrorCode {
            return when (protoNumber) {
                1 -> INVALID_SELLER
                2 -> DUPLICATE_RECEIPT
                0 -> UNSPECIFIED
                else -> UNSPECIFIED
            }
        }
        
        /** Maps a numeric or symbolic backend value; unknown and null values become [UNSPECIFIED]. */
        fun fromString(errorCode: String?): SerializableSoldItemErrorCode {
            if (errorCode == null) return UNSPECIFIED
            
            errorCode.toIntOrNull()?.let { return fromProtoNumber(it) }

            return when {
                errorCode.contains("INVALID_SELLER", ignoreCase = true) -> INVALID_SELLER
                errorCode.contains("DUPLICATE_RECEIPT", ignoreCase = true) -> DUPLICATE_RECEIPT
                else -> UNSPECIFIED
            }
        }
    }
}
