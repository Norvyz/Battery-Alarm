package com.batteryalarm.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )
)