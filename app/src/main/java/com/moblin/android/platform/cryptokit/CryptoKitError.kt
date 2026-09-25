package com.moblin.android.platform.cryptokit

sealed class CryptoKitError(description: String) : Exception(description, null, false, false) {
    object incorrectKeySize : CryptoKitError("incorrectKeySize")

    object incorrectParameterSize : CryptoKitError("incorrectParameterSize")

    object authenticationFailure : CryptoKitError("authenticationFailure")

    class underlyingCoreCryptoError(val error: Int) : CryptoKitError("underlyingCoreCryptoError(error: $error)") {
        override fun equals(other: Any?): Boolean = other is underlyingCoreCryptoError && other.error == error

        override fun hashCode(): Int = error
    }

    object wrapFailure : CryptoKitError("wrapFailure")

    object unwrapFailure : CryptoKitError("unwrapFailure")

    object invalidParameter : CryptoKitError("invalidParameter")

    override fun toString(): String = message.orEmpty()
}

sealed class CryptoKitASN1Error(description: String) : Exception(description, null, false, false) {
    object invalidFieldIdentifier : CryptoKitASN1Error("invalidFieldIdentifier")

    object unexpectedFieldType : CryptoKitASN1Error("unexpectedFieldType")

    object invalidObjectIdentifier : CryptoKitASN1Error("invalidObjectIdentifier")

    object invalidASN1Object : CryptoKitASN1Error("invalidASN1Object")

    object invalidASN1IntegerEncoding : CryptoKitASN1Error("invalidASN1IntegerEncoding")

    object truncatedASN1Field : CryptoKitASN1Error("truncatedASN1Field")

    object unsupportedFieldLength : CryptoKitASN1Error("unsupportedFieldLength")

    object invalidPEMDocument : CryptoKitASN1Error("invalidPEMDocument")

    override fun toString(): String = message.orEmpty()
}

internal fun coreCryptoError(): CryptoKitError = CryptoKitError.underlyingCoreCryptoError(-1)
