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
import uz.wegrow.invest.data.AuthSession
import uz.wegrow.invest.data.AuthSessionStore
import uz.wegrow.invest.data.CourseItem
import uz.wegrow.invest.data.FinancialGoal
import uz.wegrow.invest.data.MarketIndex
import uz.wegrow.invest.data.NewsItem
import uz.wegrow.invest.data.OtpRequestResult
import uz.wegrow.invest.data.SectorHeatTile
import uz.wegrow.invest.data.StockChartData
import uz.wegrow.invest.data.StockItem
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
    val pendingOtp: OtpRequestResult? = null
) {
    val activeStock: StockItem
        get() = stocks.find { it.symbol == activeStockSymbol } ?: stocks.firstOrNull()
            ?: WeGrowRepository.defaultFallbackData().stocks.first()
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
            activeStockSymbol = fallback.stocks.first().symbol
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
}
