package uz.wegrow.invest.ui.screens.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.wegrow.invest.data.CourseItem
import uz.wegrow.invest.data.CourseLessonSummary
import uz.wegrow.invest.data.LessonDetail
import uz.wegrow.invest.data.PaymentOrderInfo
import uz.wegrow.invest.ui.AppStrings
import uz.wegrow.invest.ui.components.AnimatedProgressBar
import uz.wegrow.invest.ui.components.CourseWaveformPreview
import uz.wegrow.invest.ui.components.StaggeredReveal
import uz.wegrow.invest.ui.components.WeGrowButton
import uz.wegrow.invest.ui.components.WeGrowChip
import uz.wegrow.invest.ui.components.WeGrowMarkdownText
import uz.wegrow.invest.ui.components.WeGrowTopBar
import uz.wegrow.invest.ui.components.bouncyClickable
import uz.wegrow.invest.ui.theme.WeGrowRadius
import uz.wegrow.invest.ui.theme.WeGrowTextStyles
import uz.wegrow.invest.ui.theme.WeGrowTheme

@Composable
fun CourseLmsScreen(
    course: CourseItem,
    lessons: List<CourseLessonSummary>,
    activeLesson: LessonDetail?,
    pendingOrder: PaymentOrderInfo?,
    strings: AppStrings,
    onOpenLesson: (CourseLessonSummary) -> Unit,
    onCloseLesson: () -> Unit,
    onCompleteLesson: (lessonId: Int, quizAnswers: Map<String, Int>?) -> Unit,
    onStartOrder: (provider: String) -> Unit,
    onSandboxPay: (orderUid: String) -> Unit,
    onOpenCertificate: () -> Unit = {}
) {
    if (activeLesson != null) {
        LessonReaderView(
            lesson = activeLesson,
            strings = strings,
            onBack = onCloseLesson,
            onSubmit = { answers -> onCompleteLesson(activeLesson.id, answers) }
        )
        return
    }

    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    var isPlayingPreview by remember { mutableStateOf(false) }
    val completedCount = lessons.count { it.completed }
    val progressFrac = if (lessons.isNotEmpty()) completedCount.toFloat() / lessons.size.toFloat() else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Unified Figcraft Course Hero Header (Rating + Author + Progress)
        item(key = "lms_header") {
            StaggeredReveal(0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WeGrowRadius.full))
                                .background(colors.brandSoft)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isUz) course.levelUz else course.level,
                                style = WeGrowTextStyles.CaptionStrong,
                                color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "★ ", color = colors.saffron, fontSize = 15.sp)
                            Text(text = "${course.rating}", style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "(${course.reviewsCount})", style = WeGrowTextStyles.CaptionM, color = colors.textTertiary)
                        }
                    }

                    Text(
                        text = if (isUz) course.titleUz else course.title,
                        style = WeGrowTextStyles.HeadlineM,
                        color = colors.textPrimary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.brandSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "WG",
                                style = WeGrowTextStyles.CaptionStrong,
                                color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isUz) course.authorUz else course.author,
                                style = WeGrowTextStyles.TitleS,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (isUz) course.authorRoleUz else course.authorRole,
                                style = WeGrowTextStyles.CaptionM,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isUz) "Jarayon: $completedCount / ${lessons.size} dars" else "Прогресс: $completedCount / ${lessons.size} уроков",
                            style = WeGrowTextStyles.CaptionStrong,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "${(progressFrac * 100).toInt()}%",
                            style = WeGrowTextStyles.CaptionStrong,
                            color = colors.brandPrimary
                        )
                    }
                    AnimatedProgressBar(
                        fraction = progressFrac,
                        barColor = colors.brandPrimary,
                        trackColor = colors.surfaceMuted
                    )
                }
            }
        }

        // Restored Figcraft Audio/Video Waveform Preview Card
        item(key = "lms_waveform_preview") {
            StaggeredReveal(1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .bouncyClickable(haptic = true) { isPlayingPreview = !isPlayingPreview }
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(colors.brandPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPlayingPreview) "❚❚" else "▶",
                            color = colors.textOnBrand,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = strings.coursePreview, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                        Text(text = course.previewDuration, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
                    }
                    CourseWaveformPreview(
                        isPlaying = isPlayingPreview,
                        barColor = colors.textTertiary,
                        activeColor = colors.brandPrimary
                    )
                }
            }
        }

        // Restored 3 Figcraft Stat Tiles (Certificate tile opens CertificateScreen!)
        item(key = "lms_stats") {
            StaggeredReveal(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LmsStatTile(
                        icon = "📚",
                        title = if (isUz) "${course.modulesCount} modul" else "${course.modulesCount} модулей",
                        caption = if (isUz) "${lessons.size} faol dars" else "${lessons.size} уроков",
                        modifier = Modifier.weight(1f)
                    )
                    LmsStatTile(
                        icon = "🕒",
                        title = if (isUz) course.accessDurationUz else course.accessDuration,
                        caption = if (isUz) "kirish huquqi" else "доступ",
                        modifier = Modifier.weight(1f)
                    )
                    LmsStatTile(
                        icon = "🏅",
                        title = if (isUz) "Sertifikat" else "Сертификат",
                        caption = if (isUz) "ochish →" else "открыть →",
                        highlighted = true,
                        onClick = onOpenCertificate,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (!course.purchased) {
            item(key = "lms_payment_box") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(1.dp, colors.brandPrimary, RoundedCornerShape(WeGrowRadius.xl))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isUz) "To'liq kirish (${course.accessDurationUz})" else "Полный доступ (${course.accessDuration})",
                            style = WeGrowTextStyles.TitleS,
                            color = colors.textPrimary
                        )
                        Text(
                            text = course.priceUzs,
                            style = WeGrowTextStyles.TitleM,
                            color = colors.brandPrimary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        WeGrowButton(
                            text = "Payme",
                            onClick = { onStartOrder("PAYME") },
                            modifier = Modifier.weight(1f)
                        )
                        WeGrowButton(
                            text = "Click",
                            onClick = { onStartOrder("CLICK") },
                            outlined = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pendingOrder != null && pendingOrder.status == "PENDING") {
                        Text(
                            text = "${if (isUz) "Buyurtma" else "Заказ"} ${pendingOrder.orderUid} (${pendingOrder.provider})",
                            style = WeGrowTextStyles.CaptionStrong,
                            color = colors.textSecondary
                        )
                        if (pendingOrder.sandboxEnabled) {
                            WeGrowButton(
                                text = if (isUz) "Sandbox to'lovni tasdiqlash ✓" else "Подтвердить тестовую оплату (Sandbox) ✓",
                                onClick = { onSandboxPay(pendingOrder.orderUid) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        item(key = "lms_lessons_title") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUz) "Kurs darslari" else "Уроки курса",
                    style = WeGrowTextStyles.TitleL,
                    color = colors.textPrimary
                )
                Text(
                    text = if (isUz) "Sertifikat 🏅" else "Сертификат 🏅",
                    style = WeGrowTextStyles.LabelM,
                    color = colors.brandPrimary,
                    modifier = Modifier.bouncyClickable(haptic = true, onClick = onOpenCertificate)
                )
            }
        }

        items(lessons, key = { "les_${it.id}" }) { lesson ->
            val icon = when {
                lesson.completed -> "✓"
                lesson.locked -> "🔒"
                lesson.lessonType == "video" -> "▶"
                lesson.lessonType == "quiz" -> "?"
                else -> "📄"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable(enabled = !lesson.locked, haptic = true) { onOpenLesson(lesson) }
                    .clip(RoundedCornerShape(WeGrowRadius.lg))
                    .background(colors.surface)
                    .border(
                        1.dp,
                        if (lesson.completed) colors.statusHalal else colors.border,
                        RoundedCornerShape(WeGrowRadius.lg)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (lesson.locked) colors.surfaceMuted else colors.brandSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        style = WeGrowTextStyles.TitleS,
                        color = if (lesson.locked) colors.textTertiary else colors.brandPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUz) lesson.titleUz else lesson.title,
                        style = WeGrowTextStyles.TitleS,
                        color = if (lesson.locked) colors.textSecondary else colors.textPrimary
                    )
                    val tag = if (lesson.isPreview) {
                        if (isUz) "Bepul prevyu" else "Бесплатно"
                    } else {
                        "${if (isUz) "Modul" else "Модуль"} ${lesson.moduleNumber}"
                    }
                    val scoreStr = lesson.quizScore?.let { " · Тест $it%" } ?: ""
                    Text(
                        text = "$tag · ${lesson.durationMin} мин$scoreStr",
                        style = WeGrowTextStyles.CaptionM,
                        color = colors.textTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun LmsStatTile(
    icon: String,
    title: String,
    caption: String,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = WeGrowTheme.colors
    Column(
        modifier = modifier
            .height(86.dp)
            .then(if (onClick != null) Modifier.bouncyClickable(haptic = true, onClick = onClick) else Modifier)
            .clip(RoundedCornerShape(WeGrowRadius.lg))
            .background(if (highlighted) colors.brandSoft else colors.surface)
            .border(
                1.dp,
                if (highlighted) colors.brandPrimary.copy(alpha = 0.6f) else colors.border,
                RoundedCornerShape(WeGrowRadius.lg)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = icon, fontSize = 18.sp)
        Column {
            Text(text = title, style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
            Text(
                text = caption,
                style = WeGrowTextStyles.CaptionM,
                color = if (highlighted) colors.brandPrimary else colors.textTertiary
            )
        }
    }
}

@Composable
private fun LessonReaderView(
    lesson: LessonDetail,
    strings: AppStrings,
    onBack: () -> Unit,
    onSubmit: (Map<String, Int>?) -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    val selectedAnswers = remember(lesson.id) { mutableStateMapOf<String, Int>() }
    var submitted by remember(lesson.id) { mutableStateOf(lesson.completed) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        WeGrowTopBar(
            title = if (isUz) lesson.titleUz else lesson.title,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WeGrowRadius.xl))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WeGrowMarkdownText(markdown = if (isUz) lesson.contentMdUz else lesson.contentMd)
        }

        if (lesson.quiz.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isUz) "Mini-test" else "Проверка знаний",
                    style = WeGrowTextStyles.TitleM,
                    color = colors.textPrimary
                )
                lesson.quiz.forEach { q ->
                    Text(
                        text = if (isUz) q.questionUz else q.question,
                        style = WeGrowTextStyles.TitleS,
                        color = colors.textPrimary
                    )
                    q.options.forEachIndexed { idx, opt ->
                        WeGrowChip(
                            label = opt,
                            selected = selectedAnswers[q.id] == idx,
                            onClick = { selectedAnswers[q.id] = idx },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        WeGrowButton(
            text = if (submitted) {
                if (isUz) "Dars yakunlandi ✓" else "Урок пройден ✓"
            } else {
                if (isUz) "Darsni yakunlash" else "Завершить урок"
            },
            onClick = {
                submitted = true
                onSubmit(if (lesson.quiz.isNotEmpty()) selectedAnswers.toMap() else null)
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(80.dp))
    }
}
