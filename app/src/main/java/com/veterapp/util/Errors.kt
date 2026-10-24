package com.veterapp.util

/**
 * Los mensajes de supabase-kt pueden incluir la URL con filtros y headers con tokens.
 * Para la UI usamos solo la primera línea y descartamos cualquier texto sensible.
 */
fun Throwable.userMessage(fallback: String): String {
  val firstLine = message?.lineSequence()?.firstOrNull()?.trim().orEmpty()
  if (firstLine.isBlank()) return fallback
  if (
    firstLine.contains("Bearer", ignoreCase = true) ||
      firstLine.contains("apikey", ignoreCase = true) ||
      firstLine.contains("URL:", ignoreCase = true) ||
      firstLine.contains("http://", ignoreCase = true) ||
      firstLine.contains("https://", ignoreCase = true) ||
      firstLine.contains("Failed to connect", ignoreCase = true)
  ) {
    return fallback
  }
  return firstLine.take(180)
}

fun Throwable.isNetworkFailure(): Boolean {
  val text = message.orEmpty()
  return NETWORK_MARKERS.any { text.contains(it, ignoreCase = true) }
}

private val NETWORK_MARKERS =
  listOf(
    "Failed to connect",
    "Network is unreachable",
    "Unable to resolve host",
    "timeout",
    "timed out",
    "HTTP request to",
    "ConnectException",
    "UnknownHostException",
  )

