package com.veterapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable data object Settings : NavKey

@Serializable data class ClientDetail(val clientId: String) : NavKey

@Serializable data class PetDetail(val petId: String) : NavKey
