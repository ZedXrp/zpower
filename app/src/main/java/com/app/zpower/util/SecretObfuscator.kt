package com.app.zpower.util

object SecretObfuscator {
    private const val DEFAULT_KEY = "zpower_secret_key_2024"

    fun decode(bytes: ByteArray, key: String = DEFAULT_KEY): String {
        val keyBytes = key.toByteArray()
        val result = ByteArray(bytes.size)
        for (i in bytes.indices) {
            result[i] = (bytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return String(result)
    }

    /**
     * Helper to obfuscate strings during development.
     * Not used in the app at runtime, but kept for future secret additions.
     */
    fun encode(input: String, key: String = DEFAULT_KEY): ByteArray {
        val inputBytes = input.toByteArray()
        val keyBytes = key.toByteArray()
        val result = ByteArray(inputBytes.size)
        for (i in inputBytes.indices) {
            result[i] = (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return result
    }
}
