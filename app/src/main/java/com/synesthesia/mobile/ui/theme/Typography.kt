package com.synesthesia.mobile.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.synesthesia.mobile.R.array.com_google_android_gms_fonts_certs
)

private fun downloadableFamily(name: String): FontFamily {
    val googleFont = GoogleFont(name)
    return FontFamily(
        Font(googleFont = googleFont, fontProvider = googleFontsProvider, weight = FontWeight.Normal),
        Font(googleFont = googleFont, fontProvider = googleFontsProvider, weight = FontWeight.Medium),
        Font(googleFont = googleFont, fontProvider = googleFontsProvider, weight = FontWeight.SemiBold),
        Font(googleFont = googleFont, fontProvider = googleFontsProvider, weight = FontWeight.Bold),
        Font(googleFont = googleFont, fontProvider = googleFontsProvider, weight = FontWeight.ExtraBold)
    )
}

/** Urbanist carries the brand; DM Sans keeps dense interface copy calm and legible. */
val SynesthesiaUrbanist: FontFamily = downloadableFamily("Urbanist")
val SynesthesiaDmSans: FontFamily = downloadableFamily("DM Sans")

private val baseTypography = Typography()

/**
 * Material 3 1.2.0 does not expose a defaultFontFamily constructor parameter.
 * Assign the two typefaces to the individual styles instead.
 */
val SynesthesiaTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = SynesthesiaUrbanist),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = SynesthesiaUrbanist),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = SynesthesiaUrbanist),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = SynesthesiaUrbanist),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = SynesthesiaUrbanist),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = SynesthesiaUrbanist),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = SynesthesiaUrbanist),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = SynesthesiaUrbanist),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = SynesthesiaUrbanist),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = SynesthesiaDmSans),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = SynesthesiaDmSans),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = SynesthesiaDmSans),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = SynesthesiaDmSans),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = SynesthesiaDmSans),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = SynesthesiaDmSans)
)

/** Softer component defaults so Material controls follow the app's rounded visual language. */
val SynesthesiaShapes = Shapes(
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(26.dp),
    large = RoundedCornerShape(34.dp)
)
