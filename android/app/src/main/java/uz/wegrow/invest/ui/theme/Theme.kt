package uz.wegrow.invest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Semantic color tokens extracted 1:1 from WeGrow_v3_Android_2.figcraft
 * Collection: Color (Modes: Light / Dark)
 */
@Immutable
data class WeGrowColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceSolid: Color,
    val surfaceMuted: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textInverse: Color,
    val textOnBrand: Color,
    val brandPrimary: Color,
    val brandPressed: Color,
    val brandSoft: Color,
    val brandDeep: Color,
    val onDeepSecondary: Color,
    val onDeepGlass: Color,
    val onDeepPattern: Color,
    val navBg: Color,
    val hatch: Color,
    val accent: Color,
    val accentSoft: Color,
    val saffron: Color,
    val saffronSoft: Color,
    val statusHalal: Color,
    val statusHalalSoft: Color,
    val statusDoubt: Color,
    val statusDoubtSoft: Color,
    val statusHaram: Color,
    val statusHaramSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val chartSeries2: Color,
    val heatUp3: Color,
    val heatUp2: Color,
    val heatUp1: Color,
    val heatDown1: Color,
    val heatDown2: Color
)

val WeGrowDarkPalette = WeGrowColors(
    isDark = true,
    bg = Color(0xFF0B2016),
    surface = Color(0x12FFFFFF),
    surfaceSolid = Color(0xFF132C20),
    surfaceMuted = Color(0x1FFFFFFF),
    border = Color(0x21FFFFFF),
    textPrimary = Color(0xFFF1F5EC),
    textSecondary = Color(0xFFB7C6BA),
    textTertiary = Color(0xFF7E9384),
    textInverse = Color(0xFF0B2016),
    textOnBrand = Color(0xFF0B2016),
    brandPrimary = Color(0xFFC6E33B),
    brandPressed = Color(0xFFDDF07A),
    brandSoft = Color(0x24C6E33B),
    brandDeep = Color(0x0FFFFFFF),
    onDeepSecondary = Color(0xFFB7C6BA),
    onDeepGlass = Color(0x1FFFFFFF),
    onDeepPattern = Color(0x14FFFFFF),
    navBg = Color(0xF2173527),
    hatch = Color(0x14FFFFFF),
    accent = Color(0xFF3B8CF0),
    accentSoft = Color(0x333B8CF0),
    saffron = Color(0xFFF0703A),
    saffronSoft = Color(0x29F0703A),
    statusHalal = Color(0xFF57D68D),
    statusHalalSoft = Color(0x2457D68D),
    statusDoubt = Color(0xFFF2B33D),
    statusDoubtSoft = Color(0x24F2B33D),
    statusHaram = Color(0xFFF0603A),
    statusHaramSoft = Color(0x26F0603A),
    info = Color(0xFF6FA8F5),
    infoSoft = Color(0x293B8CF0),
    chartSeries2 = Color(0xFF3B8CF0),
    heatUp3 = Color(0xFFB7DD4A),
    heatUp2 = Color(0xFF8DBF4A),
    heatUp1 = Color(0x1AFFFFFF),
    heatDown1 = Color(0x2EF0603A),
    heatDown2 = Color(0xFFF0703A)
)

