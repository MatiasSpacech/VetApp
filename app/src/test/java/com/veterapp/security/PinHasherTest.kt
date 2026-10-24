package com.veterapp.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
  @Test
  fun hashesAndVerifiesPin() {
    val pin = "1234".toCharArray()
    val stored = PinHasher.hash(pin)

    assertTrue(stored.startsWith("pbkdf2_sha256$"))
    assertTrue(PinHasher.verify(pin, stored))
    assertFalse(PinHasher.verify("0000".toCharArray(), stored))
  }

  @Test
  fun usesRandomSalt() {
    val pin = "1234".toCharArray()

    assertNotEquals(PinHasher.hash(pin), PinHasher.hash(pin))
  }

  @Test
  fun rejectsMalformedHashes() {
    val pin = "1234".toCharArray()

    assertFalse(PinHasher.verify(pin, ""))
    assertFalse(PinHasher.verify(pin, "pbkdf2_sha256\$abc\$salt\$hash"))
    assertFalse(PinHasher.verify(pin, "otro\$150000\$c2FsdA==\$aGFzaA=="))
    assertFalse(PinHasher.verify(pin, "pbkdf2_sha256\$150000\$***\$***"))
  }

  @Test
  fun validatesPinPolicy() {
    assertTrue(PinPolicy.error("1234") == null)
    assertTrue(PinPolicy.error("12345678") == null)
    assertTrue(PinPolicy.error("123") != null)
    assertTrue(PinPolicy.error("123456789") != null)
    assertTrue(PinPolicy.error("12a4") != null)
  }
}
