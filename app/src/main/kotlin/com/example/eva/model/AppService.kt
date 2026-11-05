package com.example.eva.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class AppService(
    val id: String,
    val name: String,
    val url: String,
    val category: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val isDesktopUserAgent: Boolean = false,
    val description: String = ""
)