val WeGrowLightPalette = WeGrowColors(
    isDark = false,
    bg = Color(0xFFEEF3E2),
    surface = Color(0xFFFFFFFF),
    surfaceSolid = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFE2EAD3),
    border = Color(0xFFD3DDC2),
    textPrimary = Color(0xFF0E2318),
    textSecondary = Color(0xFF4B5E51),
    textTertiary = Color(0xFF82937F),
    textInverse = Color(0xFFFFFFFF),
    textOnBrand = Color(0xFF0B2016),
    brandPrimary = Color(0xFFB9D52C),
    brandPressed = Color(0xFF4F6A0B),
    brandSoft = Color(0xFFE5F0B5),
    brandDeep = Color(0xFF1C3A28),
    onDeepSecondary = Color(0xFFB7C6BA),
    onDeepGlass = Color(0x1FFFFFFF),
    onDeepPattern = Color(0x17FFFFFF),
    navBg = Color(0xF0FFFFFF),
    hatch = Color(0x140E2318),
    accent = Color(0xFF2F7BE0),
    accentSoft = Color(0xFFDCE9FB),
    saffron = Color(0xFFE07B2E),
    saffronSoft = Color(0xFFFBE3D2),
    statusHalal = Color(0xFF1E9E5A),
    statusHalalSoft = Color(0xFFDDF3E6),
    statusDoubt = Color(0xFFC98A12),
    statusDoubtSoft = Color(0xFFFBEFD3),
    statusHaram = Color(0xFFDC4B2A),
    statusHaramSoft = Color(0xFFFBE1D9),
    info = Color(0xFF2F7BE0),
    infoSoft = Color(0xFFDCE9FB),
    chartSeries2 = Color(0xFF2F7BE0),
    heatUp3 = Color(0xFF8CC63F),
    heatUp2 = Color(0xFFB9D97A),
    heatUp1 = Color(0xFFDDEBC4),
    heatDown1 = Color(0xFFF7D9CF),
    heatDown2 = Color(0xFFE8724F)
)

object WeGrowSpacing {
    val space4: Dp = 4.dp
    val space8: Dp = 8.dp
    val space12: Dp = 12.dp
    val space16: Dp = 16.dp
    val space20: Dp = 20.dp
    val space24: Dp = 24.dp
    val space32: Dp = 32.dp
}

object WeGrowRadius {
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val full: Dp = 999.dp
}

/**
 * Typography tokens from WeGrow_v3_Android_2.figcraft (Styles S:1..S:15)
 */
object WeGrowTextStyles {
    val DisplayL = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        letterSpacing = (-1.8).sp
    )
    val DisplayM = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.2).sp
    )
    val NumericL = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1.0).sp
    )
    val HeadlineL = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.8).sp
    )
    val HeadlineM = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    )
    val TitleL = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp
    )
    val TitleM = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    )
    val TitleS = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val BodyL = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
    val BodyM = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val BodyS = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    val LabelM = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    val CaptionM = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
    val CaptionStrong = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
    val Micro = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.1.sp
    )
}

val LocalWeGrowColors = staticCompositionLocalOf { WeGrowDarkPalette }

object WeGrowTheme {
    val colors: WeGrowColors
        @Composable
        get() = LocalWeGrowColors.current
}

@Composable
fun WeGrowInvestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) WeGrowDarkPalette else WeGrowLightPalette

    val m3Colors: ColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.brandPrimary,
            onPrimary = palette.textOnBrand,
            background = palette.bg,
            onBackground = palette.textPrimary,
            surface = palette.surfaceSolid,
            onSurface = palette.textPrimary,
            secondary = palette.accent,
            error = palette.statusHaram
        )
    } else {
        lightColorScheme(
            primary = palette.brandPrimary,
            onPrimary = palette.textOnBrand,
            background = palette.bg,
            onBackground = palette.textPrimary,
            surface = palette.surfaceSolid,
            onSurface = palette.textPrimary,
            secondary = palette.accent,
            error = palette.statusHaram
        )
    }

    val typography = Typography(
        displayLarge = WeGrowTextStyles.DisplayL,
        displayMedium = WeGrowTextStyles.DisplayM,
        headlineLarge = WeGrowTextStyles.HeadlineL,
        headlineMedium = WeGrowTextStyles.HeadlineM,
        titleLarge = WeGrowTextStyles.TitleL,
        titleMedium = WeGrowTextStyles.TitleM,
        titleSmall = WeGrowTextStyles.TitleS,
        bodyLarge = WeGrowTextStyles.BodyL,
        bodyMedium = WeGrowTextStyles.BodyM,
        bodySmall = WeGrowTextStyles.BodyS,
        labelMedium = WeGrowTextStyles.LabelM,
        labelSmall = WeGrowTextStyles.CaptionM
    )

    CompositionLocalProvider(LocalWeGrowColors provides palette) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography = typography,
            content = content
        )
    }
}
