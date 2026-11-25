package uz.wegrow.invest

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
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.wegrow.invest.ui.StringsRu
import uz.wegrow.invest.ui.StringsUz
import uz.wegrow.invest.ui.WeGrowViewModel
import uz.wegrow.invest.ui.components.WeGrowFloatingNavBar
import uz.wegrow.invest.ui.screens.*
import uz.wegrow.invest.ui.screens.auth.AuthScreen
import uz.wegrow.invest.ui.theme.WeGrowInvestTheme
import uz.wegrow.invest.ui.theme.WeGrowTheme

enum class AppRoute {
    LANGUAGE_SELECT,
    ONBOARDING,
    AUTH,
    QUESTIONNAIRE,
    MAIN_TABS,
    TICKER_DETAIL,
    HALAL_ANALYSIS,
    CALCULATOR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val vm: WeGrowViewModel = viewModel()
            val uiState by vm.uiState.collectAsState()

            // Dynamically adapt Edge-to-Edge system bar icon contrast to Dark/Light theme
            DisposableEffect(uiState.isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (uiState.isDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (uiState.isDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            var currentRoute by remember { mutableStateOf(AppRoute.MAIN_TABS) }
            var selectedTab by remember { mutableIntStateOf(0) } // 0=Home, 1=Market, 2=Courses, 3=Profile
            var showGoalsInMain by remember { mutableStateOf(false) }

            val strings = if (uiState.langCode == "uz") StringsUz else StringsRu
            val activeStock = uiState.activeStock

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

            WeGrowInvestTheme(darkTheme = uiState.isDarkTheme) {
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
                                AppRoute.QUESTIONNAIRE,
                                AppRoute.AUTH
                            )
                            val exitingSubScreen = initialState in listOf(
                                AppRoute.TICKER_DETAIL,
                                AppRoute.HALAL_ANALYSIS,
                                AppRoute.CALCULATOR,
                                AppRoute.QUESTIONNAIRE,
                                AppRoute.AUTH
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
                                    currentLang = uiState.langCode,
                                    strings = strings,
                                    onSelectLanguage = { vm.setLanguage(it) },
                                    onContinue = { currentRoute = AppRoute.ONBOARDING }
                                )
                            }

                            AppRoute.ONBOARDING -> {
                                val nvda = uiState.stocks.find { it.symbol == "NVDA" } ?: activeStock
                                OnboardingScreen(
                                    stock = nvda,
                                    strings = strings,
                                    onSkip = { currentRoute = AppRoute.MAIN_TABS },
                                    onFinish = { currentRoute = AppRoute.AUTH }
                                )
                            }

                            AppRoute.AUTH -> {
                                AuthScreen(
                                    pendingOtp = uiState.pendingOtp,
                                    isLoading = uiState.isAuthLoading,
                                    errorMessage = uiState.authError,
                                    strings = strings,
                                    onRequestOtp = { vm.requestOtp(it) },
                                    onVerifyOtp = { id, code ->
                                        vm.verifyOtp(id, code) {
                                            currentRoute = AppRoute.MAIN_TABS
                                        }
                                    },
                                    onResetOtp = { vm.clearAuthStep() },
                                    onContinueAsGuest = {
                                        vm.loginAsGuest {
                                            currentRoute = AppRoute.MAIN_TABS
                                        }
                                    },
                                    onBack = { currentRoute = AppRoute.MAIN_TABS }
                                )
                            }

                            AppRoute.QUESTIONNAIRE -> {
                                ProfileQuestionnaireScreen(
                                    profile = uiState.profile,
                                    strings = strings,
                                    onSave = { updated ->
                                        vm.saveProfile(updated)
                                        currentRoute = AppRoute.MAIN_TABS
                                    },
                                    onBack = { currentRoute = AppRoute.MAIN_TABS }
                                )
                            }

                            AppRoute.TICKER_DETAIL -> {
                                TickerDetailScreen(
                                    stock = activeStock,
                                    news = uiState.news.filter { it.ticker == activeStock.symbol }.ifEmpty { uiState.news },
                                    strings = strings,
                                    activeTimeframe = uiState.activeTimeframe,
                                    activeChartPoints = uiState.activeChart?.takeIf { it.symbol == activeStock.symbol }?.points,
                                    onSelectTimeframe = { tf -> vm.loadStockChart(activeStock.symbol, tf) },
                                    onRefreshStock = { vm.refreshActiveStock() },
                                    onBack = { currentRoute = AppRoute.MAIN_TABS },
                                    onOpenHalalAnalysis = { st ->
                                        vm.selectStock(st)
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
                                        vm.createGoal(title, target, monthly, "Из калькулятора", "Kalkulyatordan")
                                        showGoalsInMain = true
                                        selectedTab = 0
                                        currentRoute = AppRoute.MAIN_TABS
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
                                                    goals = uiState.goals,
                                                    strings = strings,
                                                    onContributeGoal = { gid, amt -> vm.contributeGoal(gid, amt) },
                                                    onCreateGoal = { title, target, monthly ->
                                                        vm.createGoal(title, target, monthly)
                                                    },
                                                    onOpenCalculator = { currentRoute = AppRoute.CALCULATOR }
                                                )
                                            }

                                            0 -> {
                                                HomeScreen(
                                                    profile = uiState.profile,
                                                    stocks = uiState.stocks,
                                                    goals = uiState.goals,
                                                    news = uiState.news,
                                                    online = uiState.isOnline,
                                                    strings = strings,
                                                    onOpenStock = { st ->
                                                        vm.selectStock(st)
                                                        currentRoute = AppRoute.TICKER_DETAIL
                                                    },
                                                    onOpenGoals = { showGoalsInMain = true },
                                                    onOpenCalculator = { currentRoute = AppRoute.CALCULATOR },
                                                    onOpenMarket = { selectedTab = 1 },
                                                    onOpenCourses = { selectedTab = 2 },
                                                    onOpenHalalAnalysis = { st ->
                                                        vm.selectStock(st)
                                                        currentRoute = AppRoute.HALAL_ANALYSIS
                                                    },
                                                    onToggleTheme = { vm.setDarkTheme(!uiState.isDarkTheme) },
                                                    onToggleLanguage = {
                                                        vm.setLanguage(if (uiState.langCode == "ru") "uz" else "ru")
                                                    }
                                                )
                                            }

                                            1 -> {
                                                MarketScreen(
                                                    stocks = uiState.stocks,
                                                    indices = uiState.indices,
                                                    heatmap = uiState.heatmap,
                                                    marketOpenText = if (uiState.langCode == "uz") uiState.marketOpenTextUz else uiState.marketOpenTextRu,
                                                    strings = strings,
                                                    onOpenStock = { st ->
                                                        vm.selectStock(st)
                                                        currentRoute = AppRoute.TICKER_DETAIL
                                                    }
                                                )
                                            }

                                            2 -> {
                                                val firstCourse = uiState.courses.first()
                                                CourseScreen(
                                                    course = firstCourse,
                                                    strings = strings,
                                                    onPurchase = { provider ->
                                                        vm.purchaseCourse(firstCourse.id, provider)
                                                    }
                                                )
                                            }

                                            else -> {
                                                ProfileSettingsScreen(
                                                    profile = uiState.profile,
                                                    serverUrl = uiState.serverUrl,
                                                    online = uiState.isOnline,
                                                    isDarkTheme = uiState.isDarkTheme,
                                                    isAuthenticated = uiState.session.isAuthenticated,
                                                    strings = strings,
                                                    onUpdateServerUrl = { vm.updateServerUrl(it) },
                                                    onSyncNow = { vm.syncWithServer() },
                                                    onToggleTheme = { vm.setDarkTheme(it) },
                                                    onSwitchLanguage = { vm.setLanguage(it) },
                                                    onOpenQuestionnaire = { currentRoute = AppRoute.QUESTIONNAIRE },
                                                    onOpenCalculator = { currentRoute = AppRoute.CALCULATOR },
                                                    onReplayOnboarding = { currentRoute = AppRoute.LANGUAGE_SELECT },
                                                    onOpenAuth = {
                                                        vm.clearAuthStep()
                                                        currentRoute = AppRoute.AUTH
                                                    },
                                                    onLogout = { vm.logout() }
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
