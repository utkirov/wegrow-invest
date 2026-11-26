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
import uz.wegrow.invest.ui.components.WeGrowToastBanner
import uz.wegrow.invest.ui.screens.*
import uz.wegrow.invest.ui.screens.auth.AuthScreen
import uz.wegrow.invest.ui.screens.brokers.BrokersScreen
import uz.wegrow.invest.ui.screens.community.GoldClubScreen
import uz.wegrow.invest.ui.screens.community.StoriesViewerOverlay
import uz.wegrow.invest.ui.screens.community.StreamsScreen
import uz.wegrow.invest.ui.screens.courses.CertificateScreen
import uz.wegrow.invest.ui.screens.courses.CourseLmsScreen
import uz.wegrow.invest.ui.screens.news.NewsDetailScreen
import uz.wegrow.invest.ui.screens.news.NewsFeedScreen
import uz.wegrow.invest.ui.screens.notifications.NotificationsScreen
import uz.wegrow.invest.ui.screens.portfolio.PortfolioScreen
import uz.wegrow.invest.ui.screens.profile.MethodologyScreen
import uz.wegrow.invest.ui.screens.profile.OrdersHistoryScreen
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
    CALCULATOR,
    PORTFOLIO,
    NOTIFICATIONS,
    NEWS_FEED,
    NEWS_DETAIL,
    GOLD_CLUB,
    STREAMS,
    BROKERS,
    CERTIFICATE,
    ORDERS_HISTORY,
    METHODOLOGY
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val vm: WeGrowViewModel = viewModel()
            val uiState by vm.uiState.collectAsState()

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
            // 5-tab navigation: 0=Home, 1=Market, 2=Portfolio, 3=Courses, 4=Profile
            var selectedTab by remember { mutableIntStateOf(0) }
            var showGoalsInMain by remember { mutableStateOf(false) }
            var newsReturnRoute by remember { mutableStateOf(AppRoute.MAIN_TABS) }
            var calcInitialMode by remember { mutableIntStateOf(0) }

            val strings = if (uiState.langCode == "uz") StringsUz else StringsRu
            val activeStock = uiState.activeStock

            fun navigateByStoryAction(action: String) {
                when (action.uppercase()) {
                    "METHODOLOGY" -> currentRoute = AppRoute.METHODOLOGY
                    "PORTFOLIO" -> {
                        vm.syncPortfolioModule()
                        showGoalsInMain = false
                        selectedTab = 2
                        currentRoute = AppRoute.MAIN_TABS
                    }
                    "NEWS_FEED" -> currentRoute = AppRoute.NEWS_FEED
                    "BROKERS" -> currentRoute = AppRoute.BROKERS
                    "COURSES" -> {
                        showGoalsInMain = false
                        selectedTab = 3
                        currentRoute = AppRoute.MAIN_TABS
                    }
                    "GOLD_CLUB" -> currentRoute = AppRoute.GOLD_CLUB
                    "STREAMS" -> currentRoute = AppRoute.STREAMS
                    else -> {
                        showGoalsInMain = false
                        selectedTab = 1
                        currentRoute = AppRoute.MAIN_TABS
                    }
                }
            }

            BackHandler(
                enabled = uiState.activeStoryDeck != null ||
                    currentRoute != AppRoute.MAIN_TABS ||
                    showGoalsInMain ||
                    selectedTab != 0
            ) {
                when {
                    uiState.activeStoryDeck != null -> vm.closeStoryDeck()
                    currentRoute == AppRoute.HALAL_ANALYSIS -> currentRoute = AppRoute.TICKER_DETAIL
                    currentRoute == AppRoute.NEWS_DETAIL -> currentRoute = newsReturnRoute
                    currentRoute == AppRoute.ONBOARDING -> currentRoute = AppRoute.LANGUAGE_SELECT
                    currentRoute != AppRoute.MAIN_TABS -> currentRoute = AppRoute.MAIN_TABS
                    showGoalsInMain -> showGoalsInMain = false
                    selectedTab != 0 -> selectedTab = 0
                }
            }

            WeGrowInvestTheme(darkTheme = uiState.isDarkTheme) {
                val colors = WeGrowTheme.colors
                val subScreens = remember {
                    setOf(
                        AppRoute.TICKER_DETAIL,
                        AppRoute.HALAL_ANALYSIS,
                        AppRoute.CALCULATOR,
                        AppRoute.QUESTIONNAIRE,
                        AppRoute.AUTH,
                        AppRoute.NOTIFICATIONS,
                        AppRoute.NEWS_FEED,
                        AppRoute.NEWS_DETAIL,
                        AppRoute.GOLD_CLUB,
                        AppRoute.STREAMS,
                        AppRoute.BROKERS,
                        AppRoute.CERTIFICATE,
                        AppRoute.ORDERS_HISTORY,
                        AppRoute.METHODOLOGY,
                        AppRoute.PORTFOLIO
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.bg)
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    AnimatedContent(
                        targetState = currentRoute,
                        transitionSpec = {
                            val enteringSub = targetState in subScreens
                            val exitingSub = initialState in subScreens
                            when {
                                enteringSub -> {
                                    (fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                                        slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 6 }) togetherWith
                                        fadeOut(tween(150))
                                }
                                exitingSub -> {
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
                                    isWatchlisted = activeStock.symbol in uiState.watchlistSymbols,
                                    onToggleWatchlist = { vm.toggleWatchlist(activeStock.symbol) },
                                    onCreateAlert = { cond, target ->
                                        vm.createPriceAlert(activeStock.symbol, cond, target)
                                    },
                                    onAddToPortfolio = { sym, side, qty, price ->
                                        vm.addPortfolioTransaction(sym, side, qty, price)
                                    },
                                    onSelectTimeframe = { tf -> vm.loadStockChart(activeStock.symbol, tf) },
                                    onRefreshStock = { vm.refreshActiveStock() },
                                    onBack = { currentRoute = AppRoute.MAIN_TABS },
                                    onOpenHalalAnalysis = { st ->
                                        vm.selectStock(st)
                                        currentRoute = AppRoute.HALAL_ANALYSIS
                                    },
                                    onOpenNewsArticle = { art ->
                                        vm.openNewsArticle(art.id)
                                        newsReturnRoute = AppRoute.TICKER_DETAIL
                                        currentRoute = AppRoute.NEWS_DETAIL
                                    }
                                )
                            }

                            AppRoute.PORTFOLIO -> {
                                PortfolioScreen(
                                    portfolio = uiState.portfolio,
                                    watchlistSymbols = uiState.watchlistSymbols,
                                    allStocks = uiState.stocks,
                                    alerts = uiState.alerts,
                                    purification = uiState.purification,
                                    strings = strings,
                                    showBackButton = true,
                                    onBack = { currentRoute = AppRoute.MAIN_TABS },
                                    onOpenStock = { st ->
                                        vm.selectStock(st)
                                        currentRoute = AppRoute.TICKER_DETAIL
                                    },
                                    onAddTransaction = { sym, side, qty, price ->
                                        vm.addPortfolioTransaction(sym, side, qty, price)
                                    },
                                    onToggleWatchlist = { vm.toggleWatchlist(it) },
                                    onClearPurification = { vm.clearPurification(it) },
                                    onDeleteAlert = { vm.deletePriceAlert(it) }
                                )
                            }

                            AppRoute.HALAL_ANALYSIS -> {
                                HalalAnalysisScreen(
                                    stock = activeStock,
                                    strings = strings,
                                    onBack = { currentRoute = AppRoute.TICKER_DETAIL },
                                    onOpenMethodology = { currentRoute = AppRoute.METHODOLOGY }
                                )
                            }

                            AppRoute.CALCULATOR -> {
                                CalculatorScreen(
                                    strings = strings,
                                    portfolioValueUsd = uiState.portfolio.summary.totalValue,
                                    initialMode = calcInitialMode,
                                    onBack = { currentRoute = AppRoute.MAIN_TABS },
                                    onCreateGoalFromCalc = { title, target, monthly ->
                                        vm.createGoal(title, target, monthly, "Из калькулятора", "Kalkulyatordan")
                                        showGoalsInMain = true
                                        selectedTab = 0
                                        currentRoute = AppRoute.MAIN_TABS
                                    }
                                )
                            }

                            AppRoute.NOTIFICATIONS -> {
                                NotificationsScreen(
                                    notifications = uiState.notifications,
                                    readIds = uiState.readNotificationIds,
                                    allStocks = uiState.stocks,
                                    strings = strings,
                                    onBack = { currentRoute = AppRoute.MAIN_TABS },
                                    onMarkAllRead = { vm.markAllNotificationsRead() },
                                    onOpenStock = { st ->
                                        vm.selectStock(st)
                                        currentRoute = AppRoute.TICKER_DETAIL
                                    },
                                    onOpenPortfolio = {
                                        vm.syncPortfolioModule()
                                        selectedTab = 2
                                        showGoalsInMain = false
                                        currentRoute = AppRoute.MAIN_TABS
                                    },
                                    onDeleteAlert = { vm.deletePriceAlert(it) }
                                )
                            }

                            AppRoute.NEWS_FEED -> {
                                NewsFeedScreen(
                                    news = uiState.news,
                                    strings = strings,
                                    onBack = { currentRoute = AppRoute.MAIN_TABS },
                                    onOpenArticle = { art ->
                                        vm.openNewsArticle(art.id)
                                        newsReturnRoute = AppRoute.NEWS_FEED
                                        currentRoute = AppRoute.NEWS_DETAIL
                                    }
                                )
                            }

                            AppRoute.NEWS_DETAIL -> {
                                val article = uiState.activeNewsItem
                                NewsDetailScreen(
                                    article = article,
                                    relatedStock = uiState.stocks.find { it.symbol == article.ticker },
                                    otherNews = uiState.news.filterNot { it.id == article.id },
                                    strings = strings,
                                    onBack = { currentRoute = newsReturnRoute },
                                    onOpenStock = { st ->
                                        vm.selectStock(st)
                                        currentRoute = AppRoute.TICKER_DETAIL
                                    },
                                    onOpenHalalAnalysis = { st ->
                                        vm.selectStock(st)
                                        currentRoute = AppRoute.HALAL_ANALYSIS
                                    },
                                    onOpenArticle = { next -> vm.openNewsArticle(next.id) }
                                )
                            }

                            AppRoute.GOLD_CLUB -> {
                                GoldClubScreen(
                                    isJoined = uiState.goldClubJoined,
                                    strings = strings,
                                    onBack = { currentRoute = AppRoute.MAIN_TABS },
                                    onJoinPlan = { plan -> vm.joinGoldClubSandbox(plan) },
                                    onOpenStreams = { currentRoute = AppRoute.STREAMS }
                                )
                            }

                            AppRoute.STREAMS -> {
                                StreamsScreen(
                                    streams = uiState.streams,
                                    remindedIds = uiState.remindedStreamIds,
                                    goldClubJoined = uiState.goldClubJoined,
                                    strings = strings,
                                    onBack = { currentRoute = AppRoute.MAIN_TABS },
                                    onToggleReminder = { vm.toggleStreamReminder(it) },
                                    onOpenGoldClub = { currentRoute = AppRoute.GOLD_CLUB },
                                    onWatchStream = { session ->
                                        val isUz = uiState.langCode == "uz"
                                        vm.showToast(
                                            if (isUz) "Efir ochildi: ${session.titleUz}"
                                            else "Запуск эфира: ${session.title}"
                                        )
                                    }
                                )
                            }

                            AppRoute.BROKERS -> {
                                BrokersScreen(
                                    brokers = uiState.brokers,
                                    strings = strings,
                                    onBack = { currentRoute = AppRoute.MAIN_TABS }
                                )
                            }

                            AppRoute.CERTIFICATE -> {
                                val firstCourse = uiState.courses.first()
                                CertificateScreen(
                                    profile = uiState.profile,
                                    course = firstCourse,
                                    completedLessons = uiState.courseLessons.count { it.completed },
                                    totalLessons = uiState.courseLessons.size.coerceAtLeast(6),
                                    strings = strings,
                                    onBack = {
                                        selectedTab = 3
                                        currentRoute = AppRoute.MAIN_TABS
                                    },
                                    onShareNotice = {
                                        val isUz = uiState.langCode == "uz"
                                        vm.showToast(
                                            if (isUz) "Sertifikat kodi nusxalandi ✓" else "Код верификации сертификата скопирован ✓"
                                        )
                                    }
                                )
                            }

                            AppRoute.ORDERS_HISTORY -> {
                                OrdersHistoryScreen(
                                    orders = uiState.orders,
                                    strings = strings,
                                    onBack = { currentRoute = AppRoute.MAIN_TABS },
                                    onSandboxPay = { uid, cid -> vm.completeSandboxOrder(uid, cid) },
                                    onOpenCourses = {
                                        selectedTab = 3
                                        showGoalsInMain = false
                                        currentRoute = AppRoute.MAIN_TABS
                                    }
                                )
                            }

                            AppRoute.METHODOLOGY -> {
                                MethodologyScreen(
                                    strings = strings,
                                    onBack = { currentRoute = AppRoute.MAIN_TABS },
                                    onOpenCalculator = {
                                        calcInitialMode = 3 // Open directly on Zakat 2.5% tab!
                                        currentRoute = AppRoute.CALCULATOR
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
                                                    onDeleteGoal = { vm.deleteGoal(it) },
                                                    onOpenCalculator = {
                                                        calcInitialMode = 0
                                                        currentRoute = AppRoute.CALCULATOR
                                                    },
                                                    onBack = { showGoalsInMain = false }
                                                )
                                            }

                                            0 -> {
                                                HomeScreen(
                                                    profile = uiState.profile,
                                                    stocks = uiState.stocks,
                                                    goals = uiState.goals,
                                                    news = uiState.news,
                                                    stories = uiState.stories,
                                                    seenStoryIds = uiState.seenStoryIds,
                                                    unreadNotificationsCount = uiState.unreadNotificationsCount,
                                                    online = uiState.isOnline,
                                                    strings = strings,
                                                    onOpenStock = { st ->
                                                        vm.selectStock(st)
                                                        currentRoute = AppRoute.TICKER_DETAIL
                                                    },
                                                    onOpenGoals = { showGoalsInMain = true },
                                                    onOpenPortfolio = {
                                                        vm.syncPortfolioModule()
                                                        selectedTab = 2
                                                    },
                                                    onOpenCalculator = {
                                                        calcInitialMode = 0
                                                        currentRoute = AppRoute.CALCULATOR
                                                    },
                                                    onOpenBrokers = { currentRoute = AppRoute.BROKERS },
                                                    onOpenMarket = { selectedTab = 1 },
                                                    onOpenNotifications = { currentRoute = AppRoute.NOTIFICATIONS },
                                                    onOpenStory = { deckId -> vm.openStoryDeck(deckId) },
                                                    onOpenAllNews = { currentRoute = AppRoute.NEWS_FEED },
                                                    onOpenNewsArticle = { art ->
                                                        vm.openNewsArticle(art.id)
                                                        newsReturnRoute = AppRoute.MAIN_TABS
                                                        currentRoute = AppRoute.NEWS_DETAIL
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
                                                PortfolioScreen(
                                                    portfolio = uiState.portfolio,
                                                    watchlistSymbols = uiState.watchlistSymbols,
                                                    allStocks = uiState.stocks,
                                                    alerts = uiState.alerts,
                                                    purification = uiState.purification,
                                                    strings = strings,
                                                    showBackButton = false,
                                                    onOpenStock = { st ->
                                                        vm.selectStock(st)
                                                        currentRoute = AppRoute.TICKER_DETAIL
                                                    },
                                                    onAddTransaction = { sym, side, qty, price ->
                                                        vm.addPortfolioTransaction(sym, side, qty, price)
                                                    },
                                                    onToggleWatchlist = { vm.toggleWatchlist(it) },
                                                    onClearPurification = { vm.clearPurification(it) },
                                                    onDeleteAlert = { vm.deletePriceAlert(it) }
                                                )
                                            }

                                            3 -> {
                                                val firstCourse = uiState.courses.first()
                                                if (uiState.courseLessons.isNotEmpty() || uiState.activeLesson != null) {
                                                    CourseLmsScreen(
                                                        course = firstCourse,
                                                        lessons = uiState.courseLessons,
                                                        activeLesson = uiState.activeLesson,
                                                        pendingOrder = uiState.pendingOrder,
                                                        strings = strings,
                                                        onOpenLesson = { les -> vm.openLesson(firstCourse.id, les.id) },
                                                        onCloseLesson = { vm.closeLesson() },
                                                        onCompleteLesson = { lid, ans -> vm.completeLesson(firstCourse.id, lid, ans) },
                                                        onStartOrder = { prov -> vm.startCourseOrder(firstCourse.id, prov) },
                                                        onSandboxPay = { uid -> vm.completeSandboxOrder(uid, firstCourse.id) },
                                                        onOpenCertificate = { currentRoute = AppRoute.CERTIFICATE }
                                                    )
                                                } else {
                                                    CourseScreen(
                                                        course = firstCourse,
                                                        strings = strings,
                                                        onPurchase = { provider ->
                                                            vm.purchaseCourse(firstCourse.id, provider)
                                                        }
                                                    )
                                                }
                                            }

                                            else -> {
                                                ProfileSettingsScreen(
                                                    profile = uiState.profile,
                                                    serverUrl = uiState.serverUrl,
                                                    online = uiState.isOnline,
                                                    isDarkTheme = uiState.isDarkTheme,
                                                    isAuthenticated = uiState.session.isAuthenticated,
                                                    ordersCount = uiState.orders.size,
                                                    goldClubJoined = uiState.goldClubJoined,
                                                    strings = strings,
                                                    onUpdateServerUrl = { vm.updateServerUrl(it) },
                                                    onSyncNow = { vm.syncWithServer() },
                                                    onToggleTheme = { vm.setDarkTheme(it) },
                                                    onSwitchLanguage = { vm.setLanguage(it) },
                                                    onOpenQuestionnaire = { currentRoute = AppRoute.QUESTIONNAIRE },
                                                    onOpenCalculator = {
                                                        calcInitialMode = 0
                                                        currentRoute = AppRoute.CALCULATOR
                                                    },
                                                    onReplayOnboarding = { currentRoute = AppRoute.LANGUAGE_SELECT },
                                                    onOpenAuth = {
                                                        vm.clearAuthStep()
                                                        currentRoute = AppRoute.AUTH
                                                    },
                                                    onLogout = { vm.logout() },
                                                    onOpenOrders = {
                                                        vm.syncContentAndOrders()
                                                        currentRoute = AppRoute.ORDERS_HISTORY
                                                    },
                                                    onOpenBrokers = { currentRoute = AppRoute.BROKERS },
                                                    onOpenMethodology = { currentRoute = AppRoute.METHODOLOGY },
                                                    onOpenGoldClub = { currentRoute = AppRoute.GOLD_CLUB },
                                                    onOpenStreams = { currentRoute = AppRoute.STREAMS }
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
                                            if (idx == 2) vm.syncPortfolioModule()
                                        },
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                    )
                                }
                            }
                        }
                    }

                    // Full-screen Stories Viewer overlay
                    uiState.activeStoryDeck?.let { deck ->
                        StoriesViewerOverlay(
                            deck = deck,
                            strings = strings,
                            onClose = { vm.closeStoryDeck() },
                            onActionRoute = ::navigateByStoryAction
                        )
                    }

                    // Global confirmation toast banner
                    WeGrowToastBanner(
                        message = uiState.toastMessage,
                        onDismiss = { vm.clearToast() },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}
