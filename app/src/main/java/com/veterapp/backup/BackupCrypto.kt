package com.veterapp.backup

import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
  private val magic = byteArrayOf('V'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte(), '1'.code.toByte())
  private val random = SecureRandom()
  private const val SALT_SIZE = 16
  private const val IV_SIZE = 12
  private const val ITERATIONS = 150_000
  private const val KEY_BITS = 256

  fun encrypt(passphrase: CharArray, plaintext: ByteArray): ByteArray {
    require(passphrase.size >= 8) { "La frase debe tener al menos 8 caracteres" }
    val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
    val key = deriveKey(passphrase, salt)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
    val iv = cipher.iv
    require(iv.size == IV_SIZE) { "IV inesperado" }
    val ciphertext = cipher.doFinal(plaintext)

    return ByteArrayOutputStream().use { output ->
      output.write(magic)
      output.write(salt)
      output.write(iv.size)
      output.write(iv)
      output.write(ciphertext)
      output.toByteArray()
    }
  }

  fun decrypt(passphrase: CharArray, data: ByteArray): ByteArray {
    require(passphrase.size >= 8) { "La frase debe tener al menos 8 caracteres" }
    require(data.size > magic.size + SALT_SIZE + 1 + IV_SIZE) { "Backup inválido" }
    require(data.copyOfRange(0, magic.size).contentEquals(magic)) { "Formato de backup desconocido" }

    var offset = magic.size
    val salt = data.copyOfRange(offset, offset + SALT_SIZE)
    offset += SALT_SIZE
    val ivSize = data[offset].toInt() and 0xFF
    offset += 1
    require(ivSize == IV_SIZE) { "IV inválido" }
    val iv = data.copyOfRange(offset, offset + ivSize)
    offset += ivSize
    val ciphertext = data.copyOfRange(offset, data.size)

    val key = deriveKey(passphrase, salt)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
    return cipher.doFinal(ciphertext)
  }

  private fun deriveKey(passphrase: CharArray, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
    return try {
      SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    } finally {
      spec.clearPassword()
    }
  }
}
