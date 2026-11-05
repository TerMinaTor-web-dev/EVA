package com.example.eva

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Serializable
data class AppRoute(val serviceId: String) : NavKey
