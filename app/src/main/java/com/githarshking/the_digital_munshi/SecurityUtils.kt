package com.githarshking.the_digital_munshi

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature

/**
 * Handles the "Trust Layer".
 * Generates a permanent Digital Identity for the user inside the Android Secure Hardware.
 */
object SecurityUtils {

    private const val KEY_ALIAS = "DigitalMunshi_IdentityKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    // We use Elliptic Curve (EC) cryptography - it's faster and modern.
    // Algorithm: SHA256 with ECDSA
    private const val SIGN_ALGO = "SHA256withECDSA"

    /**
     * Signs the data (String) using the device's private key.
     * Returns the Signature as a Base64 String.
     */
    fun signData(data: String): String {
        ensureKeyExists()

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("Keystore entry not found")

        val signature = Signature.getInstance(SIGN_ALGO)
        signature.initSign(entry.privateKey)
        signature.update(data.toByteArray(Charsets.UTF_8))

        val signatureBytes = signature.sign()
        return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
    }

    /**
     * Generates the key pair if it doesn't exist.
     */
    private fun ensureKeyExists() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).run {
                setDigests(KeyProperties.DIGEST_SHA256)
                setUserAuthenticationRequired(false) // For MVP simplicity. In Pro, set true (requires biometric)
                build()
            }

            kpg.initialize(parameterSpec)
            kpg.generateKeyPair()
        }
    }

    /**
     * Returns the Public Key. The Lender uses this to verify the signature.
     */
    fun getPublicKey(): String {
        ensureKeyExists()
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val cert = keyStore.getCertificate(KEY_ALIAS)
        return Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
    }
}