package uz.wegrow.invest.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.wegrow.invest.data.AppNotificationItem
import uz.wegrow.invest.data.AuthSession
import uz.wegrow.invest.data.AuthSessionStore
import uz.wegrow.invest.data.BrokerInfo
import uz.wegrow.invest.data.CourseItem
import uz.wegrow.invest.data.FinancialGoal
import uz.wegrow.invest.data.MarketIndex
import uz.wegrow.invest.data.NewsItem
import uz.wegrow.invest.data.OtpRequestResult
import uz.wegrow.invest.data.PaymentOrderInfo
import uz.wegrow.invest.data.SectorHeatTile
import uz.wegrow.invest.data.StockChartData
import uz.wegrow.invest.data.StockItem
import uz.wegrow.invest.data.StoryDeck
import uz.wegrow.invest.data.StreamSession
import uz.wegrow.invest.data.UserProfile
import uz.wegrow.invest.data.WeGrowApiClient
import uz.wegrow.invest.data.WeGrowRepository

@Immutable
data class WeGrowUiState(
    val isDarkTheme: Boolean = true,
    val langCode: String = "ru",
    val serverUrl: String = "",
    val session: AuthSession = AuthSession(),
    val profile: UserProfile = UserProfile(),
    val stocks: List<StockItem> = emptyList(),
    val goals: List<FinancialGoal> = emptyList(),
    val courses: List<CourseItem> = emptyList(),
    val news: List<NewsItem> = emptyList(),
    val indices: List<MarketIndex> = emptyList(),
    val heatmap: List<SectorHeatTile> = emptyList(),
    val marketOpenTextRu: String = "",
    val marketOpenTextUz: String = "",
    val isOnline: Boolean = false,
    val isSyncing: Boolean = false,
    val activeStockSymbol: String = "AAPL",
    val activeChart: StockChartData? = null,
    val activeTimeframe: String = "1M",
    val isAuthLoading: Boolean = false,
    val authError: String? = null,
    val pendingOtp: OtpRequestResult? = null,
    val portfolio: uz.wegrow.invest.data.PortfolioData = uz.wegrow.invest.data.PortfolioData(),
    val watchlistSymbols: Set<String> = emptySet(),
    val alerts: List<uz.wegrow.invest.data.PriceAlertItem> = emptyList(),
    val purification: uz.wegrow.invest.data.PurificationLedger = uz.wegrow.invest.data.PurificationLedger(),
    val courseLessons: List<uz.wegrow.invest.data.CourseLessonSummary> = emptyList(),
    val activeLesson: uz.wegrow.invest.data.LessonDetail? = null,
    val pendingOrder: PaymentOrderInfo? = null,
    // v4.1 additions
    val orders: List<PaymentOrderInfo> = emptyList(),
    val stories: List<StoryDeck> = emptyList(),
    val brokers: List<BrokerInfo> = emptyList(),
    val streams: List<StreamSession> = emptyList(),
    val seenStoryIds: Set<String> = emptySet(),
    val readNotificationIds: Set<String> = emptySet(),
    val remindedStreamIds: Set<Int> = emptySet(),
    val goldClubJoined: Boolean = false,
    val activeNewsId: Int? = null,
    val activeStoryDeckId: String? = null,
    val toastMessage: String? = null
) {
    val activeStock: StockItem
        get() = stocks.find { it.symbol == activeStockSymbol } ?: stocks.firstOrNull()
            ?: WeGrowRepository.defaultFallbackData().stocks.first()

    val activeNewsItem: NewsItem
        get() = news.find { it.id == activeNewsId } ?: news.firstOrNull()
            ?: WeGrowRepository.defaultFallbackData().news.first()

    val activeStoryDeck: StoryDeck?
        get() = stories.find { it.id == activeStoryDeckId }

    val notifications: List<AppNotificationItem>
        get() {
            val list = mutableListOf<AppNotificationItem>()
            alerts.forEach { al ->
                list += AppNotificationItem(
                    id = "alert_${al.id}",
                    category = "ALERT",
                    title = if (al.triggered) "Сработал алерт ${al.symbol}" else "Активный алерт ${al.symbol}",
                    titleUz = if (al.triggered) "${al.symbol} alerti ishga tushdi" else "${al.symbol} faol alerti",
                    body = al.message.ifBlank { "${al.condition} $${String.format("%.2f", al.targetPrice)}" },
                    bodyUz = al.message.ifBlank { "${al.condition} $${String.format("%.2f", al.targetPrice)}" },
                    badge = if (al.triggered) "Сработал" else "Активен",
                    symbol = al.symbol,
                    alertId = al.id,
                    isUrgent = al.triggered
                )
            }
            portfolio.warnings.forEach { w ->
                list += AppNotificationItem(
                    id = "warn_${w.symbol}",
                    category = "HALAL",
                    title = "Проверка халяльности: ${w.symbol}",
                    titleUz = "Halollik ogohlantirishi: ${w.symbol}",
                    body = "${w.name} имеет статус ${w.halalStatus.name}. ${w.reason}",
                    bodyUz = "${w.name} maqomi: ${w.halalStatus.name}. ${w.reason}",
                    badge = w.halalStatus.name,
                    symbol = w.symbol,
                    isUrgent = true
                )
            }
            if (purification.totalPendingUsd > 0.0005) {
                list += AppNotificationItem(
                    id = "purif_${purification.quarterLabel}",
                    category = "PURIFICATION",
                    title = "Очищение дивидендов (${purification.quarterLabel})",
                    titleUz = "Dividend poklash (${purification.quarterLabel})",
                    body = "К направлению на благотворительность: $${String.format("%.3f", purification.totalPendingUsd)}",
                    bodyUz = "Xayriyaga ajratish kerak: $${String.format("%.3f", purification.totalPendingUsd)}",
                    badge = "Тазкия",
                    isUrgent = true
                )
            }
            list += AppNotificationItem(
                id = "digest_aaoifi_q3",
                category = "DIGEST",
                title = "Аудит AAOIFI за III квартал 2026 завершён",
                titleUz = "2026-yil III chorak AAOIFI auditi yakunlandi",
                body = "Проверено 105 компаний США: 82 Халяль, 3 Сомнительно, 20 Не халяль.",
                bodyUz = "AQShning 105 kompaniyasi tekshirildi: 82 Halol, 3 Shubhali, 20 Halol emas.",
                badge = "AAOIFI",
                symbol = "AAPL"
            )
            return list
        }

    val unreadNotificationsCount: Int
        get() = notifications.count { it.id !in readNotificationIds }
}

class WeGrowViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AuthSessionStore(application)
    private val fallback = WeGrowRepository.defaultFallbackData()

    private val _uiState = MutableStateFlow(
        WeGrowUiState(
            isDarkTheme = store.isDarkTheme(),
            langCode = store.getLangCode(),
            serverUrl = store.getServerUrl(),
            session = store.loadSession(),
            profile = fallback.profile.copy(
                language = store.getLangCode(),
                darkTheme = store.isDarkTheme()
            ),
            stocks = fallback.stocks,
            goals = fallback.goals,
            courses = fallback.courses,
            news = fallback.news,
            indices = fallback.indices,
            heatmap = fallback.heatmap,
            marketOpenTextRu = fallback.marketOpenTextRu,
            marketOpenTextUz = fallback.marketOpenTextUz,
            activeStockSymbol = fallback.stocks.first().symbol,
            stories = WeGrowRepository.defaultStories(),
            brokers = WeGrowRepository.defaultBrokers(),
            streams = WeGrowRepository.defaultStreams()
        )
    )
    val uiState: StateFlow<WeGrowUiState> = _uiState.asStateFlow()

    init {
        syncWithServer()
    }

    private fun handleSessionRefreshed(newSession: AuthSession) {
        store.saveSession(newSession)
        _uiState.update { it.copy(session = newSession) }
    }

    fun syncWithServer() {
        val current = _uiState.value
        _uiState.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            val data = WeGrowApiClient.fetchBootstrap(
                baseUrl = current.serverUrl,
                session = current.session,
                onSessionRefreshed = ::handleSessionRefreshed
            )
            _uiState.update { state ->
                val nextSymbol = data.stocks.find { it.symbol == state.activeStockSymbol }?.symbol
                    ?: data.stocks.firstOrNull()?.symbol ?: "AAPL"
                state.copy(
                    stocks = data.stocks,
                    goals = data.goals,
                    courses = data.courses,
                    news = data.news,
                    indices = data.indices,
                    heatmap = data.heatmap,
                    marketOpenTextRu = data.marketOpenTextRu,
                    marketOpenTextUz = data.marketOpenTextUz,
                    isOnline = data.online,
                    isSyncing = false,
                    activeStockSymbol = nextSymbol,
                    profile = if (data.online) {
                        data.profile.copy(language = state.langCode, darkTheme = state.isDarkTheme)
                    } else {
                        state.profile
                    }
                )
            }
            if (data.online) {
                syncPortfolioModule()
                syncCourseLms()
                syncContentAndOrders()
            }
        }
    }

    fun selectStock(stock: StockItem) {
        _uiState.update {
            it.copy(
                activeStockSymbol = stock.symbol,
                activeTimeframe = "1M",
                activeChart = null
            )
        }
        loadStockChart(stock.symbol, "1M", refresh = false)
    }

    fun loadStockChart(symbol: String, timeframe: String, refresh: Boolean = false) {
        val tf = timeframe.uppercase()
        _uiState.update { it.copy(activeTimeframe = tf) }
        val state = _uiState.value
        viewModelScope.launch {
            val chart = WeGrowApiClient.fetchStockChart(
                baseUrl = state.serverUrl,
                session = state.session,
                symbol = symbol,
                timeframe = tf,
                refresh = refresh,
                onSessionRefreshed = ::handleSessionRefreshed
            )
            if (chart != null) {
                _uiState.update { s ->
                    if (s.activeStockSymbol == symbol && s.activeTimeframe == tf) {
                        s.copy(activeChart = chart)
                    } else {
                        s
                    }
                }
            }
        }
    }

    fun refreshActiveStock() {
        val state = _uiState.value
        val symbol = state.activeStockSymbol
        viewModelScope.launch {
            val updated = WeGrowApiClient.refreshStock(
                baseUrl = state.serverUrl,
                session = state.session,
                symbol = symbol,
                onSessionRefreshed = ::handleSessionRefreshed
            )
            if (updated != null) {
                _uiState.update { s ->
                    s.copy(
                        stocks = s.stocks.map { if (it.symbol == updated.symbol) updated else it }
                    )
                }
                loadStockChart(symbol, _uiState.value.activeTimeframe, refresh = true)
            }
        }
    }

    fun requestOtp(identifier: String) {
        _uiState.update { it.copy(isAuthLoading = true, authError = null) }
        val baseUrl = _uiState.value.serverUrl
        viewModelScope.launch {
            val res = WeGrowApiClient.requestOtp(baseUrl, identifier)
            res.fold(
                onSuccess = { otp ->
                    _uiState.update { it.copy(isAuthLoading = false, pendingOtp = otp, authError = null) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isAuthLoading = false, authError = err.message ?: "Ошибка отправки кода") }
                }
            )
        }
    }

    fun verifyOtp(identifier: String, code: String, onSuccess: () -> Unit) {
        _uiState.update { it.copy(isAuthLoading = true, authError = null) }
        val state = _uiState.value
        viewModelScope.launch {
            val res = WeGrowApiClient.verifyOtp(state.serverUrl, identifier, code, state.langCode)
            res.fold(
                onSuccess = { payload ->
                    store.saveSession(payload.session)
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            session = payload.session,
                            profile = payload.profile.copy(language = it.langCode, darkTheme = it.isDarkTheme),
                            pendingOtp = null,
                            authError = null
                        )
                    }
                    syncWithServer()
                    onSuccess()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isAuthLoading = false, authError = err.message ?: "Неверный код") }
                }
            )
        }
    }

    fun loginAsGuest(onSuccess: () -> Unit) {
        _uiState.update { it.copy(isAuthLoading = true, authError = null) }
        val baseUrl = _uiState.value.serverUrl
        viewModelScope.launch {
            val res = WeGrowApiClient.loginAsGuest(baseUrl)
            res.fold(
                onSuccess = { payload ->
                    store.saveSession(payload.session)
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            session = payload.session,
                            profile = payload.profile.copy(language = it.langCode, darkTheme = it.isDarkTheme),
                            pendingOtp = null,
                            authError = null
                        )
                    }
                    syncWithServer()
                    onSuccess()
                },
                onFailure = {
                    // Offline guest fallback so users can still enter app immediately
                    _uiState.update { it.copy(isAuthLoading = false, authError = null) }
                    onSuccess()
                }
            )
        }
    }

    fun clearAuthStep() {
        _uiState.update { it.copy(pendingOtp = null, authError = null) }
    }

    fun logout() {
        val state = _uiState.value
        val refresh = state.session.refreshToken
        store.clearSession()
        _uiState.update {
            it.copy(
                session = AuthSession(),
                pendingOtp = null,
                authError = null
            )
        }
        viewModelScope.launch {
            WeGrowApiClient.logout(state.serverUrl, refresh)
            syncWithServer()
        }
    }

    fun updateServerUrl(newUrl: String) {
        store.saveServerUrl(newUrl)
        _uiState.update { it.copy(serverUrl = newUrl.trim()) }
    }

    fun setDarkTheme(dark: Boolean) {
        store.saveDarkTheme(dark)
        _uiState.update { it.copy(isDarkTheme = dark, profile = it.profile.copy(darkTheme = dark)) }
    }

    fun setLanguage(lang: String) {
        store.saveLangCode(lang)
        _uiState.update { it.copy(langCode = lang, profile = it.profile.copy(language = lang)) }
    }

    fun saveProfile(updated: UserProfile) {
        _uiState.update { it.copy(profile = updated) }
        val state = _uiState.value
        viewModelScope.launch {
            WeGrowApiClient.saveProfile(
                baseUrl = state.serverUrl,
                session = state.session,
                profile = updated,
                onSessionRefreshed = ::handleSessionRefreshed
            )
        }
    }

    fun createGoal(title: String, targetAmount: Double, monthlyContribution: Double, deadlineRu: String = "2027", deadlineUz: String = "2027") {
        val tempId = (_uiState.value.goals.maxOfOrNull { it.id } ?: 3) + 1
        val localGoal = FinancialGoal(
            id = tempId,
            title = title,
            titleUz = title,
            targetAmount = targetAmount,
            currentAmount = 0.0,
            monthlyContribution = monthlyContribution,
            deadlineLabel = deadlineRu,
            deadlineLabelUz = deadlineUz,
            category = "savings",
            icon = "target"
        )
        _uiState.update { it.copy(goals = it.goals + localGoal) }
        val state = _uiState.value
        viewModelScope.launch {
            val remote = WeGrowApiClient.createGoal(
                baseUrl = state.serverUrl,
                session = state.session,
                title = title,
                targetAmount = targetAmount,
                monthlyContribution = monthlyContribution,
                onSessionRefreshed = ::handleSessionRefreshed
            )
            if (remote != null) {
                syncWithServer()
            }
        }
    }

    fun contributeGoal(goalId: Int, amount: Double) {
        _uiState.update { s ->
            s.copy(
                goals = s.goals.map { g ->
                    if (g.id == goalId) g.copy(currentAmount = (g.currentAmount + amount).coerceAtMost(g.targetAmount))
                    else g
                }
            )
        }
        val state = _uiState.value
        viewModelScope.launch {
            WeGrowApiClient.contributeGoal(
                baseUrl = state.serverUrl,
                session = state.session,
                goalId = goalId,
                amount = amount,
                onSessionRefreshed = ::handleSessionRefreshed
            )
        }
    }

    fun purchaseCourse(courseId: Int, provider: String) {
        _uiState.update { s ->
            s.copy(
                courses = s.courses.map { c ->
                    if (c.id == courseId) {
                        c.copy(purchased = true, modules = c.modules.map { it.copy(locked = false) })
                    } else c
                }
            )
        }
        val state = _uiState.value
        viewModelScope.launch {
            WeGrowApiClient.purchaseCourse(
                baseUrl = state.serverUrl,
                session = state.session,
                courseId = courseId,
                provider = provider,
                onSessionRefreshed = ::handleSessionRefreshed
            )
        }
    }

    fun syncPortfolioModule() {
        val state = _uiState.value
        viewModelScope.launch {
            val portfolio = WeGrowApiClient.fetchPortfolio(state.serverUrl, state.session, ::handleSessionRefreshed)
            val wl = WeGrowApiClient.fetchWatchlistSymbols(state.serverUrl, state.session, ::handleSessionRefreshed)
            val alerts = WeGrowApiClient.fetchAlerts(state.serverUrl, state.session, ::handleSessionRefreshed)
            val ledger = WeGrowApiClient.fetchPurification(state.serverUrl, state.session, ::handleSessionRefreshed)
            _uiState.update { s ->
                s.copy(
                    portfolio = portfolio ?: s.portfolio,
                    watchlistSymbols = wl ?: s.watchlistSymbols,
                    alerts = alerts ?: s.alerts,
                    purification = ledger ?: s.purification
                )
            }
        }
    }

    fun addPortfolioTransaction(symbol: String, side: String, quantity: Double, price: Double) {
        val state = _uiState.value
        val sym = symbol.trim().uppercase()
        val isUz = state.langCode == "uz"
        viewModelScope.launch {
            val updated = WeGrowApiClient.addPortfolioTransaction(
                state.serverUrl, state.session, sym, side, quantity, price, ::handleSessionRefreshed
            )
            if (updated != null) {
                val ledger = WeGrowApiClient.fetchPurification(state.serverUrl, state.session, ::handleSessionRefreshed)
                _uiState.update { s ->
                    s.copy(
                        portfolio = updated,
                        purification = ledger ?: s.purification,
                        toastMessage = if (isUz) "$sym bitimi portfelga qo'shildi ✓" else "Сделка $sym добавлена в портфель ✓"
                    )
                }
            } else {
                showToast(if (isUz) "Server bilan aloqa yo'q" else "Не удалось сохранить сделку (проверьте сервер)")
            }
        }
    }

    fun toggleWatchlist(symbol: String) {
        val sym = symbol.uppercase()
        val currentlyIn = sym in _uiState.value.watchlistSymbols
        val nextSet = if (currentlyIn) _uiState.value.watchlistSymbols - sym else _uiState.value.watchlistSymbols + sym
        val isUz = _uiState.value.langCode == "uz"
        _uiState.update {
            it.copy(
                watchlistSymbols = nextSet,
                toastMessage = if (currentlyIn) {
                    if (isUz) "$sym tanlanganlardan olib tashlandi" else "$sym удалена из избранного"
                } else {
                    if (isUz) "$sym tanlanganlarga qo'shildi ★" else "$sym добавлена в избранное ★"
                }
            )
        }
        val state = _uiState.value
        viewModelScope.launch {
            WeGrowApiClient.setWatchlistSymbol(state.serverUrl, state.session, sym, add = !currentlyIn, ::handleSessionRefreshed)
        }
    }

    fun createPriceAlert(symbol: String, condition: String, targetPrice: Double) {
        val state = _uiState.value
        val sym = symbol.trim().uppercase()
        val isUz = state.langCode == "uz"
        viewModelScope.launch {
            val ok = WeGrowApiClient.createAlert(state.serverUrl, state.session, sym, condition, targetPrice, ::handleSessionRefreshed)
            if (ok) {
                val alerts = WeGrowApiClient.fetchAlerts(state.serverUrl, state.session, ::handleSessionRefreshed)
                _uiState.update { s ->
                    s.copy(
                        alerts = alerts ?: s.alerts,
                        toastMessage = if (isUz) "Alert yaratildi: $sym $${String.format("%.2f", targetPrice)} 🔔"
                        else "Алерт создан: $sym $${String.format("%.2f", targetPrice)} 🔔"
                    )
                }
            } else {
                // Local fallback alert when offline so UI remains interactive
                val localId = ((_uiState.value.alerts.maxOfOrNull { it.id } ?: 100) + 1)
                val localAlert = uz.wegrow.invest.data.PriceAlertItem(
                    id = localId,
                    symbol = sym,
                    condition = condition,
                    targetPrice = targetPrice,
                    active = true,
                    triggered = false,
                    message = "$condition $${String.format("%.2f", targetPrice)}"
                )
                _uiState.update { s ->
                    s.copy(
                        alerts = listOf(localAlert) + s.alerts,
                        toastMessage = if (isUz) "Lokal alert saqlandi: $sym 🔔" else "Алерт сохранён локально: $sym 🔔"
                    )
                }
            }
        }
    }

    fun deletePriceAlert(alertId: Int) {
        _uiState.update { s -> s.copy(alerts = s.alerts.filterNot { it.id == alertId }) }
        val state = _uiState.value
        viewModelScope.launch {
            WeGrowApiClient.deleteAlert(state.serverUrl, state.session, alertId, ::handleSessionRefreshed)
        }
    }

    fun clearPurification(item: uz.wegrow.invest.data.PurificationItem) {
        val state = _uiState.value
        val isUz = state.langCode == "uz"
        viewModelScope.launch {
            val ledger = WeGrowApiClient.clearPurification(state.serverUrl, state.session, item, ::handleSessionRefreshed)
            if (ledger != null) {
                _uiState.update {
                    it.copy(
                        purification = ledger,
                        toastMessage = if (isUz) "${item.symbol} poklash bajarildi ✓" else "Очищение ${item.symbol} отмечено ✓"
                    )
                }
            }
        }
    }

    fun deleteGoal(goalId: Int) {
        val isUz = _uiState.value.langCode == "uz"
        _uiState.update { s ->
            s.copy(
                goals = s.goals.filterNot { it.id == goalId },
                toastMessage = if (isUz) "Maqsad o'chirildi" else "Цель удалена"
            )
        }
        val state = _uiState.value
        viewModelScope.launch {
            WeGrowApiClient.deleteGoal(state.serverUrl, state.session, goalId, ::handleSessionRefreshed)
        }
    }

    fun syncCourseLms(courseId: Int = 1) {
        val state = _uiState.value
        viewModelScope.launch {
            val res = WeGrowApiClient.fetchCourseLessons(state.serverUrl, state.session, courseId, ::handleSessionRefreshed)
            if (res != null) {
                val (purchased, lessons) = res
                _uiState.update { s ->
                    s.copy(
                        courseLessons = lessons,
                        courses = s.courses.map { c ->
                            if (c.id == courseId) c.copy(purchased = purchased) else c
                        }
                    )
                }
            }
        }
    }

    fun openLesson(courseId: Int, lessonId: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            val detail = WeGrowApiClient.fetchLessonDetail(state.serverUrl, state.session, courseId, lessonId, ::handleSessionRefreshed)
            if (detail != null) {
                _uiState.update { it.copy(activeLesson = detail) }
            }
        }
    }

    fun closeLesson() {
        _uiState.update { it.copy(activeLesson = null) }
    }

    fun completeLesson(courseId: Int, lessonId: Int, quizAnswers: Map<String, Int>?) {
        val state = _uiState.value
        val isUz = state.langCode == "uz"
        viewModelScope.launch {
            WeGrowApiClient.submitLessonProgress(
                state.serverUrl, state.session, courseId, lessonId, true, quizAnswers, ::handleSessionRefreshed
            )
            syncCourseLms(courseId)
            _uiState.update {
                it.copy(
                    activeLesson = null,
                    toastMessage = if (isUz) "Dars yakunlandi ✓" else "Урок отмечен пройденным ✓"
                )
            }
        }
    }

    fun startCourseOrder(courseId: Int, provider: String) {
        val state = _uiState.value
        val isUz = state.langCode == "uz"
        viewModelScope.launch {
            val order = WeGrowApiClient.createPaymentOrder(state.serverUrl, state.session, courseId, provider, ::handleSessionRefreshed)
            if (order != null) {
                _uiState.update { s ->
                    s.copy(
                        pendingOrder = order,
                        orders = listOf(order) + s.orders.filterNot { it.orderUid == order.orderUid },
                        toastMessage = if (isUz) "Buyurtma ${order.orderUid} yaratildi" else "Заказ ${order.orderUid} создан"
                    )
                }
            }
        }
    }

    fun completeSandboxOrder(orderUid: String, courseId: Int = 1) {
        val state = _uiState.value
        val isUz = state.langCode == "uz"
        viewModelScope.launch {
            val paid = WeGrowApiClient.sandboxPayOrder(state.serverUrl, state.session, orderUid, ::handleSessionRefreshed)
            if (paid != null) {
                _uiState.update { s ->
                    s.copy(
                        pendingOrder = paid,
                        orders = s.orders.map { if (it.orderUid == paid.orderUid) paid else it },
                        toastMessage = if (isUz) "To'lov tasdiqlandi! Kurs ochildi ✓" else "Оплата подтверждена! Доступ открыт ✓"
                    )
                }
                syncCourseLms(courseId)
                syncWithServer()
            }
        }
    }

    // ---- v4.1: Content, Stories, Notifications, Gold Club, Streams ----

    fun syncContentAndOrders() {
        val state = _uiState.value
        viewModelScope.launch {
            val orders = WeGrowApiClient.fetchUserOrders(state.serverUrl, state.session, ::handleSessionRefreshed)
            val stories = WeGrowApiClient.fetchStories(state.serverUrl, state.session, ::handleSessionRefreshed)
            val brokers = WeGrowApiClient.fetchBrokers(state.serverUrl, state.session, ::handleSessionRefreshed)
            val streams = WeGrowApiClient.fetchStreams(state.serverUrl, state.session, ::handleSessionRefreshed)
            _uiState.update { s ->
                s.copy(
                    orders = orders ?: s.orders,
                    stories = stories?.takeIf { it.isNotEmpty() } ?: s.stories,
                    brokers = brokers?.takeIf { it.isNotEmpty() } ?: s.brokers,
                    streams = streams?.takeIf { it.isNotEmpty() } ?: s.streams
                )
            }
        }
    }

    fun openNewsArticle(newsId: Int) {
        _uiState.update { it.copy(activeNewsId = newsId) }
    }

    fun openStoryDeck(deckId: String) {
        _uiState.update {
            it.copy(
                activeStoryDeckId = deckId,
                seenStoryIds = it.seenStoryIds + deckId
            )
        }
    }

    fun closeStoryDeck() {
        _uiState.update { it.copy(activeStoryDeckId = null) }
    }

    fun markAllNotificationsRead() {
        _uiState.update { s ->
            s.copy(readNotificationIds = s.readNotificationIds + s.notifications.map { it.id })
        }
    }

    fun toggleStreamReminder(streamId: Int) {
        val isUz = _uiState.value.langCode == "uz"
        _uiState.update { s ->
            val exists = streamId in s.remindedStreamIds
            val next = if (exists) s.remindedStreamIds - streamId else s.remindedStreamIds + streamId
            s.copy(
                remindedStreamIds = next,
                toastMessage = if (exists) {
                    if (isUz) "Efir eslatmasi o'chirildi" else "Напоминание об эфире отключено"
                } else {
                    if (isUz) "Efir boshlanishidan oldin eslatamiz 🔔" else "Напомним перед началом эфира 🔔"
                }
            )
        }
    }

    fun joinGoldClubSandbox(planLabel: String) {
        val isUz = _uiState.value.langCode == "uz"
        _uiState.update {
            it.copy(
                goldClubJoined = true,
                toastMessage = if (isUz) "Gold Club ($planLabel) faollashtirildi 👑"
                else "Подписка Gold Club ($planLabel) активирована 👑"
            )
        }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}

