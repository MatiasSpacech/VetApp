package com.veterapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorsTest {
  @Test
  fun keepsTheFirstLineOfAUsefulError() {
    val error = IllegalStateException("column reference \"id\" is ambiguous\nURL: https://x")
    assertEquals("column reference \"id\" is ambiguous", error.userMessage("fallback"))
  }

  @Test
  fun hidesHeadersAndTokens() {
    val error = IllegalStateException("URL: https://example.supabase.co\nAuthorization=Bearer secret")
    assertEquals("No se pudo sincronizar", error.userMessage("No se pudo sincronizar"))
  }

  @Test
  fun hidesUrlsEvenWithoutNewlines() {
    val error =
      IllegalStateException(
        "HTTP request to http://10.0.2.2:54321/rest/v1/rpc/apply_pet_operation failed",
      )
    assertEquals("Sin conexión con Supabase", error.userMessage("Sin conexión con Supabase"))
  }

  @Test
  fun detectsNetworkFailures() {
    assertEquals(
      true,
      IllegalStateException("Failed to connect to /10.0.2.2:54321").isNetworkFailure(),
    )
    assertEquals(false, IllegalStateException("column reference id is ambiguous").isNetworkFailure())
  }

  @Test
  fun usesFallbackWhenThereIsNoMessage() {
    assertEquals("No se pudo guardar", IllegalStateException().userMessage("No se pudo guardar"))
  }
}
