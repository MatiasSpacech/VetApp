package com.veterapp.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
  private const val PREFIX = "pbkdf2_sha256"
  private const val ITERATIONS = 150_000
  private const val KEY_BITS = 256
  private const val SALT_BYTES = 16
  private val random = SecureRandom()

  fun hash(pin: CharArray, salt: ByteArray = generateSalt()): String {
    val spec = PBEKeySpec(pin, salt, ITERATIONS, KEY_BITS)
    val key =
      try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
      } finally {
        spec.clearPassword()
      }
    return try {
      val encoder = Base64.getEncoder()
      "${PREFIX}\$${ITERATIONS}\$${encoder.encodeToString(salt)}\$${encoder.encodeToString(key)}"
    } finally {
      key.fill(0)
    }
  }

  fun verify(pin: CharArray, stored: String): Boolean {
    val parts = stored.split('$')
    if (parts.size != 4 || parts[0] != PREFIX) return false
    val iterations = parts[1].toIntOrNull() ?: return false
    if (iterations <= 0) return false
    val salt = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull() ?: return false
    val expected = runCatching { Base64.getDecoder().decode(parts[3]) }.getOrNull() ?: return false
    if (salt.isEmpty() || expected.size * 8 != KEY_BITS) return false

    val spec = PBEKeySpec(pin, salt, iterations, KEY_BITS)
    val actual =
      try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
      } finally {
        spec.clearPassword()
      }
    return try {
      MessageDigest.isEqual(expected, actual)
    } finally {
      actual.fill(0)
    }
  }

  fun generateSalt(): ByteArray = ByteArray(SALT_BYTES).also(random::nextBytes)
}
