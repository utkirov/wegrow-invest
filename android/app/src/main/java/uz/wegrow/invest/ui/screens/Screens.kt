package uz.wegrow.invest.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.wegrow.invest.data.*
import uz.wegrow.invest.ui.AppStrings
import uz.wegrow.invest.ui.components.*
import uz.wegrow.invest.ui.theme.*
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Screen 01: Выбор языка (Language Selection)
 */
@Composable
fun LanguageSelectionScreen(
    currentLang: String,
    strings: AppStrings,
    onSelectLanguage: (String) -> Unit,
    onContinue: () -> Unit
) {
    val colors = WeGrowTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        StaggeredReveal(0) {
            WeGrowBrandLogo(size = 64.dp)
        }
        Spacer(modifier = Modifier.height(20.dp))
        StaggeredReveal(1) {
            Column {
                Text(
                    text = "WeGrow\nInvest",
                    style = WeGrowTextStyles.DisplayL,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = strings.taglineUz, style = WeGrowTextStyles.TitleM, color = colors.brandPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = strings.taglineRu, style = WeGrowTextStyles.BodyM, color = colors.textSecondary)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        StaggeredReveal(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeGrowChip(label = strings.featHalal, selected = false, onClick = {})
                WeGrowChip(label = strings.featCourses, selected = false, onClick = {})
                WeGrowChip(label = strings.featGoals, selected = false, onClick = {})
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        StaggeredReveal(3) {
            Column {
                Text(text = strings.chooseLanguage, style = WeGrowTextStyles.HeadlineM, color = colors.textPrimary)
                Text(text = strings.chooseLanguageSub, style = WeGrowTextStyles.BodyM, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(16.dp))

                LanguageOptionCard(
                    flagEmoji = "🇺🇿",
                    title = "O'zbekcha",
                    subtitle = "O'zbek tili (Lotin)",
                    selected = currentLang == "uz",
                    onClick = { onSelectLanguage("uz") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                LanguageOptionCard(
                    flagEmoji = "🇷🇺",
                    title = "Русский",
                    subtitle = "Русский язык",
                    selected = currentLang == "ru",
                    onClick = { onSelectLanguage("ru") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = strings.canChangeInProfile,
            style = WeGrowTextStyles.CaptionM,
            color = colors.textTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        WeGrowButton(
            text = strings.btnContinue,
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LanguageOptionCard(
    flagEmoji: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = WeGrowTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .bouncyClickable(haptic = true, onClick = onClick)
            .clip(RoundedCornerShape(WeGrowRadius.lg))
            .background(if (selected) colors.brandSoft else colors.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.brandPrimary else colors.border,
                shape = RoundedCornerShape(WeGrowRadius.lg)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.surfaceMuted),
            contentAlignment = Alignment.Center
        ) {
            Text(text = flagEmoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
            Text(text = subtitle, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) colors.brandPrimary else colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(colors.brandPrimary)
                )
            }
        }
    }
}

/**
 * Screen 02: Онбординг 1..3 (3-step interactive onboarding with NVDA card & pulsing HalalSeal)
 */
@Composable
fun OnboardingScreen(
    stock: StockItem,
    strings: AppStrings,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    val colors = WeGrowTheme.colors
    var step by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (step) {
                    1 -> strings.step1of3
                    2 -> strings.step2of3
                    else -> strings.step3of3
                },
                style = WeGrowTextStyles.LabelM,
                color = colors.textSecondary
            )
            WeGrowChip(label = strings.btnSkip, selected = false, onClick = onSkip)
        }

        Spacer(modifier = Modifier.height(20.dp))

        StaggeredReveal(0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.brandSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "N", style = WeGrowTextStyles.TitleM, color = colors.brandPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "NVDA", style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                        Text(text = "NVIDIA Corp.", style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
                    }
                    HalalSeal(status = HalalStatus.HALAL, size = 48.dp, pulsing = true)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = "$134,20", style = WeGrowTextStyles.NumericL, color = colors.textPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(WeGrowRadius.full))
                            .background(colors.statusHalalSoft)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "+3,42%", style = WeGrowTextStyles.LabelM, color = colors.statusHalal)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                InteractivePriceChart(points = stock.chart1m)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                Text(text = strings.criteria4of4, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                CriteriaCheckRow("Деятельность", "допустимо")
                CriteriaCheckRow("Долговая нагрузка", "9,8% из 30%")
                CriteriaCheckRow("Процентные депозиты", "4,6% из 30%")
                CriteriaCheckRow("Недопустимый доход", "0,9% из 5%")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 1..3) {
                Box(
                    modifier = Modifier
                        .width(if (step == i) 20.dp else 8.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(WeGrowRadius.full))
                        .background(if (step == i) colors.brandPrimary else colors.surfaceMuted)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (step) {
                1 -> strings.onboarding1Title
                2 -> strings.onboarding2Title
                else -> strings.onboarding3Title
            },
            style = WeGrowTextStyles.HeadlineL,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (step) {
                1 -> strings.onboarding1Subtitle
                2 -> strings.onboarding2Subtitle
                else -> strings.onboarding3Subtitle
            },
            style = WeGrowTextStyles.BodyM,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HalalBadge(status = HalalStatus.HALAL, strings = strings, small = true)
            HalalBadge(status = HalalStatus.DOUBTFUL, strings = strings, small = true)
            HalalBadge(status = HalalStatus.HARAM, strings = strings, small = true)
        }

        Spacer(modifier = Modifier.height(24.dp))
        WeGrowButton(
            text = if (step < 3) strings.btnNext else strings.btnContinue,
            onClick = {
                if (step < 3) step++ else onFinish()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CriteriaCheckRow(label: String, value: String) {
    val colors = WeGrowTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "✓", color = colors.statusHalal, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
        }
        Text(text = value, style = WeGrowTextStyles.CaptionStrong, color = colors.textPrimary)
    }
}

/**
 * Screen 05: Анкета (Profile Questionnaire — direct registration without SMS verification)
 */
@Composable
fun ProfileQuestionnaireScreen(
    profile: UserProfile,
    strings: AppStrings,
    onSave: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    val colors = WeGrowTheme.colors
    var lastName by remember(profile) { mutableStateOf(profile.lastName) }
    var firstName by remember(profile) { mutableStateOf(profile.firstName) }
    var middleName by remember(profile) { mutableStateOf(profile.middleName) }
    var birthDate by remember(profile) { mutableStateOf(profile.birthDate) }
    var gender by remember(profile) { mutableStateOf(profile.gender) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .bouncyClickable(haptic = true, onClick = onBack)
                    .clip(CircleShape)
                    .background(colors.surfaceMuted),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "←", style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = strings.editProfileBtn,
                style = WeGrowTextStyles.TitleL,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(WeGrowRadius.full))
                        .background(colors.brandPrimary)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = strings.profileFormTitle, style = WeGrowTextStyles.HeadlineL, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = strings.profileFormSubtitle, style = WeGrowTextStyles.BodyM, color = colors.textSecondary)

        Spacer(modifier = Modifier.height(20.dp))
        WeGrowTextField(label = strings.fieldLastName, value = lastName, onValueChange = { lastName = it })
        Spacer(modifier = Modifier.height(12.dp))
        WeGrowTextField(label = strings.fieldFirstName, value = firstName, onValueChange = { firstName = it })
        Spacer(modifier = Modifier.height(12.dp))
        WeGrowTextField(label = strings.fieldMiddleName, value = middleName, onValueChange = { middleName = it })
        Spacer(modifier = Modifier.height(12.dp))
        WeGrowTextField(
            label = strings.fieldBirthDate,
            value = birthDate,
            onValueChange = { birthDate = it },
            helper = strings.fieldBirthHelper
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = strings.fieldGender, style = WeGrowTextStyles.LabelM, color = colors.textSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GenderChoiceCard(
                label = strings.genderMale,
                selected = gender == "male",
                onClick = { gender = "male" },
                modifier = Modifier.weight(1f)
            )
            GenderChoiceCard(
                label = strings.genderFemale,
                selected = gender == "female",
                onClick = { gender = "female" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
        WeGrowButton(
            text = strings.btnContinue,
            onClick = {
                onSave(
                    profile.copy(
                        firstName = firstName.ifBlank { "Азиз" },
                        lastName = lastName.ifBlank { "Каримов" },
                        middleName = middleName,
                        birthDate = birthDate,
                        gender = gender
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun WeGrowTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    helper: String? = null
) {
    val colors = WeGrowTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = WeGrowTextStyles.LabelM, color = colors.textSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = RoundedCornerShape(WeGrowRadius.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.brandPrimary,
                unfocusedBorderColor = colors.border,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (helper != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = helper, style = WeGrowTextStyles.CaptionM, color = colors.textTertiary)
        }
    }
}

@Composable
private fun GenderChoiceCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WeGrowTheme.colors
    Row(
        modifier = modifier
            .height(60.dp)
            .bouncyClickable(haptic = true, onClick = onClick)
            .clip(RoundedCornerShape(WeGrowRadius.lg))
            .background(if (selected) colors.brandSoft else colors.surface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.brandPrimary else colors.border,
                shape = RoundedCornerShape(WeGrowRadius.lg)
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) colors.brandPrimary else colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.brandPrimary)
                )
            }
        }
    }
}

/**
 * Screens 06 & 13: Главная (LazyColumn MVP with staggered reveal, animated numbers & quick theme/lang toggles)
 */
@Composable
fun HomeScreen(
    profile: UserProfile,
    stocks: List<StockItem>,
    goals: List<FinancialGoal>,
    news: List<NewsItem>,
    online: Boolean,
    strings: AppStrings,
    onOpenStock: (StockItem) -> Unit,
    onOpenGoals: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenMarket: () -> Unit,
    onOpenCourses: () -> Unit,
    onOpenHalalAnalysis: (StockItem) -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    var searchQuery by remember { mutableStateOf("") }
    var activeStoryDialog by remember { mutableStateOf<String?>(null) }

    val totalSaved = remember(goals) { goals.sumOf { it.currentAmount } }
    val searchMatches = remember(searchQuery, stocks) {
        if (searchQuery.isBlank()) emptyList()
        else stocks.filter {
            it.symbol.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    if (activeStoryDialog != null) {
        AlertDialog(
            onDismissRequest = { activeStoryDialog = null },
            containerColor = colors.surfaceSolid,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = { Text(text = activeStoryDialog ?: "", style = WeGrowTextStyles.TitleL) },
            text = {
                Text(
                    text = if (isUz)
                        "WeGrow Invest 3.1 MVP: AQSh aksiyalari halol skriningi, moliyaviy maqsadlar va amaliy investitsiya kurslari."
                    else
                        "WeGrow Invest 3.1 MVP: Ежеквартальный пересмотр акций США по стандарту AAOIFI и методике WeGrow, аналитика и прямые эфиры.",
                    style = WeGrowTextStyles.BodyM
                )
            },
            confirmButton = {
                TextButton(onClick = { activeStoryDialog = null }) {
                    Text(text = "OK", color = colors.brandPrimary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.brandPrimary.copy(alpha = if (colors.isDark) 0.08f else 0.14f),
                        colors.bg,
                        colors.bg
                    )
                )
            ),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 112.dp)
    ) {
        item(key = "home_header") {
            StaggeredReveal(0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.brandSoft)
                            .border(1.dp, colors.brandPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.initials,
                            style = WeGrowTextStyles.TitleM,
                            color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = strings.greeting, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
                        Text(
                            text = profile.fullName,
                            style = WeGrowTextStyles.TitleL,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Quick RU / UZ pill for 1-tap MVP switching
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .bouncyClickable(haptic = true, onClick = onToggleLanguage)
                            .clip(RoundedCornerShape(WeGrowRadius.full))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.full))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isUz) "UZ" else "RU",
                            style = WeGrowTextStyles.CaptionStrong,
                            color = colors.brandPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Quick Light/Dark theme toggle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .bouncyClickable(haptic = true, onClick = onToggleTheme)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (colors.isDark) "☀" else "🌙", fontSize = 17.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔔", fontSize = 16.sp)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(7.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (online) colors.statusHalal else colors.saffron)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item(key = "home_stories") {
            StaggeredReveal(1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        StoryRingItem(strings.storyNews, seen = false, badgeSymbol = "📰") {
                            activeStoryDialog = strings.storyNews
                        }
                    }
                    item {
                        StoryRingItem(strings.storyCourses, seen = false, badgeSymbol = "🎓") {
                            onOpenCourses()
                        }
                    }
                    item {
                        StoryRingItem(strings.storyHalal, seen = false, badgeSymbol = "✦") {
                            stocks.firstOrNull()?.let { onOpenHalalAnalysis(it) }
                        }
                    }
                    item {
                        StoryRingItem(strings.storyGoldClub, seen = true, badgeSymbol = "👑") {
                            activeStoryDialog = strings.storyGoldClub
                        }
                    }
                    item {
                        StoryRingItem(strings.storyStreams, seen = true, badgeSymbol = "▶") {
                            activeStoryDialog = strings.storyStreams
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item(key = "home_hero_goals") {
            StaggeredReveal(2) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(text = strings.savedForGoals, style = WeGrowTextStyles.TitleM, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedNumberText(
                                targetValue = totalSaved,
                                formatter = { "$${String.format("%,.2f", it).replace(',', ' ')}" },
                                style = WeGrowTextStyles.DisplayM,
                                color = colors.textPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "✦ ", color = colors.brandPrimary, fontSize = 14.sp)
                            Text(
                                text = if (isUz) "Rejadan 2 hafta\noldindasiz" else "Опережаете план\nна 2 недели",
                                style = WeGrowTextStyles.CaptionM,
                                color = colors.textSecondary,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WeGrowRadius.full))
                                .background(colors.statusHalalSoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(text = "+12,8%", style = WeGrowTextStyles.LabelM, color = colors.statusHalal)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = strings.savedInNovember, style = WeGrowTextStyles.BodyM, color = colors.textSecondary)
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { WeGrowChip(label = strings.chipGoals, selected = true, onClick = onOpenGoals) }
                        item {
                            WeGrowChip(
                                label = strings.chipExpenses,
                                selected = false,
                                onClick = { stocks.firstOrNull()?.let { onOpenHalalAnalysis(it) } }
                            )
                        }
                        item { WeGrowChip(label = strings.chipCalculator, selected = false, onClick = onOpenCalculator) }
                        item { WeGrowChip(label = strings.chipBrokers, selected = false, onClick = onOpenMarket) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }

        item(key = "home_halal_scanner") {
            StaggeredReveal(3) {
                val deepBg = if (colors.isDark) Color(0xFF153123) else colors.brandDeep
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.xxl))
                        .background(deepBg)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xxl))
                ) {
                    KhatamPatternBackground(modifier = Modifier.matchParentSize())

                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.halalScannerTitle,
                                    style = WeGrowTextStyles.TitleL,
                                    color = Color(0xFFF1F5EC)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = strings.halalScannerSub,
                                    style = WeGrowTextStyles.CaptionM,
                                    color = colors.onDeepSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .bouncyClickable(haptic = true, onClick = onOpenMarket)
                                    .clip(CircleShape)
                                    .background(colors.onDeepGlass),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "→", color = Color.White, fontSize = 18.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = strings.searchPlaceholder,
                                    style = WeGrowTextStyles.BodyM,
                                    color = colors.onDeepSecondary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(WeGrowRadius.lg),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.onDeepGlass,
                                unfocusedContainerColor = colors.onDeepGlass,
                                focusedBorderColor = colors.brandPrimary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnimatedVisibility(
                            visible = searchMatches.isNotEmpty(),
                            enter = fadeIn(tween(180)) + expandVertically(),
                            exit = fadeOut(tween(140)) + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                searchMatches.forEach { st ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bouncyClickable(haptic = true) { onOpenStock(st) }
                                            .clip(RoundedCornerShape(WeGrowRadius.md))
                                            .background(colors.onDeepGlass)
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${st.symbol} · ${st.name}",
                                            style = WeGrowTextStyles.TitleS,
                                            color = Color.White
                                        )
                                        HalalBadge(status = st.halalStatus, strings = strings, small = true)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = strings.recentLabel,
                                style = WeGrowTextStyles.CaptionM,
                                color = colors.onDeepSecondary
                            )
                            stocks.take(3).forEach { st ->
                                Row(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .bouncyClickable(haptic = true) { onOpenStock(st) }
                                        .clip(RoundedCornerShape(WeGrowRadius.full))
                                        .background(colors.onDeepGlass)
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HalalSeal(status = st.halalStatus, size = 16.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = st.symbol,
                                        style = WeGrowTextStyles.LabelM,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item(key = "home_news_header") {
            StaggeredReveal(4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.marketNewsTitle, style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
                    Text(
                        text = strings.seeAll,
                        style = WeGrowTextStyles.LabelM,
                        color = colors.brandPrimary,
                        modifier = Modifier.bouncyClickable(haptic = true, onClick = onOpenMarket)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        itemsIndexed(news, key = { _, item -> "news_${item.id}" }) { idx, item ->
            StaggeredReveal(4 + idx) {
                NewsCardItem(
                    item = item,
                    isUz = isUz,
                    onClick = {
                        stocks.find { it.symbol == item.ticker }?.let(onOpenStock)
                    }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun NewsCardItem(
    item: NewsItem,
    isUz: Boolean,
    onClick: () -> Unit
) {
    val colors = WeGrowTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(haptic = true, onClick = onClick)
            .clip(RoundedCornerShape(WeGrowRadius.lg))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(WeGrowRadius.md))
                .background(colors.brandSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.ticker,
                style = WeGrowTextStyles.TitleM,
                color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isUz) item.titleUz else item.title,
                style = WeGrowTextStyles.TitleS,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isUz) item.sourceUz else item.source,
                style = WeGrowTextStyles.CaptionM,
                color = colors.textTertiary
            )
        }
    }
}

/**
 * Screen 07: Карточка тикера (Ticker Detail Screen)
 */
@Composable
fun TickerDetailScreen(
    stock: StockItem,
    news: List<NewsItem>,
    strings: AppStrings,
    activeTimeframe: String = "1M",
    activeChartPoints: List<Float>? = null,
    isWatchlisted: Boolean = false,
    onToggleWatchlist: () -> Unit = {},
    onQuickAlert: () -> Unit = {},
    onSelectTimeframe: (String) -> Unit = {},
    onRefreshStock: () -> Unit = {},
    onBack: () -> Unit,
    onOpenHalalAnalysis: (StockItem) -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    val periods = remember {
        listOf(
            "1D" to "1Д",
            "1W" to "1Н",
            "1M" to "1М",
            "6M" to "6М",
            "1Y" to "1Г"
        )
    }

    val displayedChart = remember(stock, activeTimeframe, activeChartPoints) {
        activeChartPoints?.takeIf { it.size >= 2 } ?: stock.chartForTimeframe(activeTimeframe)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
            Text(text = stock.symbol, style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .bouncyClickable(haptic = true, onClick = onToggleWatchlist)
                        .clip(CircleShape)
                        .background(if (isWatchlisted) colors.brandSoft else colors.surface)
                        .border(1.dp, if (isWatchlisted) colors.brandPrimary else colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (isWatchlisted) "★" else "☆", color = colors.brandPrimary, fontSize = 16.sp)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .bouncyClickable(haptic = true, onClick = onQuickAlert)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔔", fontSize = 15.sp)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .bouncyClickable(haptic = true, onClick = onRefreshStock)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "↻", color = colors.textPrimary, fontSize = 17.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        StaggeredReveal(0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.brandSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stock.symbol.take(1),
                        style = WeGrowTextStyles.TitleL,
                        color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stock.name, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                    Text(
                        text = "${stock.exchange}, ${if (isUz) stock.sectorUz else stock.sector}",
                        style = WeGrowTextStyles.CaptionM,
                        color = colors.textSecondary
                    )
                }
                HalalSeal(
                    status = stock.halalStatus,
                    size = 48.dp,
                    pulsing = true,
                    modifier = Modifier.bouncyClickable(haptic = true) { onOpenHalalAnalysis(stock) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        StaggeredReveal(1) {
            Column {
                AnimatedNumberText(
                    targetValue = stock.price,
                    formatter = { "$${String.format("%.2f", it).replace('.', ',')}" },
                    style = WeGrowTextStyles.DisplayL,
                    color = colors.textPrimary
                )
                val isPositive = stock.changePct >= 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${if (isPositive) "+" else ""}$${String.format("%.2f", stock.changeAbs)} (${String.format("%.2f", stock.changePct)}%)",
                        style = WeGrowTextStyles.TitleM,
                        color = if (isPositive) colors.statusHalal else colors.statusHaram
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUz) "bugun" else "за сегодня",
                        style = WeGrowTextStyles.CaptionM,
                        color = colors.textTertiary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🕒 ${strings.quoteDelayNote}",
                    style = WeGrowTextStyles.CaptionM,
                    color = colors.textTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        StaggeredReveal(2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = strings.priceForMonth, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                    Text(
                        text = "${activeTimeframe.uppercase()} · ${if (stock.dataSource == "live") "LIVE" else "AAOIFI"}",
                        style = WeGrowTextStyles.CaptionM,
                        color = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                InteractivePriceChart(points = displayedChart)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            periods.forEach { (tfCode, label) ->
                val sel = activeTimeframe.equals(tfCode, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .bouncyClickable(haptic = true) { onSelectTimeframe(tfCode) }
                        .clip(RoundedCornerShape(WeGrowRadius.md))
                        .background(if (sel) colors.brandPrimary else colors.surface)
                        .border(1.dp, if (sel) colors.brandPrimary else colors.border, RoundedCornerShape(WeGrowRadius.md)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = WeGrowTextStyles.LabelM,
                        color = if (sel) colors.textOnBrand else colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        WeGrowButton(
            text = strings.openHalalAnalysis,
            onClick = { onOpenHalalAnalysis(stock) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        StaggeredReveal(3) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTileCard(strings.metricMarketCap, stock.marketCap, Modifier.weight(1f))
                    MetricTileCard(strings.metricPe, stock.peRatio, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTileCard(strings.metricDividend, stock.dividendAnnual, Modifier.weight(1f))
                    MetricTileCard(strings.metricYield, stock.dividendYield, Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        news.firstOrNull()?.let { n ->
            NewsCardItem(item = n, isUz = isUz, onClick = {})
        }
    }
}

@Composable
private fun MetricTileCard(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = WeGrowTheme.colors
    Column(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(WeGrowRadius.lg))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
        Text(text = value, style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
    }
}

/**
 * Screen 08: Халяль-анализ (Detailed Halal Screening with pulsing HalalSeal & animated progress bars)
 */
@Composable
fun HalalAnalysisScreen(
    stock: StockItem,
    strings: AppStrings,
    onBack: () -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
            Text(text = strings.halalAnalysisTitle, style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
            Spacer(modifier = Modifier.size(44.dp))
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.brandSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stock.symbol.take(1),
                    style = WeGrowTextStyles.TitleM,
                    color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stock.symbol, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                Text(text = stock.name, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
            }
            Text(
                text = "${strings.checkedOn} ${stock.checkedDate}",
                style = WeGrowTextStyles.CaptionM,
                color = colors.textTertiary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        StaggeredReveal(0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HalalSeal(status = stock.halalStatus, size = 84.dp, pulsing = true)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val verdictTitle = when (stock.halalStatus) {
                        HalalStatus.HALAL -> strings.statusHalal
                        HalalStatus.DOUBTFUL -> strings.statusDoubtful
                        HalalStatus.HARAM -> strings.statusHaram
                    }
                    val verdictColor = when (stock.halalStatus) {
                        HalalStatus.HALAL -> colors.statusHalal
                        HalalStatus.DOUBTFUL -> colors.statusDoubt
                        HalalStatus.HARAM -> colors.statusHaram
                    }
                    Text(text = verdictTitle, style = WeGrowTextStyles.DisplayM, color = verdictColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stock.verdictSummary, style = WeGrowTextStyles.BodyM, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = stock.verdictSubtitle, style = WeGrowTextStyles.CaptionM, color = colors.textTertiary)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        StaggeredReveal(1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                    .padding(16.dp)
            ) {
                Text(text = strings.revenueStructure, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                Text(text = stock.reportPeriod, style = WeGrowTextStyles.CaptionM, color = colors.textTertiary)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(WeGrowRadius.full)),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    stock.revenueSegments.forEach { seg ->
                        val segColor = when (seg.type) {
                            "halal" -> colors.statusHalal
                            "accent" -> colors.accent
                            "haram" -> colors.statusHaram
                            else -> colors.textTertiary
                        }
                        Box(
                            modifier = Modifier
                                .weight(seg.pct.coerceAtLeast(2f))
                                .fillMaxHeight()
                                .background(segColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        stock.revenueSegments.filterIndexed { i, _ -> i % 2 == 0 }.forEach { seg ->
                            SegmentLegendDot(seg, isUz)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        stock.revenueSegments.filterIndexed { i, _ -> i % 2 == 1 }.forEach { seg ->
                            SegmentLegendDot(seg, isUz)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val pairs = remember(stock.criteria) { stock.criteria.chunked(2) }
        pairs.forEachIndexed { rowIdx, rowItems ->
            StaggeredReveal(2 + rowIdx) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { crit ->
                        CriterionTileCard(crit = crit, isUz = isUz, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        StaggeredReveal(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.brandSoft)
                    .border(1.dp, colors.brandPrimary.copy(alpha = 0.4f), RoundedCornerShape(WeGrowRadius.xl))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(WeGrowRadius.md))
                        .background(colors.brandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✦", color = colors.textOnBrand, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.purificationTitle,
                        style = WeGrowTextStyles.TitleM,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isUz)
                            "Har $${stock.dividendPerQuarter} dividenddan $${stock.purificationPerDividend} xayriyaga ajrating"
                        else
                            "С дивиденда $${String.format("%.2f", stock.dividendPerQuarter)} на акцию отдайте $${String.format("%.3f", stock.purificationPerDividend)} на благотворительность",
                        style = WeGrowTextStyles.CaptionM,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "ⓘ ${strings.notInvestmentAdvice}",
            style = WeGrowTextStyles.CaptionM,
            color = colors.textTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SegmentLegendDot(seg: RevenueSegment, isUz: Boolean) {
    val colors = WeGrowTheme.colors
    val dotColor = when (seg.type) {
        "halal" -> colors.statusHalal
        "accent" -> colors.accent
        "haram" -> colors.statusHaram
        else -> colors.textTertiary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${if (isUz) seg.labelUz else seg.label} ${seg.pct}%",
            style = WeGrowTextStyles.CaptionM,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun CriterionTileCard(
    crit: HalalCriterion,
    isUz: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = WeGrowTheme.colors
    val statusColor = when (crit.status) {
        HalalStatus.HALAL -> colors.statusHalal
        HalalStatus.DOUBTFUL -> colors.statusDoubt
        HalalStatus.HARAM -> colors.statusHaram
    }
    val ratio = if (crit.limitPct > 0f) (crit.pct / crit.limitPct).coerceIn(0.08f, 1f) else 1f

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(WeGrowRadius.lg))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = crit.valueLabel, style = WeGrowTextStyles.HeadlineM, color = colors.textPrimary)
            HalalSeal(status = crit.status, size = 24.dp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isUz) crit.titleUz else crit.title,
            style = WeGrowTextStyles.CaptionStrong,
            color = colors.textPrimary
        )
        Text(
            text = if (isUz) crit.subtitleUz else crit.subtitle,
            style = WeGrowTextStyles.CaptionM,
            color = colors.textTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedProgressBar(
            fraction = ratio,
            barColor = statusColor,
            trackColor = colors.surfaceMuted,
            height = 4.dp
        )
    }
}

/**
 * Screen 09: Рынок (LazyColumn Market Screen with LivePulseDot, Halal filter, Heatmap & Top Gainers)
 */
@Composable
fun MarketScreen(
    stocks: List<StockItem>,
    indices: List<MarketIndex>,
    heatmap: List<SectorHeatTile>,
    marketOpenText: String,
    strings: AppStrings,
    onOpenStock: (StockItem) -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    var halalOnly by remember { mutableStateOf(true) }
    var selectedSector by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val availableSectors = remember(stocks) {
        stocks.map { it.sector to it.sectorUz }.distinctBy { it.first }
    }

    val filteredStocks = remember(stocks, halalOnly, selectedSector, searchQuery) {
        val q = searchQuery.trim().lowercase()
        stocks.filter { st ->
            val passHalal = !halalOnly || st.halalStatus == HalalStatus.HALAL
            val passSector = selectedSector == "ALL" || st.sector.equals(selectedSector, ignoreCase = true)
            val passQuery = q.isBlank() ||
                st.symbol.lowercase().contains(q) ||
                st.name.lowercase().contains(q) ||
                st.sector.lowercase().contains(q) ||
                st.sectorUz.lowercase().contains(q)
            passHalal && passSector && passQuery
        }
    }

    val heatmapRows = remember(heatmap) { heatmap.chunked(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 112.dp)
    ) {
        item(key = "market_header") {
            StaggeredReveal(0) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.marketTitle, style = WeGrowTextStyles.DisplayM, color = colors.textPrimary)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WeGrowRadius.full))
                                .background(colors.brandSoft)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${filteredStocks.size}/${stocks.size} ${strings.marketCountLabel}",
                                style = WeGrowTextStyles.CaptionStrong,
                                color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LivePulseDot(color = colors.statusHalal, size = 8.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = marketOpenText, style = WeGrowTextStyles.BodyS, color = colors.textSecondary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text(strings.marketSearchHint, style = WeGrowTextStyles.BodyS) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(WeGrowRadius.lg),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.brandPrimary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item(key = "sec_all") {
                            WeGrowChip(
                                label = strings.filterAll,
                                selected = selectedSector == "ALL",
                                onClick = { selectedSector = "ALL" }
                            )
                        }
                        items(availableSectors, key = { it.first }) { (secRu, secUz) ->
                            WeGrowChip(
                                label = if (isUz) secUz else secRu,
                                selected = selectedSector == secRu,
                                onClick = { selectedSector = secRu }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        item(key = "market_halal_switch") {
            StaggeredReveal(1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.lg))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HalalSeal(status = HalalStatus.HALAL, size = 28.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = strings.halalOnlyTitle, style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
                        Text(text = strings.halalOnlySub, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
                    }
                    Switch(
                        checked = halalOnly,
                        onCheckedChange = { halalOnly = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.textOnBrand,
                            checkedTrackColor = colors.brandPrimary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        item(key = "market_indices") {
            StaggeredReveal(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    indices.forEach { idx ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(WeGrowRadius.lg))
                                .background(colors.surface)
                                .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
                                .padding(12.dp)
                        ) {
                            Text(text = idx.name, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = idx.value, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = idx.changePct,
                                style = WeGrowTextStyles.CaptionStrong,
                                color = if (idx.isUp) colors.statusHalal else colors.statusHaram
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item(key = "market_heatmap") {
            StaggeredReveal(3) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.sectorHeatmapTitle, style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
                        Text(text = strings.perDay, style = WeGrowTextStyles.CaptionM, color = colors.textTertiary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        heatmapRows.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { tile ->
                                    val tileBg = when (tile.tone) {
                                        "UP_3" -> colors.heatUp3
                                        "UP_2" -> colors.heatUp2
                                        "UP_1" -> colors.heatUp1
                                        "DOWN_1" -> colors.heatDown1
                                        else -> colors.heatDown2
                                    }
                                    val darkTextOnBright = tile.tone in listOf("UP_3", "UP_2", "DOWN_2")
                                    val tileFg = if (darkTextOnBright) Color(0xFF0B2016) else colors.textPrimary
                                    Column(
                                        modifier = Modifier
                                            .weight(tile.weight)
                                            .height(64.dp)
                                            .clip(RoundedCornerShape(WeGrowRadius.md))
                                            .background(tileBg)
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isUz) tile.nameUz else tile.name,
                                            style = WeGrowTextStyles.CaptionStrong,
                                            color = tileFg,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tile.changePct,
                                            style = WeGrowTextStyles.TitleS,
                                            color = tileFg
                                        )
                                    }
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = halalOnly) {
                        Text(
                            text = "ⓘ ${strings.financeSectorHidden}",
                            style = WeGrowTextStyles.CaptionM,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item(key = "market_gainers_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = strings.topGainersTitle, style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
                Text(text = "${filteredStocks.size}", style = WeGrowTextStyles.LabelM, color = colors.brandPrimary)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        itemsIndexed(filteredStocks, key = { _, st -> "ticker_${st.symbol}" }) { idx, st ->
            StaggeredReveal(idx) {
                TickerListRow(stock = st, onClick = { onOpenStock(st) })
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun TickerListRow(
    stock: StockItem,
    onClick: () -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUp = stock.changePct >= 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .bouncyClickable(haptic = true, onClick = onClick)
            .clip(RoundedCornerShape(WeGrowRadius.lg))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.brandSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stock.symbol.take(1),
                style = WeGrowTextStyles.TitleM,
                color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stock.symbol, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                HalalSeal(status = stock.halalStatus, size = 16.dp)
            }
            Text(text = stock.name, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$${String.format("%.2f", stock.price).replace('.', ',')}",
                style = WeGrowTextStyles.TitleM,
                color = colors.textPrimary
            )
            Text(
                text = "${if (isUp) "+" else ""}${String.format("%.2f", stock.changePct)}%",
                style = WeGrowTextStyles.CaptionStrong,
                color = if (isUp) colors.statusHalal else colors.statusHaram
            )
        }
    }
}

/**
 * Screen 10: Курс (LazyColumn Course Detail with 1-Canvas CourseWaveformPreview & Payme/Click purchase)
 */
@Composable
fun CourseScreen(
    course: CourseItem,
    strings: AppStrings,
    onPurchase: (String) -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    var isPlayingPreview by remember { mutableStateOf(false) }
    var selectedProvider by remember { mutableStateOf("Payme") }
    var activeModuleDialog by remember { mutableStateOf<CourseModule?>(null) }

    if (activeModuleDialog != null) {
        val m = activeModuleDialog!!
        AlertDialog(
            onDismissRequest = { activeModuleDialog = null },
            containerColor = colors.surfaceSolid,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = { Text(text = "${m.number}. ${if (isUz) m.titleUz else m.title}") },
            text = {
                Text(
                    text = if (m.locked && !course.purchased)
                        (if (isUz) "Barcha 8 modulni ochish uchun kursni Payme yoki Click orqali xarid qiling."
                        else "Оплатите курс через Payme или Click в нижней панели, чтобы разблокировать все 42 урока и сертификат.")
                    else
                        (if (isUz) "Video-dars va konspektlar tayyor! (${m.metaUz})"
                        else "Видеоурок, рабочая тетрадь и практический чек-лист доступны (${m.meta}).")
                )
            },
            confirmButton = {
                TextButton(onClick = { activeModuleDialog = null }) {
                    Text(text = "OK", color = colors.brandPrimary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 112.dp)
    ) {
        item(key = "course_header") {
            StaggeredReveal(0) {
                Column {
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
                            Text(text = "★ ", color = colors.saffron, fontSize = 16.sp)
                            Text(text = "${course.rating}", style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "(${course.reviewsCount})", style = WeGrowTextStyles.CaptionM, color = colors.textTertiary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isUz) course.titleUz else course.title,
                        style = WeGrowTextStyles.HeadlineL,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.brandSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "WG",
                                style = WeGrowTextStyles.TitleS,
                                color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
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
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item(key = "course_preview") {
            StaggeredReveal(1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .bouncyClickable(haptic = true) { isPlayingPreview = !isPlayingPreview }
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(colors.brandPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPlayingPreview) "❚❚" else "▶",
                            color = colors.textOnBrand,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
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
            Spacer(modifier = Modifier.height(14.dp))
        }

        item(key = "course_stats") {
            StaggeredReveal(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CourseStatTile("📚", if (isUz) "8 modul" else "8 модулей", if (isUz) "42 dars" else "42 урока", Modifier.weight(1f))
                    CourseStatTile("🕒", if (isUz) "6 oy" else "6 месяцев", if (isUz) "kursga kirish" else "доступ к курсу", Modifier.weight(1f))
                    CourseStatTile("🏅", if (isUz) "Sertifikat" else "Сертификат", if (isUz) "yakunda" else "по окончании", Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item(key = "course_program_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = strings.programTitle, style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
                Text(text = strings.all8, style = WeGrowTextStyles.LabelM, color = colors.brandPrimary)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        itemsIndexed(course.modules, key = { _, mod -> "mod_${mod.number}" }) { idx, mod ->
            val isUnlocked = !mod.locked || course.purchased
            StaggeredReveal(idx) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClickable(haptic = true) { activeModuleDialog = mod }
                        .clip(RoundedCornerShape(WeGrowRadius.lg))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isUnlocked) colors.brandSoft else colors.surfaceMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${mod.number}",
                            style = WeGrowTextStyles.LabelM,
                            color = if (isUnlocked) colors.brandPrimary else colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUz) mod.titleUz else mod.title,
                            style = WeGrowTextStyles.TitleS,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isUz) mod.metaUz else mod.meta,
                            style = WeGrowTextStyles.CaptionM,
                            color = colors.textSecondary
                        )
                    }
                    Text(
                        text = if (isUnlocked) "▶" else "🔒",
                        color = if (isUnlocked) colors.brandPrimary else colors.textTertiary,
                        fontSize = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        item(key = "course_purchase_bar") {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.brandPrimary.copy(alpha = 0.5f), RoundedCornerShape(WeGrowRadius.xl))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = course.priceUzs, style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
                        Text(text = strings.accessFor6Months, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Payme", "Click").forEach { provider ->
                            val sel = selectedProvider == provider
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .bouncyClickable(haptic = true) { selectedProvider = provider }
                                    .clip(RoundedCornerShape(WeGrowRadius.sm))
                                    .background(if (sel) colors.brandSoft else colors.surfaceMuted)
                                    .border(1.dp, if (sel) colors.brandPrimary else colors.border, RoundedCornerShape(WeGrowRadius.sm))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = provider,
                                    style = WeGrowTextStyles.CaptionStrong,
                                    color = if (sel) colors.brandPrimary else colors.textSecondary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                WeGrowButton(
                    text = if (course.purchased) "✓ ${strings.coursePurchased}" else "${strings.buyCourse} ($selectedProvider)",
                    onClick = { if (!course.purchased) onPurchase(selectedProvider) },
                    enabled = !course.purchased,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CourseStatTile(icon: String, title: String, caption: String, modifier: Modifier = Modifier) {
    val colors = WeGrowTheme.colors
    Column(
        modifier = modifier
            .height(88.dp)
            .clip(RoundedCornerShape(WeGrowRadius.lg))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = icon, fontSize = 18.sp)
        Column {
            Text(text = title, style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
            Text(text = caption, style = WeGrowTextStyles.CaptionM, color = colors.textTertiary)
        }
    }
}

/**
 * Screen 11: Цели (LazyColumn Financial Goals Tracker with AnimatedNumberText & AnimatedProgressBar)
 */
@Composable
fun GoalsScreen(
    goals: List<FinancialGoal>,
    strings: AppStrings,
    onContributeGoal: (Int, Double) -> Unit,
    onCreateGoal: (String, Double, Double) -> Unit,
    onOpenCalculator: () -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    var activeTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val totalSaved = remember(goals) { goals.sumOf { it.currentAmount } }
    val totalTarget = remember(goals) { goals.sumOf { it.targetAmount }.coerceAtLeast(1.0) }
    val overallFraction = (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f)

    val visibleGoals = remember(goals, activeTab) {
        val list = if (activeTab == 0) {
            goals.filter { it.currentAmount < it.targetAmount }
        } else {
            goals.filter { it.currentAmount >= it.targetAmount }
        }
        list.ifEmpty { goals }
    }

    if (showCreateDialog) {
        var titleInput by remember { mutableStateOf("") }
        var targetInput by remember { mutableStateOf("5000") }
        var monthlyInput by remember { mutableStateOf("250") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = colors.surfaceSolid,
            titleContentColor = colors.textPrimary,
            title = { Text(text = strings.addGoalBtn) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeGrowTextField(
                        label = if (isUz) "Maqsad nomi" else "Название цели",
                        value = titleInput,
                        onValueChange = { titleInput = it }
                    )
                    WeGrowTextField(
                        label = if (isUz) "Maqsad summasi ($)" else "Целевая сумма ($)",
                        value = targetInput,
                        onValueChange = { targetInput = it }
                    )
                    WeGrowTextField(
                        label = strings.monthlyTopUp,
                        value = monthlyInput,
                        onValueChange = { monthlyInput = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = titleInput.ifBlank { if (isUz) "Yangi maqsad" else "Инвестиционный портфель" }
                        val target = targetInput.toDoubleOrNull() ?: 5000.0
                        val monthly = monthlyInput.toDoubleOrNull() ?: 250.0
                        onCreateGoal(t, target, monthly)
                        showCreateDialog = false
                    }
                ) {
                    Text(text = strings.btnContinue, color = colors.brandPrimary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 112.dp)
    ) {
        item(key = "goals_header") {
            StaggeredReveal(0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.goalsTitle, style = WeGrowTextStyles.DisplayM, color = colors.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeGrowChip(label = "🧮", selected = false, onClick = onOpenCalculator)
                        WeGrowChip(label = "+ ${strings.addGoalBtn}", selected = true, onClick = { showCreateDialog = true })
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        item(key = "goals_summary") {
            StaggeredReveal(1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = strings.accumulatedLabel, style = WeGrowTextStyles.TitleM, color = colors.textSecondary)
                        AnimatedNumberText(
                            targetValue = totalSaved,
                            formatter = { "$${it.roundToInt()}" },
                            style = WeGrowTextStyles.DisplayM,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${strings.ofAmount} $${totalTarget.roundToInt()}",
                            style = WeGrowTextStyles.BodyM,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WeGrowRadius.full))
                                .background(colors.statusHalalSoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isUz) "+$350 noyabrda" else "+$350 в ноябре",
                                style = WeGrowTextStyles.CaptionStrong,
                                color = colors.statusHalal
                            )
                        }
                    }
                    CircularRingProgress(fraction = overallFraction, size = 104.dp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item(key = "goals_tabs") {
            StaggeredReveal(2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(WeGrowRadius.md))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.md))
                        .padding(4.dp)
                ) {
                    listOf(strings.tabActiveGoals, strings.tabCompletedGoals).forEachIndexed { idx, tabTitle ->
                        val sel = activeTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .bouncyClickable(haptic = true) { activeTab = idx }
                                .clip(RoundedCornerShape(WeGrowRadius.sm))
                                .background(if (sel) colors.brandPrimary else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                style = WeGrowTextStyles.TitleS,
                                color = if (sel) colors.textOnBrand else colors.textSecondary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        itemsIndexed(visibleGoals, key = { _, g -> "goal_${g.id}" }) { idx, g ->
            StaggeredReveal(idx) {
                GoalItemCard(
                    goal = g,
                    isUz = isUz,
                    topUpLabel = strings.topUpGoalBtn,
                    onTopUp = { onContributeGoal(g.id, 100.0) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GoalItemCard(
    goal: FinancialGoal,
    isUz: Boolean,
    topUpLabel: String,
    onTopUp: () -> Unit
) {
    val colors = WeGrowTheme.colors
    val pct = (goal.progressFraction * 100).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WeGrowRadius.xl))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(WeGrowRadius.md))
                    .background(colors.brandSoft),
                contentAlignment = Alignment.Center
            ) {
                val emoji = when (goal.icon) {
                    "plane" -> "✈"
                    "laptop" -> "💻"
                    "shield-check" -> "🛡"
                    else -> "🎯"
                }
                Text(text = emoji, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isUz) goal.titleUz else goal.title,
                    style = WeGrowTextStyles.TitleM,
                    color = colors.textPrimary
                )
                Text(
                    text = "$${goal.currentAmount.roundToInt()} / $${goal.targetAmount.roundToInt()}",
                    style = WeGrowTextStyles.CaptionStrong,
                    color = colors.brandPrimary
                )
            }
            Box(
                modifier = Modifier
                    .bouncyClickable(haptic = true, onClick = onTopUp)
                    .clip(RoundedCornerShape(WeGrowRadius.full))
                    .background(colors.brandSoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = topUpLabel,
                    style = WeGrowTextStyles.CaptionStrong,
                    color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedProgressBar(
            fraction = goal.progressFraction,
            barColor = colors.brandPrimary,
            trackColor = colors.surfaceMuted,
            height = 6.dp
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isUz) goal.deadlineLabelUz else goal.deadlineLabel,
                style = WeGrowTextStyles.CaptionM,
                color = colors.textSecondary
            )
            Text(
                text = "$pct%",
                style = WeGrowTextStyles.CaptionStrong,
                color = colors.textPrimary
            )
        }
    }
}

/**
 * Screen 12: Калькулятор сложного процента (zero-jank sliders + AnimatedNumberText + CompoundGrowthChart)
 */
@Composable
fun CalculatorScreen(
    strings: AppStrings,
    onBack: () -> Unit,
    onCreateGoalFromCalc: (String, Double, Double) -> Unit
) {
    val colors = WeGrowTheme.colors
    val haptics = LocalHapticFeedback.current
    var mode by remember { mutableIntStateOf(0) }
    var initialAmount by remember { mutableFloatStateOf(1000f) }
    var monthlyTopUp by remember { mutableFloatStateOf(200f) }
    var annualYield by remember { mutableFloatStateOf(8f) }
    var years by remember { mutableFloatStateOf(10f) }
    var capIndex by remember { mutableIntStateOf(0) }

    val periodsPerYear = when (capIndex) {
        0 -> 12
        1 -> 4
        else -> 1
    }

    val yInt = years.roundToInt().coerceAtLeast(1)
    val rate = (annualYield / 100.0)

    val (totalSeries, investedSeries) = remember(initialAmount, monthlyTopUp, annualYield, yInt, periodsPerYear, mode) {
        val totals = mutableListOf<Float>()
        val invs = mutableListOf<Float>()
        for (y in 0..yInt) {
            val inv = initialAmount + monthlyTopUp * 12f * y
            invs.add(inv)
            if (y == 0) {
                totals.add(initialAmount)
            } else {
                val n = periodsPerYear.toDouble()
                val rPerPeriod = rate / n
                val totalPeriods = n * y
                val compoundPrincipal = initialAmount * (1.0 + rPerPeriod).pow(totalPeriods)
                val contribPerPeriod = monthlyTopUp * (12.0 / n)
                val compoundContrib = if (rPerPeriod > 0.00001) {
                    contribPerPeriod * (((1.0 + rPerPeriod).pow(totalPeriods) - 1.0) / rPerPeriod)
                } else {
                    contribPerPeriod * totalPeriods
                }
                val rawTotal = (compoundPrincipal + compoundContrib).toFloat()
                val adjusted = when (mode) {
                    1 -> rawTotal * 0.92f
                    2 -> (rawTotal * 0.85f).coerceAtLeast(inv)
                    else -> rawTotal
                }
                totals.add(adjusted)
            }
        }
        Pair(totals, invs)
    }

    val finalTotal = totalSeries.lastOrNull()?.toDouble() ?: 38809.0
    val finalInvested = investedSeries.lastOrNull()?.toDouble() ?: 25000.0
    val finalProfit = (finalTotal - finalInvested).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
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
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = strings.calculatorsTitle, style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { WeGrowChip(strings.calcCompound, selected = mode == 0, onClick = { mode = 0 }) }
            item { WeGrowChip(strings.calcDividends, selected = mode == 1, onClick = { mode = 1 }) }
            item { WeGrowChip(strings.calcWithdrawal, selected = mode == 2, onClick = { mode = 2 }) }
        }

        Spacer(modifier = Modifier.height(18.dp))

        CalculatorSliderItem(
            label = strings.initialAmount,
            valueLabel = "$${initialAmount.roundToInt()}",
            value = initialAmount,
            valueRange = 100f..25000f,
            onValueChange = { raw ->
                val snapped = ((raw / 100f).roundToInt() * 100f).coerceIn(100f, 25000f)
                if (snapped != initialAmount) {
                    initialAmount = snapped
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        )
        CalculatorSliderItem(
            label = strings.monthlyTopUp,
            valueLabel = "$${monthlyTopUp.roundToInt()}",
            value = monthlyTopUp,
            valueRange = 50f..2500f,
            onValueChange = { raw ->
                val snapped = ((raw / 50f).roundToInt() * 50f).coerceIn(50f, 2500f)
                if (snapped != monthlyTopUp) {
                    monthlyTopUp = snapped
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        )
        CalculatorSliderItem(
            label = strings.annualYield,
            valueLabel = "${annualYield.roundToInt()}%",
            value = annualYield,
            valueRange = 2f..25f,
            onValueChange = { raw ->
                val snapped = raw.roundToInt().toFloat().coerceIn(2f, 25f)
                if (snapped != annualYield) {
                    annualYield = snapped
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        )
        CalculatorSliderItem(
            label = strings.termYears,
            valueLabel = "${yInt} ${strings.yearsUnit}",
            value = years,
            valueRange = 1f..30f,
            onValueChange = { raw ->
                val snapped = raw.roundToInt().toFloat().coerceIn(1f, 30f)
                if (snapped != years) {
                    years = snapped
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = strings.capitalization, style = WeGrowTextStyles.LabelM, color = colors.textSecondary)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(WeGrowRadius.md))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.md))
                .padding(4.dp)
        ) {
            listOf(strings.capMonthly, strings.capQuarterly, strings.capYearly).forEachIndexed { idx, title ->
                val sel = capIndex == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .bouncyClickable(haptic = true) { capIndex = idx }
                        .clip(RoundedCornerShape(WeGrowRadius.sm))
                        .background(if (sel) colors.brandPrimary else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = WeGrowTextStyles.TitleS,
                        color = if (sel) colors.textOnBrand else colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        StaggeredReveal(0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                    .padding(16.dp)
            ) {
                Text(
                    text = "${strings.inYearsPrefix} $yInt ${strings.yearsUnit}",
                    style = WeGrowTextStyles.CaptionM,
                    color = colors.textSecondary
                )
                AnimatedNumberText(
                    targetValue = finalTotal,
                    formatter = { "$${String.format("%,.0f", it).replace(',', ' ')}" },
                    style = WeGrowTextStyles.DisplayM,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AnimatedNumberText(
                        targetValue = finalProfit,
                        formatter = { "● ${strings.profitPrefix} $${String.format("%,.0f", it).replace(',', ' ')}" },
                        style = WeGrowTextStyles.CaptionStrong,
                        color = colors.brandPrimary
                    )
                    AnimatedNumberText(
                        targetValue = finalInvested,
                        formatter = { "● ${strings.investedPrefix} $${String.format("%,.0f", it).replace(',', ' ')}" },
                        style = WeGrowTextStyles.CaptionStrong,
                        color = colors.chartSeries2
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                CompoundGrowthChart(
                    totalSeries = totalSeries,
                    investedSeries = investedSeries,
                    years = yInt
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        WeGrowButton(
            text = strings.createGoalForSum,
            onClick = {
                val title = if (strings.langCode == "uz") "Kapital ($yInt yil)" else "Капитал за $yInt лет"
                onCreateGoalFromCalc(title, finalTotal.roundToInt().toDouble(), monthlyTopUp.toDouble())
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CalculatorSliderItem(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val colors = WeGrowTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = WeGrowTextStyles.BodyM, color = colors.textSecondary)
            Text(text = valueLabel, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = colors.brandPrimary,
                activeTrackColor = colors.brandPrimary,
                inactiveTrackColor = colors.surfaceMuted
            )
        )
    }
}

/**
 * Profile & Settings Screen with LivePulseDot tunnel status, theme & language switchers
 */
@Composable
fun ProfileSettingsScreen(
    profile: UserProfile,
    serverUrl: String,
    online: Boolean,
    isDarkTheme: Boolean,
    isAuthenticated: Boolean = false,
    strings: AppStrings,
    onUpdateServerUrl: (String) -> Unit,
    onSyncNow: () -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onSwitchLanguage: (String) -> Unit,
    onOpenQuestionnaire: () -> Unit,
    onOpenCalculator: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onOpenAuth: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenPortfolio: () -> Unit = {}
) {
    val colors = WeGrowTheme.colors
    var urlInput by remember(serverUrl) { mutableStateOf(serverUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 112.dp)
    ) {
        Text(text = strings.profileTitle, style = WeGrowTextStyles.DisplayM, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        StaggeredReveal(0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(colors.brandSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.initials,
                        style = WeGrowTextStyles.TitleL,
                        color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = profile.fullName, style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
                    Text(text = profile.contactLabel, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
                }
                WeGrowChip(label = "✎", selected = false, onClick = onOpenQuestionnaire)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            WeGrowButton(
                text = strings.authSignInBtn,
                onClick = onOpenAuth,
                outlined = isAuthenticated,
                modifier = Modifier.weight(1f)
            )
            if (isAuthenticated) {
                WeGrowButton(
                    text = strings.authLogoutBtn,
                    onClick = onLogout,
                    outlined = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        StaggeredReveal(1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                    .padding(16.dp)
            ) {
                Text(text = strings.themeModeTitle, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeGrowChip(
                        label = "🌙 ${strings.darkThemeLabel}",
                        selected = isDarkTheme,
                        onClick = { onToggleTheme(true) },
                        modifier = Modifier.weight(1f)
                    )
                    WeGrowChip(
                        label = "☀ ${strings.lightThemeLabel}",
                        selected = !isDarkTheme,
                        onClick = { onToggleTheme(false) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = strings.chooseLanguage, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeGrowChip(
                        label = "🇷🇺 Русский",
                        selected = strings.langCode == "ru",
                        onClick = { onSwitchLanguage("ru") },
                        modifier = Modifier.weight(1f)
                    )
                    WeGrowChip(
                        label = "🇺🇿 O'zbekcha",
                        selected = strings.langCode == "uz",
                        onClick = { onSwitchLanguage("uz") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        StaggeredReveal(2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (online) {
                        LivePulseDot(color = colors.statusHalal, size = 8.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(colors.saffron)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.serverTunnelTitle, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (online) strings.connectedOnline else strings.offlineFallback,
                    style = WeGrowTextStyles.CaptionStrong,
                    color = if (online) colors.statusHalal else colors.saffron
                )
                Spacer(modifier = Modifier.height(10.dp))
                WeGrowTextField(
                    label = strings.serverTunnelHint,
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        onUpdateServerUrl(it)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                WeGrowButton(
                    text = strings.syncNowBtn,
                    onClick = onSyncNow,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        WeGrowButton(
            text = if (strings.langCode == "uz") "Investitsiya portfeli va poklash" else "Инвестиционный портфель и очищение",
            onClick = onOpenPortfolio,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        WeGrowButton(
            text = strings.calculatorsTitle,
            onClick = onOpenCalculator,
            outlined = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        WeGrowButton(
            text = strings.replayOnboardingBtn,
            onClick = onReplayOnboarding,
            outlined = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
