package uz.wegrow.invest.ui.screens.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.wegrow.invest.data.CourseItem
import uz.wegrow.invest.data.HalalStatus
import uz.wegrow.invest.data.UserProfile
import uz.wegrow.invest.ui.AppStrings
import uz.wegrow.invest.ui.components.AnimatedProgressBar
import uz.wegrow.invest.ui.components.HalalSeal
import uz.wegrow.invest.ui.components.KhatamPatternBackground
import uz.wegrow.invest.ui.components.StaggeredReveal
import uz.wegrow.invest.ui.components.WeGrowBrandLogo
import uz.wegrow.invest.ui.components.WeGrowButton
import uz.wegrow.invest.ui.components.WeGrowTopBar
import uz.wegrow.invest.ui.theme.WeGrowRadius
import uz.wegrow.invest.ui.theme.WeGrowTextStyles
import uz.wegrow.invest.ui.theme.WeGrowTheme

@Composable
fun CertificateScreen(
    profile: UserProfile,
    course: CourseItem,
    completedLessons: Int,
    totalLessons: Int,
    strings: AppStrings,
    onBack: () -> Unit,
    onShareNotice: () -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    val safeTotal = totalLessons.coerceAtLeast(1)
    val fraction = (completedLessons.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
    val isUnlocked = course.purchased && completedLessons >= safeTotal
    val certCode = "WG-CERT-2026-${profile.id.toString().padStart(4, '0')}-${course.id}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WeGrowTopBar(title = strings.certificateTitle, onBack = onBack)

        StaggeredReveal(0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xxl))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0E261B), Color(0xFF173A28), Color(0xFF0B2016))
                        )
                    )
                    .border(2.dp, colors.brandPrimary, RoundedCornerShape(WeGrowRadius.xxl))
            ) {
                KhatamPatternBackground(modifier = Modifier.matchParentSize())

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WeGrowBrandLogo(size = 44.dp)
                        HalalSeal(status = HalalStatus.HALAL, size = 44.dp, pulsing = isUnlocked)
                    }

                    Text(
                        text = "WEGROW ACADEMY",
                        style = WeGrowTextStyles.CaptionStrong,
                        color = colors.brandPrimary
                    )

                    Text(
                        text = if (isUz) "MUVAFFAQIYATLI YAKUNLASH SERTIFIKATI" else "СЕРТИФИКАТ О ПРОХОЖДЕНИИ КУРСА",
                        style = WeGrowTextStyles.TitleM,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isUz) "Ushbu sertifikat tasdiqlaydiki" else "Настоящим подтверждается, что",
                        style = WeGrowTextStyles.CaptionM,
                        color = colors.onDeepSecondary
                    )

                    Text(
                        text = profile.fullName.ifBlank { "Азиз Каримов" },
                        style = WeGrowTextStyles.HeadlineL,
                        color = colors.brandPrimary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isUz) "quyidagi dastur bo'yicha o'qishni yakunladi:" else "успешно освоил(а) программу обучения:",
                        style = WeGrowTextStyles.CaptionM,
                        color = colors.onDeepSecondary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isUz) course.titleUz else course.title,
                        style = WeGrowTextStyles.TitleL,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WeGrowRadius.md))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isUz) "Tekshiruv kodi" else "Код верификации",
                                style = WeGrowTextStyles.Micro,
                                color = colors.onDeepSecondary
                            )
                            Text(text = certCode, style = WeGrowTextStyles.CaptionStrong, color = Color.White)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WeGrowRadius.full))
                                .background(if (isUnlocked) colors.statusHalalSoft else colors.statusDoubtSoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isUnlocked) (if (isUz) "Tasdiqlangan ✓" else "Выдан ✓")
                                else (if (isUz) "Namuna" else "Превью"),
                                style = WeGrowTextStyles.CaptionStrong,
                                color = if (isUnlocked) colors.statusHalal else colors.statusDoubt
                            )
                        }
                    }
                }
            }
        }

        StaggeredReveal(1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isUz) "Kurs jarayoni" else "Прогресс для получения",
                        style = WeGrowTextStyles.TitleS,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "$completedLessons / $safeTotal (${(fraction * 100).toInt()}%)",
                        style = WeGrowTextStyles.CaptionStrong,
                        color = colors.brandPrimary
                    )
                }
                AnimatedProgressBar(
                    fraction = fraction,
                    barColor = colors.brandPrimary,
                    trackColor = colors.surfaceMuted
                )
                Text(
                    text = if (isUnlocked) {
                        if (isUz) "Tabriklaymiz! Barcha darslar yakunlandi va sertifikat tayyor."
                        else "Поздравляем! Все уроки пройдены — именной сертификат активен и доступен для публикации."
                    } else {
                        val left = (safeTotal - completedLessons).coerceAtLeast(0)
                        if (isUz) "Sertifikatni ochish uchun yana $left ta darsni yakunlang."
                        else "Осталось пройти уроков: $left, чтобы разблокировать официальный сертификат."
                    },
                    style = WeGrowTextStyles.CaptionM,
                    color = colors.textSecondary
                )
            }
        }

        WeGrowButton(
            text = if (isUnlocked) {
                if (isUz) "Sertifikat havolasini nusxalash ✓" else "Поделиться сертификатом ✓"
            } else {
                if (isUz) "Darslarga qaytish" else "Вернуться к урокам"
            },
            onClick = { if (isUnlocked) onShareNotice() else onBack() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
