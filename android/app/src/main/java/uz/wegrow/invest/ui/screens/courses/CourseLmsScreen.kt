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
import uz.wegrow.invest.data.CourseItem
import uz.wegrow.invest.data.CourseLessonSummary
import uz.wegrow.invest.data.LessonDetail
import uz.wegrow.invest.data.PaymentOrderInfo
import uz.wegrow.invest.ui.AppStrings
import uz.wegrow.invest.ui.components.AnimatedProgressBar
import uz.wegrow.invest.ui.components.StaggeredReveal
import uz.wegrow.invest.ui.components.WeGrowButton
import uz.wegrow.invest.ui.components.WeGrowChip
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
    onSandboxPay: (orderUid: String) -> Unit
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
    val completedCount = lessons.count { it.completed }
    val progressFrac = if (lessons.isNotEmpty()) completedCount.toFloat() / lessons.size.toFloat() else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "lms_header") {
            StaggeredReveal(0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                        .padding(18.dp)
                ) {
                    Text(
                        text = if (isUz) course.levelUz else course.level,
                        style = WeGrowTextStyles.CaptionStrong,
                        color = colors.brandPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isUz) course.titleUz else course.title,
                        style = WeGrowTextStyles.TitleL,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
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
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedProgressBar(
                        fraction = progressFrac,
                        barColor = colors.brandPrimary,
                        trackColor = colors.surfaceMuted
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
            Text(
                text = if (isUz) "Kurs darslari" else "Уроки курса",
                style = WeGrowTextStyles.TitleL,
                color = colors.textPrimary
            )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .bouncyClickable(haptic = true, onClick = onBack)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "←", style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
            }
            Text(
                text = if (isUz) lesson.titleUz else lesson.title,
                style = WeGrowTextStyles.TitleM,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WeGrowRadius.xl))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isUz) lesson.contentMdUz else lesson.contentMd,
                style = WeGrowTextStyles.BodyS,
                color = colors.textPrimary
            )
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
