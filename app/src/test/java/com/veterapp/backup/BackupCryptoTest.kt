package com.veterapp.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoTest {
  private val passphrase = "frase-segura-123".toCharArray()

  @Test
  fun encryptsAndDecrypts() {
    val plaintext = """{"pets":[],"outbox":[]}""".encodeToByteArray()

    val encrypted = BackupCrypto.encrypt(passphrase, plaintext)
    val decrypted = BackupCrypto.decrypt(passphrase, encrypted)

    assertFalse(encrypted.contentEquals(plaintext))
    assertArrayEquals(plaintext, decrypted)
  }

  @Test
  fun rejectsWrongPassphrase() {
    val encrypted = BackupCrypto.encrypt(passphrase, "datos".encodeToByteArray())

    assertThrows(Exception::class.java) {
      BackupCrypto.decrypt("otra-frase-123".toCharArray(), encrypted)
    }
  }

  @Test
  fun rejectsCorruptedHeader() {
    val encrypted = BackupCrypto.encrypt(passphrase, "datos".encodeToByteArray())
    encrypted[0] = 'X'.code.toByte()

    assertThrows(IllegalArgumentException::class.java) {
      BackupCrypto.decrypt(passphrase, encrypted)
    }
  }
}
