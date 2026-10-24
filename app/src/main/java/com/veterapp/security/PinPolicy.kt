package com.veterapp.security

object PinPolicy {
  const val MIN_LENGTH = 4
  const val MAX_LENGTH = 8

  fun error(pin: String): String? =
    when {
      pin.length < MIN_LENGTH || pin.length > MAX_LENGTH ->
        "El PIN debe tener entre $MIN_LENGTH y $MAX_LENGTH dígitos"
      pin.any { it !in '0'..'9' } -> "El PIN solo admite dígitos"
      else -> null
    }
}
