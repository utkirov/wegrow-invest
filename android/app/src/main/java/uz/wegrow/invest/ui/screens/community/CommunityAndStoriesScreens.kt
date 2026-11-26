package uz.wegrow.invest.ui.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.wegrow.invest.data.StoryDeck
import uz.wegrow.invest.data.StreamSession
import uz.wegrow.invest.ui.AppStrings
import uz.wegrow.invest.ui.components.KhatamPatternBackground
import uz.wegrow.invest.ui.components.LivePulseDot
import uz.wegrow.invest.ui.components.StaggeredReveal
import uz.wegrow.invest.ui.components.WeGrowButton
import uz.wegrow.invest.ui.components.WeGrowChip
import uz.wegrow.invest.ui.components.WeGrowTopBar
import uz.wegrow.invest.ui.components.bouncyClickable
import uz.wegrow.invest.ui.theme.WeGrowRadius
import uz.wegrow.invest.ui.theme.WeGrowTextStyles
import uz.wegrow.invest.ui.theme.WeGrowTheme

/**
 * Full-screen interactive Story Viewer overlay replacing the old v3.1 AlertDialog
 */
@Composable
fun StoriesViewerOverlay(
    deck: StoryDeck,
    strings: AppStrings,
    onClose: () -> Unit,
    onActionRoute: (String) -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    var slideIndex by remember(deck.id) { mutableIntStateOf(0) }
    val slide = deck.slides.getOrElse(slideIndex) { deck.slides.first() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A1D14), Color(0xFF132F22), Color(0xFF091811))
                )
            )
    ) {
        KhatamPatternBackground(modifier = Modifier.fillMaxSize())

        // Invisible left/right tap zones for Instagram-style slide switching
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (slideIndex > 0) slideIndex-- else onClose()
                    }
            )
            Box(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (slideIndex < deck.slides.lastIndex) slideIndex++ else onClose()
                    }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Top progress segments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    deck.slides.forEachIndexed { idx, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(WeGrowRadius.full))
                                .background(
                                    if (idx <= slideIndex) colors.brandPrimary else Color.White.copy(alpha = 0.24f)
                                )
                        )
                    }
                }

                // Story header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.brandPrimary.copy(alpha = 0.2f))
                                .border(1.dp, colors.brandPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = deck.badgeSymbol, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isUz) deck.labelUz else deck.label,
                                style = WeGrowTextStyles.TitleM,
                                color = Color.White
                            )
                            Text(
                                text = "${slideIndex + 1} / ${deck.slides.size}",
                                style = WeGrowTextStyles.CaptionM,
                                color = colors.onDeepSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .bouncyClickable(haptic = true, onClick = onClose)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", color = Color.White, style = WeGrowTextStyles.TitleM)
                    }
                }
            }

            // Center card content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xxl))
                    .background(Color(0xCC12281D))
                    .border(1.dp, colors.brandPrimary.copy(alpha = 0.35f), RoundedCornerShape(WeGrowRadius.xxl))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(WeGrowRadius.full))
                        .background(colors.brandPrimary.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isUz) slide.eyebrowUz else slide.eyebrow,
                        style = WeGrowTextStyles.CaptionStrong,
                        color = colors.brandPrimary
                    )
                }
                Text(
                    text = if (isUz) slide.titleUz else slide.title,
                    style = WeGrowTextStyles.HeadlineL,
                    color = Color.White
                )
                Text(
                    text = if (isUz) slide.bodyUz else slide.body,
                    style = WeGrowTextStyles.BodyL,
                    color = colors.onDeepSecondary
                )
            }

            // Bottom CTA
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WeGrowButton(
                    text = if (isUz) slide.ctaLabelUz else slide.ctaLabel,
                    onClick = {
                        onClose()
                        onActionRoute(slide.actionRoute)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (isUz) "Keyingi slayd uchun o'ng tomonga bosing" else "Нажмите справа для следующего слайда",
                    style = WeGrowTextStyles.CaptionM,
                    color = colors.onDeepSecondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

/**
 * Premium WeGrow Gold Club Screen
 */
@Composable
fun GoldClubScreen(
    isJoined: Boolean,
    strings: AppStrings,
    onBack: () -> Unit,
    onJoinPlan: (String) -> Unit,
    onOpenStreams: () -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    var yearlyPlan by remember { mutableStateOf(true) }

    val perks = remember(isUz) {
        if (isUz) {
            listOf(
                Triple("✦", "Haftalik halol invest-g'oyalar", "AAOIFI auditi bilan AQSh bozorining kuchli kompaniyalari tahlili"),
                Triple("📊", "Choraklik auditga erta kirish", "S&P 500 maqom o'zgarishlari haqida birinchi bo'lib xabar oling"),
                Triple("🎥", "Yopiq jonli efirlar va Q&A", "Tahlilchilar bilan portfel tahlili va savol-javob sessiyalari"),
                Triple("🧾", "Zakot va poklash bo'yicha yordam", "Shaxsiy portfel uchun tayyor choraklik hisobot")
            )
        } else {
            listOf(
                Triple("✦", "Еженедельные халяль-идеи", "Глубокие разборы сильных компаний США, прошедших 4 критерия AAOIFI"),
                Triple("📊", "Ранний доступ к аудиту", "Первыми получайте уведомления о смене статуса бумаг S&P 500"),
                Triple("🎥", "Закрытые эфиры и Q&A", "Прямые разборы портфелей и ответы от CFA-аналитиков WeGrow"),
                Triple("🧾", "Поддержка по Закяту и Тазкие", "Готовая квартальная справка для очищения дивидендов")
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        WeGrowTopBar(title = strings.goldClubTitle, onBack = onBack)

        StaggeredReveal(0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xxl))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF143122), Color(0xFF1E3E29), Color(0xFF11261A))
                        )
                    )
                    .border(1.2.dp, colors.statusDoubt.copy(alpha = 0.6f), RoundedCornerShape(WeGrowRadius.xxl))
            ) {
                KhatamPatternBackground(modifier = Modifier.matchParentSize())
                Column(
                    modifier = Modifier.padding(20.dp),
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
                                .background(colors.statusDoubtSoft)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "👑 GOLD CLUB",
                                style = WeGrowTextStyles.CaptionStrong,
                                color = colors.statusDoubt
                            )
                        }
                        if (isJoined) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(WeGrowRadius.full))
                                    .background(colors.statusHalalSoft)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = if (isUz) "Faol ✓" else "Активна ✓",
                                    style = WeGrowTextStyles.CaptionStrong,
                                    color = colors.statusHalal
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isUz) "Tizimli halol investorlar yopiq klubi" else "Закрытый клуб системных халяль-инвесторов",
                        style = WeGrowTextStyles.HeadlineM,
                        color = Color.White
                    )
                    Text(
                        text = if (isUz) "Analitika, jonli efirlar va AAOIFI mutaxassislari hamjamiyati bir joyda"
                        else "Аналитика от CFA-экспертов, закрытые эфиры и сообщество осознанных инвесторов Узбекистана",
                        style = WeGrowTextStyles.BodyS,
                        color = colors.onDeepSecondary
                    )
                }
            }
        }

        perks.forEachIndexed { idx, (icon, title, subtitle) ->
            StaggeredReveal(idx + 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.lg))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(WeGrowRadius.md))
                            .background(colors.brandSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = icon, fontSize = 18.sp, color = colors.brandPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
                        Text(text = subtitle, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
                    }
                }
            }
        }

        // Plan selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlanOptionTile(
                title = if (isUz) "Oylik" else "Месяц",
                price = "190 000 сум",
                caption = if (isUz) "har oy" else "в месяц",
                selected = !yearlyPlan,
                onClick = { yearlyPlan = false },
                modifier = Modifier.weight(1f)
            )
            PlanOptionTile(
                title = if (isUz) "Yillik · −25%" else "Год · −25%",
                price = "1 690 000 сум",
                caption = if (isUz) "140 800 сум / oy" else "140 800 сум / мес",
                selected = yearlyPlan,
                onClick = { yearlyPlan = true },
                modifier = Modifier.weight(1f)
            )
        }

        WeGrowButton(
            text = when {
                isJoined -> if (isUz) "Gold Club faol · Efirlarga o'tish →" else "Подписка активна · Перейти к эфирам →"
                yearlyPlan -> if (isUz) "Gold Club ga qo'shilish (Yillik · Sandbox)" else "Вступить в Gold Club (Год · Sandbox)"
                else -> if (isUz) "Gold Club ga qo'shilish (Oylik · Sandbox)" else "Вступить в Gold Club (Месяц · Sandbox)"
            },
            onClick = {
                if (isJoined) onOpenStreams()
                else onJoinPlan(if (yearlyPlan) (if (isUz) "Yillik" else "Годовой") else (if (isUz) "Oylik" else "Месячный"))
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun PlanOptionTile(
    title: String,
    price: String,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WeGrowTheme.colors
    Column(
        modifier = modifier
            .bouncyClickable(haptic = true, onClick = onClick)
            .clip(RoundedCornerShape(WeGrowRadius.lg))
            .background(if (selected) colors.brandSoft else colors.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.brandPrimary else colors.border,
                shape = RoundedCornerShape(WeGrowRadius.lg)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = WeGrowTextStyles.CaptionStrong,
            color = if (selected) colors.brandPrimary else colors.textSecondary
        )
        Text(text = price, style = WeGrowTextStyles.TitleM, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
        Text(text = caption, style = WeGrowTextStyles.CaptionM, color = colors.textTertiary)
    }
}

/**
 * Live Streams & Recorded Webinars Screen
 */
@Composable
fun StreamsScreen(
    streams: List<StreamSession>,
    remindedIds: Set<Int>,
    goldClubJoined: Boolean,
    strings: AppStrings,
    onBack: () -> Unit,
    onToggleReminder: (Int) -> Unit,
    onOpenGoldClub: () -> Unit,
    onWatchStream: (StreamSession) -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    var filter by remember { mutableStateOf("ALL") }

    val filtered = remember(streams, filter) {
        when (filter) {
            "LIVE_UP" -> streams.filter { it.status == "LIVE" || it.status == "UPCOMING" }
            "RECORDED" -> streams.filter { it.status == "RECORDED" }
            else -> streams
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "streams_top") {
            WeGrowTopBar(title = strings.streamsTitle, onBack = onBack)
        }

        item(key = "streams_filter") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    WeGrowChip(
                        label = strings.filterAll,
                        selected = filter == "ALL",
                        onClick = { filter = "ALL" }
                    )
                }
                item {
                    WeGrowChip(
                        label = if (isUz) "Jonli / Tez orada" else "В эфире и скоро",
                        selected = filter == "LIVE_UP",
                        onClick = { filter = "LIVE_UP" }
                    )
                }
                item {
                    WeGrowChip(
                        label = if (isUz) "Yozuvlar" else "Записи",
                        selected = filter == "RECORDED",
                        onClick = { filter = "RECORDED" }
                    )
                }
            }
        }

        itemsIndexed(filtered, key = { _, s -> "stream_${s.id}" }) { idx, session ->
            val isLive = session.status == "LIVE"
            val isReminded = session.id in remindedIds
            val locked = session.isGoldClub && !goldClubJoined

            StaggeredReveal(idx) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(
                            width = if (isLive) 1.5.dp else 1.dp,
                            color = if (isLive) colors.statusHalal else colors.border,
                            shape = RoundedCornerShape(WeGrowRadius.xl)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLive) {
                                LivePulseDot(color = colors.statusHalal, size = 8.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (isUz) session.dateLabelUz else session.dateLabel,
                                style = WeGrowTextStyles.CaptionStrong,
                                color = if (isLive) colors.statusHalal else colors.brandPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WeGrowRadius.full))
                                .background(if (session.isGoldClub) colors.statusDoubtSoft else colors.surfaceMuted)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (session.isGoldClub) "👑 Gold Club" else (if (isUz) session.topicTagUz else session.topicTag),
                                style = WeGrowTextStyles.CaptionStrong,
                                color = if (session.isGoldClub) colors.statusDoubt else colors.textSecondary
                            )
                        }
                    }

                    Text(
                        text = if (isUz) session.titleUz else session.title,
                        style = WeGrowTextStyles.TitleM,
                        color = colors.textPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = session.speaker, style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
                            Text(
                                text = "${if (isUz) session.speakerRoleUz else session.speakerRole} · ${session.durationMin} ${if (isUz) "daq" else "мин"}",
                                style = WeGrowTextStyles.CaptionM,
                                color = colors.textSecondary
                            )
                        }

                        when {
                            locked -> {
                                WeGrowChip(
                                    label = if (isUz) "Gold Club 🔒" else "Открыть 👑",
                                    selected = false,
                                    onClick = onOpenGoldClub
                                )
                            }
                            session.status == "UPCOMING" -> {
                                WeGrowChip(
                                    label = if (isReminded) (if (isUz) "Eslatma ✓" else "Напомню ✓") else (if (isUz) "Eslatish 🔔" else "Напомнить 🔔"),
                                    selected = isReminded,
                                    onClick = { onToggleReminder(session.id) }
                                )
                            }
                            else -> {
                                WeGrowChip(
                                    label = if (isLive) (if (isUz) "Kirish ▶" else "Смотреть ▶") else (if (isUz) "Yozuv ▶" else "Запись ▶"),
                                    selected = isLive,
                                    onClick = { onWatchStream(session) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
