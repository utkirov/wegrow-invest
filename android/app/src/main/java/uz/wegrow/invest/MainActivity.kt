package uz.wegrow.invest

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import uz.wegrow.invest.data.*
import uz.wegrow.invest.ui.StringsRu
import uz.wegrow.invest.ui.StringsUz
import uz.wegrow.invest.ui.components.WeGrowFloatingNavBar
import uz.wegrow.invest.ui.screens.*
import uz.wegrow.invest.ui.theme.WeGrowInvestTheme
import uz.wegrow.invest.ui.theme.WeGrowTheme

enum class AppRoute {
    LANGUAGE_SELECT,
    ONBOARDING,
    QUESTIONNAIRE,
    MAIN_TABS,
    TICKER_DETAIL,
    HALAL_ANALYSIS,
    CALCULATOR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("wegrow_prefs", Context.MODE_PRIVATE)
        val savedUrl = prefs.getString("tunnel_url", BuildConfig.DEFAULT_TUNNEL_URL) ?: BuildConfig.DEFAULT_TUNNEL_URL

        setContent {
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", true)) }
            var langCode by remember { mutableStateOf(prefs.getString("lang_code", "ru") ?: "ru") }
            var tunnelUrl by remember { mutableStateOf(savedUrl) }

            // Dynamically adapt Edge-to-Edge system bar icon contrast to Dark/Light theme
            DisposableEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            val fallback = remember { WeGrowRepository.defaultFallbackData() }
            var profile by remember { mutableStateOf(fallback.profile) }
            var stocks by remember { mutableStateOf(fallback.stocks) }
            var goals by remember { mutableStateOf(fallback.goals) }
            var courses by remember { mutableStateOf(fallback.courses) }
            var news by remember { mutableStateOf(fallback.news) }
            var indices by remember { mutableStateOf(fallback.indices) }
            var heatmap by remember { mutableStateOf(fallback.heatmap) }
            var marketTextRu by remember { mutableStateOf(fallback.marketOpenTextRu) }
            var marketTextUz by remember { mutableStateOf(fallback.marketOpenTextUz) }
            var isOnline by remember { mutableStateOf(false) }

            // MVP fast startup: open directly into Home screen
            var currentRoute by remember { mutableStateOf(AppRoute.MAIN_TABS) }
            var selectedTab by remember { mutableIntStateOf(0) } // 0=Home, 1=Market, 2=Courses, 3=Profile
            var showGoalsInMain by remember { mutableStateOf(false) }
            var activeStock by remember { mutableStateOf(stocks.first()) }

            val scope = rememberCoroutineScope()
            val strings = if (langCode == "uz") StringsUz else StringsRu

            fun syncWithTunnel() {
                scope.launch {
                    val data = WeGrowRepository.fetchBootstrap(tunnelUrl)
                    stocks = data.stocks
                    goals = data.goals
                    courses = data.courses
                    news = data.news
                    indices = data.indices
                    heatmap = data.heatmap
                    marketTextRu = data.marketOpenTextRu
                    marketTextUz = data.marketOpenTextUz
                    isOnline = data.online
                    if (data.online) {
                        profile = data.profile.copy(language = langCode, darkTheme = isDarkTheme)
                    }
                    activeStock = stocks.find { it.symbol == activeStock.symbol } ?: stocks.first()
                }
            }

            LaunchedEffect(Unit) {
                syncWithTunnel()
            }

            // Predictive Back gesture handling
            BackHandler(enabled = currentRoute != AppRoute.MAIN_TABS || showGoalsInMain || selectedTab != 0) {
                when {
                    currentRoute == AppRoute.HALAL_ANALYSIS -> currentRoute = AppRoute.TICKER_DETAIL
                    currentRoute != AppRoute.MAIN_TABS -> {
                        currentRoute = if (currentRoute == AppRoute.ONBOARDING) {
                            AppRoute.LANGUAGE_SELECT
                        } else {
                            AppRoute.MAIN_TABS
                        }
                    }
                    showGoalsInMain -> showGoalsInMain = false
                    selectedTab != 0 -> selectedTab = 0
                }
            }

            WeGrowInvestTheme(darkTheme = isDarkTheme) {
                val colors = WeGrowTheme.colors
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.bg)
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    AnimatedContent(
                        targetState = currentRoute,
                        transitionSpec = {
                            val enteringSubScreen = targetState in listOf(
                                AppRoute.TICKER_DETAIL,
                                AppRoute.HALAL_ANALYSIS,
                                AppRoute.CALCULATOR,
                                AppRoute.QUESTIONNAIRE
                            )
                            val exitingSubScreen = initialState in listOf(
                                AppRoute.TICKER_DETAIL,
                                AppRoute.HALAL_ANALYSIS,
                                AppRoute.CALCULATOR,
                                AppRoute.QUESTIONNAIRE
                            )
                            when {
                                enteringSubScreen -> {
                                    (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                                        slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 6 }) togetherWith
                                        fadeOut(tween(150))
                                }
                                exitingSubScreen -> {
                                    fadeIn(tween(180)) togetherWith
                                        (fadeOut(tween(150)) +
                                            slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { it / 6 })
                                }
                                else -> {
                                    (fadeIn(tween(200)) + scaleIn(initialScale = 0.98f, animationSpec = tween(200))) togetherWith
                                        fadeOut(tween(140))
                                }
                            }
                        },
                        label = "routeTransition"
                    ) { route ->
                        when (route) {
                            AppRoute.LANGUAGE_SELECT -> {
                                LanguageSelectionScreen(
                                    currentLang = langCode,
                                    strings = strings,
                                    onSelectLanguage = { newLang ->
                                        langCode = newLang
                                        prefs.edit().putString("lang_code", newLang).apply()
                                    },
                                    onContinue = { currentRoute = AppRoute.ONBOARDING }
                                )
                            }

                            AppRoute.ONBOARDING -> {
                                val nvda = stocks.find { it.symbol == "NVDA" } ?: stocks.first()
                                OnboardingScreen(
                                    stock = nvda,
                                    strings = strings,
                                    onSkip = { currentRoute = AppRoute.MAIN_TABS },
                                    onFinish = { currentRoute = AppRoute.QUESTIONNAIRE }
                                )
                            }

                            AppRoute.QUESTIONNAIRE -> {
                                ProfileQuestionnaireScreen(
                                    profile = profile,
                                    strings = strings,
                                    onSave = { updated ->
                                        profile = updated
                                        currentRoute = AppRoute.MAIN_TABS
                                        scope.launch {
                                            WeGrowRepository.saveProfileRemote(tunnelUrl, updated)
                                        }
                                    },
                                    onBack = { currentRoute = AppRoute.MAIN_TABS }
                                )
                            }

                            AppRoute.TICKER_DETAIL -> {
                                TickerDetailScreen(
                                    stock = activeStock,
                                    news = news.filter { it.ticker == activeStock.symbol }.ifEmpty { news },
                                    strings = strings,
                                    onBack = { currentRoute = AppRoute.MAIN_TABS },
                                    onOpenHalalAnalysis = { st ->
                                        activeStock = st
                                        currentRoute = AppRoute.HALAL_ANALYSIS
                                    }
                                )
                            }

                            AppRoute.HALAL_ANALYSIS -> {
                                HalalAnalysisScreen(
                                    stock = activeStock,
                                    strings = strings,
                                    onBack = { currentRoute = AppRoute.TICKER_DETAIL }
                                )
                            }

                            AppRoute.CALCULATOR -> {
                                CalculatorScreen(
                                    strings = strings,
                                    onBack = { currentRoute = AppRoute.MAIN_TABS },
                                    onCreateGoalFromCalc = { title, target, monthly ->
                                        val newGoal = FinancialGoal(
                                            id = (goals.maxOfOrNull { it.id } ?: 3) + 1,
                                            title = title,
                                            titleUz = title,
                                            targetAmount = target,
                                            currentAmount = 0.0,
                                            monthlyContribution = monthly,
                                            deadlineLabel = "Из калькулятора",
                                            deadlineLabelUz = "Kalkulyatordan",
                                            category = "savings",
                                            icon = "target"
                                        )
                                        goals = goals + newGoal
                                        showGoalsInMain = true
                                        selectedTab = 0
                                        currentRoute = AppRoute.MAIN_TABS
                                        scope.launch {
                                            val remote = WeGrowRepository.createGoalRemote(tunnelUrl, title, target, monthly)
                                            if (remote != null) {
                                                syncWithTunnel()
                                            }
                                        }
                                    }
                                )
                            }

                            AppRoute.MAIN_TABS -> {
                                val tabKey = if (selectedTab == 0 && showGoalsInMain) -1 else selectedTab
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AnimatedContent(
                                        targetState = tabKey,
                                        transitionSpec = {
                                            fadeIn(tween(160, easing = FastOutSlowInEasing)) togetherWith
                                                fadeOut(tween(120, easing = FastOutSlowInEasing))
                                        },
                                        label = "tabTransition"
                                    ) { activeKey ->
                                        when (activeKey) {
                                            -1 -> {
                                                GoalsScreen(
                                                    goals = goals,
                                                    strings = strings,
                                                    onContributeGoal = { gid, amt ->
                                                        goals = goals.map { g ->
                                                            if (g.id == gid) g.copy(currentAmount = (g.currentAmount + amt).coerceAtMost(g.targetAmount))
                                                            else g
                                                        }
                                                        scope.launch {
                                                            WeGrowRepository.contributeGoalRemote(tunnelUrl, gid, amt)
                                                        }
                                                    },
                                                    onCreateGoal = { title, target, monthly ->
                                                        val g = FinancialGoal(
                                                            id = (goals.maxOfOrNull { it.id } ?: 3) + 1,
                                                            title = title,
                                                            titleUz = title,
                                                            targetAmount = target,
                                                            currentAmount = 0.0,
                                                            monthlyContribution = monthly,
                                                            deadlineLabel = "2027",
                                                            deadlineLabelUz = "2027",
                                                            category = "savings",
                                                            icon = "target"
                                                        )
                                                        goals = goals + g
                                                        scope.launch {
                                                            WeGrowRepository.createGoalRemote(tunnelUrl, title, target, monthly)
                                                        }
                                                    },
                                                    onOpenCalculator = { currentRoute = AppRoute.CALCULATOR }
                                                )
                                            }

                                            0 -> {
                                                HomeScreen(
                                                    profile = profile,
                                                    stocks = stocks,
                                                    goals = goals,
                                                    news = news,
                                                    online = isOnline,
                                                    strings = strings,
                                                    onOpenStock = { st ->
                                                        activeStock = st
                                                        currentRoute = AppRoute.TICKER_DETAIL
                                                    },
                                                    onOpenGoals = { showGoalsInMain = true },
                                                    onOpenCalculator = { currentRoute = AppRoute.CALCULATOR },
                                                    onOpenMarket = { selectedTab = 1 },
                                                    onOpenCourses = { selectedTab = 2 },
                                                    onOpenHalalAnalysis = { st ->
                                                        activeStock = st
                                                        currentRoute = AppRoute.HALAL_ANALYSIS
                                                    },
                                                    onToggleTheme = {
                                                        isDarkTheme = !isDarkTheme
                                                        prefs.edit().putBoolean("dark_theme", isDarkTheme).apply()
                                                    },
                                                    onToggleLanguage = {
                                                        val nextLang = if (langCode == "ru") "uz" else "ru"
                                                        langCode = nextLang
                                                        prefs.edit().putString("lang_code", nextLang).apply()
                                                    }
                                                )
                                            }

                                            1 -> {
                                                MarketScreen(
                                                    stocks = stocks,
                                                    indices = indices,
                                                    heatmap = heatmap,
                                                    marketOpenText = if (langCode == "uz") marketTextUz else marketTextRu,
                                                    strings = strings,
                                                    onOpenStock = { st ->
                                                        activeStock = st
                                                        currentRoute = AppRoute.TICKER_DETAIL
                                                    }
                                                )
                                            }

                                            2 -> {
                                                val firstCourse = courses.first()
                                                CourseScreen(
                                                    course = firstCourse,
                                                    strings = strings,
                                                    onPurchase = { provider ->
                                                        courses = courses.map { c ->
                                                            c.copy(
                                                                purchased = true,
                                                                modules = c.modules.map { it.copy(locked = false) }
                                                            )
                                                        }
                                                        scope.launch {
                                                            WeGrowRepository.purchaseCourseRemote(tunnelUrl, firstCourse.id, provider)
                                                        }
                                                    }
                                                )
                                            }

                                            else -> {
                                                ProfileSettingsScreen(
                                                    profile = profile,
                                                    serverUrl = tunnelUrl,
                                                    online = isOnline,
                                                    isDarkTheme = isDarkTheme,
                                                    strings = strings,
                                                    onUpdateServerUrl = { newUrl ->
                                                        tunnelUrl = newUrl
                                                        prefs.edit().putString("tunnel_url", newUrl).apply()
                                                    },
                                                    onSyncNow = { syncWithTunnel() },
                                                    onToggleTheme = { dark ->
                                                        isDarkTheme = dark
                                                        prefs.edit().putBoolean("dark_theme", dark).apply()
                                                    },
                                                    onSwitchLanguage = { newLang ->
                                                        langCode = newLang
                                                        prefs.edit().putString("lang_code", newLang).apply()
                                                    },
                                                    onOpenQuestionnaire = { currentRoute = AppRoute.QUESTIONNAIRE },
                                                    onOpenCalculator = { currentRoute = AppRoute.CALCULATOR },
                                                    onReplayOnboarding = { currentRoute = AppRoute.LANGUAGE_SELECT }
                                                )
                                            }
                                        }
                                    }

                                    WeGrowFloatingNavBar(
                                        selectedTab = selectedTab,
                                        strings = strings,
                                        onSelectTab = { idx ->
                                            showGoalsInMain = false
                                            selectedTab = idx
                                        },
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
