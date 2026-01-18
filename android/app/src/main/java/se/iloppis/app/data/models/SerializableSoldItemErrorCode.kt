package se.iloppis.app.data.models

import kotlinx.serialization.Serializable

/**
 * Error codes for rejected sold items.
 * Used for file storage (pending_review.json).
 * 
 * Maps to backend proto: com.iloppis.v1.SoldItemErrorCode
 * 
 * Error code mapping (backend proto -> Android enum):
 * - SOLD_ITEM_ERROR_CODE_INVALID_SELLER (1) -> INVALID_SELLER
 * - SOLD_ITEM_ERROR_CODE_DUPLICATE_RECEIPT (2) -> DUPLICATE_RECEIPT
 * - SOLD_ITEM_ERROR_CODE_UNSPECIFIED (0) -> UNSPECIFIED
 */
@Serializable
enum class SerializableSoldItemErrorCode {
    UNSPECIFIED,
    INVALID_SELLER,
    DUPLICATE_RECEIPT;
    
    companion object {
        /**
         * Parse error code from backend API response.
         * Backend sends numeric proto enum values: 0=UNSPECIFIED, 1=INVALID_SELLER, 2=DUPLICATE_RECEIPT
         */
        fun fromProtoNumber(protoNumber: Int?): SerializableSoldItemErrorCode {
            return when (protoNumber) {
                1 -> INVALID_SELLER
                2 -> DUPLICATE_RECEIPT
                0 -> UNSPECIFIED
                else -> UNSPECIFIED // Unknown/null values
            }
        }
        
        /**
         * Parse error code from string representation (e.g. from API).
         * 
         * Accepts both:
         * - Numeric strings: "0", "1", "2"
         * - Backend enum names: "SOLD_ITEM_ERROR_CODE_INVALID_SELLER"
         */
        fun fromString(errorCode: String?): SerializableSoldItemErrorCode {
            if (errorCode == null) return UNSPECIFIED
            
            // Try numeric parse first
            errorCode.toIntOrNull()?.let { return fromProtoNumber(it) }
            
            // Fall back to string matching
            return when {
                errorCode.contains("INVALID_SELLER", ignoreCase = true) -> INVALID_SELLER
                errorCode.contains("DUPLICATE_RECEIPT", ignoreCase = true) -> DUPLICATE_RECEIPT
                else -> UNSPECIFIED
            }
        }
    }
}
