package com.synesthesia.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
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

val SynesthesiaTypography = Typography(defaultFontFamily = SynesthesiaDmSans)

/** Softer component defaults so Material controls follow the app's rounded visual language. */
val SynesthesiaShapes = Shapes(
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(26.dp),
    large = RoundedCornerShape(34.dp)
)
