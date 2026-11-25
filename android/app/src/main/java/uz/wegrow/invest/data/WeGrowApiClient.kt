package uz.wegrow.invest.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class AuthLoginPayload(
    val session: AuthSession,
    val profile: UserProfile
)

data class StocksSearchPayload(
    val stocks: List<StockItem>,
    val total: Int,
    val page: Int,
    val totalPages: Int,
    val sectors: List<String>
)

object WeGrowApiClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun cleanBase(baseUrl: String): String = baseUrl.trim().trimEnd('/')

    private fun buildRequest(
        url: String,
        method: String = "GET",
        accessToken: String? = null,
        bodyJson: String? = null
    ): Request {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
        if (!accessToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $accessToken")
        }
        when (method.uppercase()) {
            "POST" -> builder.post((bodyJson ?: "{}").toRequestBody(jsonMediaType))
            "PUT" -> builder.put((bodyJson ?: "{}").toRequestBody(jsonMediaType))
            "DELETE" -> builder.delete()
            else -> builder.get()
        }
        return builder.build()
    }

    private suspend fun executeWithRefresh(
        baseUrl: String,
        path: String,
        method: String = "GET",
        session: AuthSession,
        bodyJson: String? = null,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): Pair<Int, String?> = withContext(Dispatchers.IO) {
        val base = cleanBase(baseUrl)
        if (base.isBlank()) return@withContext (-1 to null)
        try {
            val firstReq = buildRequest("$base$path", method, session.accessToken, bodyJson)
            httpClient.newCall(firstReq).execute().use { resp ->
                if (resp.code == 401 && session.refreshToken.isNotBlank()) {
                    val refreshed = refreshTokensInternal(base, session.refreshToken)
                    if (refreshed != null) {
                        onSessionRefreshed?.invoke(refreshed.session)
                        val retryReq = buildRequest("$base$path", method, refreshed.session.accessToken, bodyJson)
                        httpClient.newCall(retryReq).execute().use { retryResp ->
                            return@withContext (retryResp.code to retryResp.body?.string())
                        }
                    }
                }
                return@withContext (resp.code to resp.body?.string())
            }
        } catch (_: Exception) {
            return@withContext (-1 to null)
        }
    }

    private fun refreshTokensInternal(cleanBaseUrl: String, refreshToken: String): AuthLoginPayload? {
        return try {
            val payload = buildJsonObject { put("refresh_token", refreshToken) }.toString()
            val req = buildRequest("$cleanBaseUrl/api/v1/auth/refresh", "POST", null, payload)
            httpClient.newCall(req).execute().use { resp ->
                if (resp.code != 200) return null
                val raw = resp.body?.string() ?: return null
                parseAuthLoginJson(raw)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun requestOtp(baseUrl: String, identifier: String): Result<OtpRequestResult> =
        withContext(Dispatchers.IO) {
            val base = cleanBase(baseUrl)
            if (base.isBlank()) return@withContext Result.failure(IllegalArgumentException("Server URL is empty"))
            try {
                val payload = buildJsonObject { put("identifier", identifier.trim()) }.toString()
                val req = buildRequest("$base/api/v1/auth/otp/request", "POST", null, payload)
                httpClient.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (resp.code !in 200..299) {
                        return@withContext Result.failure(IllegalStateException(extractErrorDetail(raw, resp.code)))
                    }
                    val obj = json.parseToJsonElement(raw).jsonObject
                    Result.success(
                        OtpRequestResult(
                            identifier = obj.str("identifier", identifier.trim()),
                            channel = obj.str("channel", "phone"),
                            expiresInSeconds = obj.intVal("expires_in", 300),
                            devCode = obj["dev_code"]?.jsonPrimitive?.content
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun verifyOtp(
        baseUrl: String,
        identifier: String,
        code: String,
        language: String = "ru"
    ): Result<AuthLoginPayload> = withContext(Dispatchers.IO) {
        val base = cleanBase(baseUrl)
        if (base.isBlank()) return@withContext Result.failure(IllegalArgumentException("Server URL is empty"))
        try {
            val payload = buildJsonObject {
                put("identifier", identifier.trim())
                put("code", code.trim())
                put("language", language)
            }.toString()
            val req = buildRequest("$base/api/v1/auth/otp/verify", "POST", null, payload)
            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (resp.code !in 200..299) {
                    return@withContext Result.failure(IllegalStateException(extractErrorDetail(raw, resp.code)))
                }
                val parsed = parseAuthLoginJson(raw)
                    ?: return@withContext Result.failure(IllegalStateException("Malformed auth response"))
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginAsGuest(baseUrl: String): Result<AuthLoginPayload> = withContext(Dispatchers.IO) {
        val base = cleanBase(baseUrl)
        if (base.isBlank()) return@withContext Result.failure(IllegalArgumentException("Server URL is empty"))
        try {
            val req = buildRequest("$base/api/v1/auth/guest", "POST", null, "{}")
            httpClient.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (resp.code !in 200..299) {
                    return@withContext Result.failure(IllegalStateException(extractErrorDetail(raw, resp.code)))
                }
                val parsed = parseAuthLoginJson(raw)
                    ?: return@withContext Result.failure(IllegalStateException("Malformed guest response"))
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(baseUrl: String, refreshToken: String) = withContext(Dispatchers.IO) {
        val base = cleanBase(baseUrl)
        if (base.isBlank() || refreshToken.isBlank()) return@withContext
        try {
            val payload = buildJsonObject { put("refresh_token", refreshToken) }.toString()
            val req = buildRequest("$base/api/v1/auth/logout", "POST", null, payload)
            httpClient.newCall(req).execute().close()
        } catch (_: Exception) {
        }
    }

    suspend fun fetchBootstrap(
        baseUrl: String,
        session: AuthSession,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): BootstrapPayload {
        val path = if (session.isAuthenticated) "/api/v1/bootstrap" else "/api/bootstrap"
        val (code, raw) = executeWithRefresh(baseUrl, path, "GET", session, null, onSessionRefreshed)
        if (code in 200..299 && !raw.isNullOrBlank()) {
            parseBootstrapJson(raw)?.let { return it }
        }
        return WeGrowRepository.defaultFallbackData()
    }

    suspend fun searchStocks(
        baseUrl: String,
        session: AuthSession,
        query: String? = null,
        halalOnly: Boolean = false,
        sector: String? = null,
        page: Int = 1,
        pageSize: Int = 60,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): StocksSearchPayload? {
        val params = mutableListOf("page=$page", "page_size=$pageSize")
        if (!query.isNullOrBlank()) {
            params += "q=${URLEncoder.encode(query.trim(), "UTF-8")}"
        }
        if (halalOnly) params += "halal_only=true"
        if (!sector.isNullOrBlank() && sector != "ALL") {
            params += "sector=${URLEncoder.encode(sector, "UTF-8")}"
        }
        val path = "/api/v1/stocks?${params.joinToString("&")}"
        val (code, raw) = executeWithRefresh(baseUrl, path, "GET", session, null, onSessionRefreshed)
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return parseStocksSearchJson(raw)
    }

    suspend fun fetchStockChart(
        baseUrl: String,
        session: AuthSession,
        symbol: String,
        timeframe: String,
        refresh: Boolean = false,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): StockChartData? {
        val tf = timeframe.uppercase()
        val path = "/api/v1/stocks/${symbol.uppercase()}/chart?timeframe=$tf&refresh=$refresh"
        val (code, raw) = executeWithRefresh(baseUrl, path, "GET", session, null, onSessionRefreshed)
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return parseChartJson(raw)
    }

    suspend fun refreshStock(
        baseUrl: String,
        session: AuthSession,
        symbol: String,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): StockItem? {
        val path = "/api/v1/stocks/${symbol.uppercase()}/refresh"
        val (code, raw) = executeWithRefresh(baseUrl, path, "POST", session, "{}", onSessionRefreshed)
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return try {
            parseStockObject(json.parseToJsonElement(raw).jsonObject)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveProfile(
        baseUrl: String,
        session: AuthSession,
        profile: UserProfile,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): Boolean {
        val payload = buildJsonObject {
            put("first_name", profile.firstName)
            put("last_name", profile.lastName)
            put("middle_name", profile.middleName)
            put("birth_date", profile.birthDate)
            put("gender", profile.gender)
            put("language", profile.language)
            put("dark_theme", profile.darkTheme)
        }.toString()
        val (path, method) = if (session.isAuthenticated) {
            "/api/v1/auth/me" to "PUT"
        } else {
            "/api/profile" to "POST"
        }
        val (code, _) = executeWithRefresh(baseUrl, path, method, session, payload, onSessionRefreshed)
        return code in 200..299
    }

    suspend fun createGoal(
        baseUrl: String,
        session: AuthSession,
        title: String,
        targetAmount: Double,
        monthlyContribution: Double,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): FinancialGoal? {
        val payload = buildJsonObject {
            put("title", title)
            put("target_amount", targetAmount)
            put("current_amount", 0.0)
            put("monthly_contribution", monthlyContribution)
            put("deadline_label", "Декабрь 2027")
        }.toString()
        val path = if (session.isAuthenticated) "/api/v1/goals" else "/api/goals"
        val (code, raw) = executeWithRefresh(baseUrl, path, "POST", session, payload, onSessionRefreshed)
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return try {
            val g = json.parseToJsonElement(raw).jsonObject["goal"]?.jsonObject ?: return null
            parseGoalObject(g)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun contributeGoal(
        baseUrl: String,
        session: AuthSession,
        goalId: Int,
        amount: Double,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): Boolean {
        val payload = buildJsonObject { put("amount", amount) }.toString()
        val path = if (session.isAuthenticated) "/api/v1/goals/$goalId/contribute" else "/api/goals/$goalId/contribute"
        val (code, _) = executeWithRefresh(baseUrl, path, "POST", session, payload, onSessionRefreshed)
        return code in 200..299
    }

    suspend fun purchaseCourse(
        baseUrl: String,
        session: AuthSession,
        courseId: Int,
        provider: String,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): Boolean {
        val payload = buildJsonObject { put("provider", provider) }.toString()
        val path = if (session.isAuthenticated) "/api/v1/courses/$courseId/purchase" else "/api/courses/$courseId/purchase"
        val (code, _) = executeWithRefresh(baseUrl, path, "POST", session, payload, onSessionRefreshed)
        return code in 200..299
    }

    // ---- Pure JVM-testable JSON Parsers ----

    internal fun parseAuthLoginJson(raw: String): AuthLoginPayload? {
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val userObj = obj["user"]?.jsonObject ?: return null
            val profile = parseProfileObject(userObj)
            val session = AuthSession(
                accessToken = obj.str("access_token", ""),
                refreshToken = obj.str("refresh_token", ""),
                identifier = profile.phone ?: profile.email ?: if (profile.isGuest) "guest" else "",
                isGuest = profile.isGuest
            )
            if (session.accessToken.isBlank()) return null
            AuthLoginPayload(session = session, profile = profile)
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseStocksSearchJson(raw: String): StocksSearchPayload? {
        return try {
            val root = json.parseToJsonElement(raw).jsonObject
            val stocksArr = root["stocks"]?.jsonArray ?: JsonArray(emptyList())
            val stocks = stocksArr.map { parseStockObject(it.jsonObject) }
            val pag = root["pagination"]?.jsonObject
            val sectors = root["sectors"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            StocksSearchPayload(
                stocks = stocks,
                total = pag?.intVal("total", stocks.size) ?: stocks.size,
                page = pag?.intVal("page", 1) ?: 1,
                totalPages = pag?.intVal("total_pages", 1) ?: 1,
                sectors = sectors
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseChartJson(raw: String): StockChartData? {
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            val pts = obj["points"]?.jsonArray?.mapNotNull { it.jsonPrimitive.floatOrNull } ?: emptyList()
            if (pts.size < 2) return null
            StockChartData(
                symbol = obj.str("symbol", ""),
                timeframe = obj.str("timeframe", "1M"),
                points = pts,
                currentPrice = obj.doubleVal("current_price", pts.last().toDouble()),
                periodChangeAbs = obj.doubleVal("period_change_abs", 0.0),
                periodChangePct = obj.doubleVal("period_change_pct", 0.0),
                dataSource = obj.str("data_source", "seed")
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseBootstrapJson(raw: String): BootstrapPayload? {
        return try {
            val root = json.parseToJsonElement(raw).jsonObject
            val profileObj = root["profile"]?.jsonObject ?: return null
            val profile = parseProfileObject(profileObj)

            val stocks = root["stocks"]?.jsonArray?.map { parseStockObject(it.jsonObject) } ?: emptyList()
            val goals = root["goals"]?.jsonArray?.map { parseGoalObject(it.jsonObject) } ?: emptyList()
            val courses = root["courses"]?.jsonArray?.map { parseCourseObject(it.jsonObject) } ?: emptyList()
            val news = root["news"]?.jsonArray?.map { elem ->
                val o = elem.jsonObject
                NewsItem(
                    id = o.intVal("id", 0),
                    title = o.str("title", ""),
                    titleUz = o.str("title_uz", o.str("title", "")),
                    source = o.str("source", ""),
                    sourceUz = o.str("source_uz", o.str("source", "")),
                    ticker = o.str("ticker", "AAPL"),
                    tag = o.str("tag", "")
                )
            } ?: emptyList()

            val mo = root["market_overview"]?.jsonObject
            val indices = mo?.get("indices")?.jsonArray?.map { elem ->
                val o = elem.jsonObject
                MarketIndex(
                    name = o.str("name", ""),
                    value = o.str("value", ""),
                    changePct = o.str("change_pct", ""),
                    isUp = o.boolVal("is_up", true)
                )
            } ?: emptyList()

            val heatmap = mo?.get("heatmap")?.jsonArray?.map { elem ->
                val o = elem.jsonObject
                SectorHeatTile(
                    name = o.str("name", ""),
                    nameUz = o.str("name_uz", o.str("name", "")),
                    changePct = o.str("change_pct", ""),
                    tone = o.str("tone", "UP_1"),
                    weight = o.floatVal("weight", 1f)
                )
            } ?: emptyList()

            val fallback = WeGrowRepository.defaultFallbackData()
            BootstrapPayload(
                profile = profile,
                stocks = stocks.ifEmpty { fallback.stocks },
                goals = goals.ifEmpty { fallback.goals },
                courses = courses.ifEmpty { fallback.courses },
                news = news.ifEmpty { fallback.news },
                indices = indices.ifEmpty { fallback.indices },
                heatmap = heatmap.ifEmpty { fallback.heatmap },
                marketOpenTextRu = mo?.str("status_text", fallback.marketOpenTextRu) ?: fallback.marketOpenTextRu,
                marketOpenTextUz = mo?.str("status_text_uz", fallback.marketOpenTextUz) ?: fallback.marketOpenTextUz,
                online = true
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseProfileObject(o: JsonObject): UserProfile {
        return UserProfile(
            id = o.intVal("id", 1),
            phone = o.nullableStr("phone"),
            email = o.nullableStr("email"),
            firstName = o.str("first_name", "Азиз"),
            lastName = o.str("last_name", "Каримов"),
            middleName = o.str("middle_name", ""),
            birthDate = o.str("birth_date", "15.04.1996"),
            gender = o.str("gender", "male"),
            language = o.str("language", "ru"),
            darkTheme = o.boolVal("dark_theme", true),
            isGuest = o.boolVal("is_guest", false)
        )
    }

    internal fun parseStockObject(o: JsonObject): StockItem {
        val status = when (o.str("halal_status", "HALAL").uppercase()) {
            "DOUBTFUL" -> HalalStatus.DOUBTFUL
            "HARAM" -> HalalStatus.HARAM
            else -> HalalStatus.HALAL
        }
        val segments = o["revenue_segments"]?.jsonArray?.map { elem ->
            val seg = elem.jsonObject
            RevenueSegment(
                label = seg.str("label", ""),
                labelUz = seg.str("label_uz", seg.str("label", "")),
                pct = seg.floatVal("pct", 0f),
                type = seg.str("type", "halal")
            )
        } ?: emptyList()

        val criteria = o["criteria"]?.jsonArray?.map { elem ->
            val c = elem.jsonObject
            val cStatus = when (c.str("status", "HALAL").uppercase()) {
                "DOUBTFUL" -> HalalStatus.DOUBTFUL
                "HARAM" -> HalalStatus.HARAM
                else -> HalalStatus.HALAL
            }
            HalalCriterion(
                id = c.str("id", ""),
                title = c.str("title", ""),
                titleUz = c.str("title_uz", c.str("title", "")),
                valueLabel = c.str("value_label", ""),
                subtitle = c.str("subtitle", ""),
                subtitleUz = c.str("subtitle_uz", c.str("subtitle", "")),
                pct = c.floatVal("pct", 0f),
                limitPct = c.floatVal("limit_pct", 30f),
                status = cStatus
            )
        } ?: emptyList()

        val chart1m = o["chart_1m"]?.jsonArray?.mapNotNull { it.jsonPrimitive.floatOrNull } ?: emptyList()
        val chartsMap = mutableMapOf<String, List<Float>>()
        o["charts"]?.let { chartsElem ->
            if (chartsElem is JsonObject) {
                for ((tf, arr) in chartsElem) {
                    if (arr is JsonArray) {
                        val pts = arr.mapNotNull { it.jsonPrimitive.floatOrNull }
                        if (pts.size >= 2) chartsMap[tf.uppercase()] = pts
                    }
                }
            }
        }
        if ("1M" !in chartsMap && chart1m.size >= 2) {
            chartsMap["1M"] = chart1m
        }

        return StockItem(
            symbol = o.str("symbol", ""),
            name = o.str("name", ""),
            exchange = o.str("exchange", "NASDAQ"),
            sector = o.str("sector", ""),
            sectorUz = o.str("sector_uz", o.str("sector", "")),
            price = o.doubleVal("price", 0.0),
            changeAbs = o.doubleVal("change_abs", 0.0),
            changePct = o.doubleVal("change_pct", 0.0),
            halalStatus = status,
            checkedDate = o.str("checked_date", ""),
            marketCap = o.str("market_cap", ""),
            peRatio = o.str("pe_ratio", ""),
            dividendAnnual = o.str("dividend_annual", "$0,00"),
            dividendYield = o.str("dividend_yield", "0,00%"),
            purificationPerDividend = o.doubleVal("purification_per_dividend", 0.0),
            dividendPerQuarter = o.doubleVal("dividend_per_quarter", 0.0),
            verdictSummary = o.str("verdict_summary", ""),
            verdictSubtitle = o.str("verdict_subtitle", ""),
            reportPeriod = o.str("report_period", ""),
            revenueSegments = segments,
            criteria = criteria,
            chart1m = chart1m,
            charts = chartsMap,
            dataSource = o.str("data_source", "seed")
        )
    }

    private fun parseGoalObject(g: JsonObject): FinancialGoal {
        return FinancialGoal(
            id = g.intVal("id", 0),
            title = g.str("title", ""),
            titleUz = g.str("title_uz", g.str("title", "")),
            targetAmount = g.doubleVal("target_amount", 1000.0),
            currentAmount = g.doubleVal("current_amount", 0.0),
            monthlyContribution = g.doubleVal("monthly_contribution", 100.0),
            deadlineLabel = g.str("deadline_label", ""),
            deadlineLabelUz = g.str("deadline_label_uz", g.str("deadline_label", "")),
            category = g.str("category", "savings"),
            icon = g.str("icon", "target")
        )
    }

    private fun parseCourseObject(c: JsonObject): CourseItem {
        val modules = c["modules"]?.jsonArray?.map { elem ->
            val m = elem.jsonObject
            CourseModule(
                number = m.intVal("number", 1),
                title = m.str("title", ""),
                titleUz = m.str("title_uz", m.str("title", "")),
                meta = m.str("meta", ""),
                metaUz = m.str("meta_uz", m.str("meta", "")),
                locked = m.boolVal("locked", true)
            )
        } ?: emptyList()

        return CourseItem(
            id = c.intVal("id", 1),
            title = c.str("title", ""),
            titleUz = c.str("title_uz", c.str("title", "")),
            level = c.str("level", ""),
            levelUz = c.str("level_uz", c.str("level", "")),
            rating = c.doubleVal("rating", 4.9),
            reviewsCount = c.intVal("reviews_count", 0),
            author = c.str("author", ""),
            authorUz = c.str("author_uz", c.str("author", "")),
            authorRole = c.str("author_role", ""),
            authorRoleUz = c.str("author_role_uz", c.str("author_role", "")),
            previewDuration = c.str("preview_duration", ""),
            modulesCount = c.intVal("modules_count", modules.size),
            lessonsCount = c.intVal("lessons_count", 0),
            accessDuration = c.str("access_duration", ""),
            accessDurationUz = c.str("access_duration_uz", c.str("access_duration", "")),
            priceUzs = c.str("price_uzs", ""),
            purchased = c.boolVal("purchased", false),
            modules = modules
        )
    }

    // ---- Sprint 2: Portfolio, Watchlist, Alerts, Purification ----

    suspend fun fetchPortfolio(
        baseUrl: String,
        session: AuthSession,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): PortfolioData? {
        val (code, raw) = executeWithRefresh(baseUrl, "/api/v1/portfolio", "GET", session, null, onSessionRefreshed)
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return parsePortfolioJson(raw)
    }

    suspend fun addPortfolioTransaction(
        baseUrl: String,
        session: AuthSession,
        symbol: String,
        side: String,
        quantity: Double,
        price: Double,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): PortfolioData? {
        val payload = buildJsonObject {
            put("symbol", symbol.trim().uppercase())
            put("side", side.uppercase())
            put("quantity", quantity)
            put("price", price)
        }.toString()
        val (code, raw) = executeWithRefresh(
            baseUrl, "/api/v1/portfolio/transactions", "POST", session, payload, onSessionRefreshed
        )
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return try {
            val pObj = json.parseToJsonElement(raw).jsonObject["portfolio"]?.jsonObject ?: return null
            parsePortfolioObject(pObj)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchWatchlistSymbols(
        baseUrl: String,
        session: AuthSession,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): Set<String>? {
        val (code, raw) = executeWithRefresh(baseUrl, "/api/v1/watchlist", "GET", session, null, onSessionRefreshed)
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return try {
            val arr = json.parseToJsonElement(raw).jsonObject["symbols"]?.jsonArray ?: return emptySet()
            arr.mapNotNull { it.jsonPrimitive.content.uppercase().ifBlank { null } }.toSet()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun setWatchlistSymbol(
        baseUrl: String,
        session: AuthSession,
        symbol: String,
        add: Boolean,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): Boolean {
        val method = if (add) "POST" else "DELETE"
        val (code, _) = executeWithRefresh(
            baseUrl, "/api/v1/watchlist/${symbol.uppercase()}", method, session, if (add) "{}" else null, onSessionRefreshed
        )
        return code in 200..299
    }

    suspend fun fetchAlerts(
        baseUrl: String,
        session: AuthSession,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): List<PriceAlertItem>? {
        val (code, raw) = executeWithRefresh(baseUrl, "/api/v1/alerts", "GET", session, null, onSessionRefreshed)
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return parseAlertsJson(raw)
    }

    suspend fun createAlert(
        baseUrl: String,
        session: AuthSession,
        symbol: String,
        condition: String,
        targetPrice: Double,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): Boolean {
        val payload = buildJsonObject {
            put("symbol", symbol.uppercase())
            put("condition", condition)
            put("target_price", targetPrice)
        }.toString()
        val (code, _) = executeWithRefresh(baseUrl, "/api/v1/alerts", "POST", session, payload, onSessionRefreshed)
        return code in 200..299
    }

    suspend fun deleteAlert(
        baseUrl: String,
        session: AuthSession,
        alertId: Int,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): Boolean {
        val (code, _) = executeWithRefresh(baseUrl, "/api/v1/alerts/$alertId", "DELETE", session, null, onSessionRefreshed)
        return code in 200..299
    }

    suspend fun fetchPurification(
        baseUrl: String,
        session: AuthSession,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): PurificationLedger? {
        val (code, raw) = executeWithRefresh(baseUrl, "/api/v1/purification", "GET", session, null, onSessionRefreshed)
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return parsePurificationJson(raw)
    }

    suspend fun clearPurification(
        baseUrl: String,
        session: AuthSession,
        item: PurificationItem,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): PurificationLedger? {
        val payload = buildJsonObject {
            put("symbol", item.symbol)
            put("amount", item.remainingAmount)
            put("quarter_label", item.quarterLabel)
            put("shares", item.shares)
            put("dividend_total", item.dividendTotal)
        }.toString()
        val (code, raw) = executeWithRefresh(
            baseUrl, "/api/v1/purification/clear", "POST", session, payload, onSessionRefreshed
        )
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return try {
            val lObj = json.parseToJsonElement(raw).jsonObject["ledger"]?.jsonObject ?: return null
            parsePurificationObject(lObj)
        } catch (_: Exception) {
            null
        }
    }

    internal fun parsePortfolioJson(raw: String): PortfolioData? {
        return try {
            parsePortfolioObject(json.parseToJsonElement(raw).jsonObject)
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePortfolioObject(root: JsonObject): PortfolioData {
        val s = root["summary"]?.jsonObject
        val summary = PortfolioSummary(
            totalValue = s?.doubleVal("total_value", 0.0) ?: 0.0,
            totalInvested = s?.doubleVal("total_invested", 0.0) ?: 0.0,
            totalPnlAbs = s?.doubleVal("total_pnl_abs", 0.0) ?: 0.0,
            totalPnlPct = s?.doubleVal("total_pnl_pct", 0.0) ?: 0.0,
            dailyPnlAbs = s?.doubleVal("daily_pnl_abs", 0.0) ?: 0.0,
            realizedPnlAbs = s?.doubleVal("realized_pnl_abs", 0.0) ?: 0.0,
            halalCompliancePct = s?.doubleVal("halal_compliance_pct", 100.0) ?: 100.0,
            positionsCount = s?.intVal("positions_count", 0) ?: 0
        )
        val positions = root["positions"]?.jsonArray?.map { elem ->
            val p = elem.jsonObject
            val st = when (p.str("halal_status", "HALAL").uppercase()) {
                "DOUBTFUL" -> HalalStatus.DOUBTFUL
                "HARAM" -> HalalStatus.HARAM
                else -> HalalStatus.HALAL
            }
            PortfolioPosition(
                symbol = p.str("symbol", ""),
                name = p.str("name", ""),
                sector = p.str("sector", ""),
                sectorUz = p.str("sector_uz", p.str("sector", "")),
                halalStatus = st,
                quantity = p.doubleVal("quantity", 0.0),
                avgPrice = p.doubleVal("avg_price", 0.0),
                currentPrice = p.doubleVal("current_price", 0.0),
                invested = p.doubleVal("invested", 0.0),
                marketValue = p.doubleVal("market_value", 0.0),
                pnlAbs = p.doubleVal("pnl_abs", 0.0),
                pnlPct = p.doubleVal("pnl_pct", 0.0),
                dailyPnl = p.doubleVal("daily_pnl", 0.0),
                weightPct = p.doubleVal("weight_pct", 0.0)
            )
        } ?: emptyList()

        val warnings = root["non_halal_warnings"]?.jsonArray?.map { elem ->
            val w = elem.jsonObject
            val st = when (w.str("halal_status", "HARAM").uppercase()) {
                "DOUBTFUL" -> HalalStatus.DOUBTFUL
                else -> HalalStatus.HARAM
            }
            PortfolioWarning(
                symbol = w.str("symbol", ""),
                name = w.str("name", ""),
                halalStatus = st,
                marketValue = w.doubleVal("market_value", 0.0),
                reason = w.str("reason", "")
            )
        } ?: emptyList()

        return PortfolioData(summary = summary, positions = positions, warnings = warnings)
    }

    internal fun parseAlertsJson(raw: String): List<PriceAlertItem> {
        return try {
            val arr = json.parseToJsonElement(raw).jsonObject["alerts"]?.jsonArray ?: return emptyList()
            arr.map { elem ->
                val a = elem.jsonObject
                PriceAlertItem(
                    id = a.intVal("id", 0),
                    symbol = a.str("symbol", ""),
                    condition = a.str("condition", "ABOVE"),
                    targetPrice = a.doubleVal("target_price", 0.0),
                    active = a.boolVal("active", true),
                    triggered = a.boolVal("triggered", false),
                    message = a.str("message", ""),
                    currentPrice = (a["current_price"] as? JsonPrimitive)?.doubleOrNull
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun parsePurificationJson(raw: String): PurificationLedger? {
        return try {
            parsePurificationObject(json.parseToJsonElement(raw).jsonObject)
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePurificationObject(obj: JsonObject): PurificationLedger {
        val items = obj["items"]?.jsonArray?.map { elem ->
            val it = elem.jsonObject
            PurificationItem(
                symbol = it.str("symbol", ""),
                name = it.str("name", ""),
                quarterLabel = it.str("quarter_label", obj.str("quarter_label", "Q4 2026")),
                shares = it.doubleVal("shares", 0.0),
                dividendTotal = it.doubleVal("dividend_total", 0.0),
                purificationDue = it.doubleVal("purification_due", 0.0),
                clearedAmount = it.doubleVal("cleared_amount", 0.0),
                remainingAmount = it.doubleVal("remaining_amount", 0.0),
                status = it.str("status", "PENDING")
            )
        } ?: emptyList()

        return PurificationLedger(
            quarterLabel = obj.str("quarter_label", "Q4 2026"),
            totalPendingUsd = obj.doubleVal("total_pending_usd", 0.0),
            totalClearedUsd = obj.doubleVal("total_cleared_usd", 0.0),
            items = items
        )
    }

    // ---- Sprint 3: LMS Lessons & Payme/Click Orders ----

    suspend fun fetchCourseLessons(
        baseUrl: String,
        session: AuthSession,
        courseId: Int,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): Pair<Boolean, List<CourseLessonSummary>>? {
        val (code, raw) = executeWithRefresh(
            baseUrl, "/api/v1/courses/$courseId", "GET", session, null, onSessionRefreshed
        )
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return parseCourseLessonsJson(raw)
    }

    suspend fun fetchLessonDetail(
        baseUrl: String,
        session: AuthSession,
        courseId: Int,
        lessonId: Int,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): LessonDetail? {
        val (code, raw) = executeWithRefresh(
            baseUrl, "/api/v1/courses/$courseId/lessons/$lessonId", "GET", session, null, onSessionRefreshed
        )
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return parseLessonDetailJson(raw)
    }

    suspend fun submitLessonProgress(
        baseUrl: String,
        session: AuthSession,
        courseId: Int,
        lessonId: Int,
        completed: Boolean,
        quizAnswers: Map<String, Int>? = null,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): Int? {
        val payload = buildJsonObject {
            put("completed", completed)
            put("position_sec", 0)
            if (quizAnswers != null) {
                put("quiz_answers", buildJsonObject {
                    quizAnswers.forEach { (k, v) -> put(k, v) }
                })
            }
        }.toString()
        val (code, raw) = executeWithRefresh(
            baseUrl,
            "/api/v1/courses/$courseId/lessons/$lessonId/progress",
            "POST",
            session,
            payload,
            onSessionRefreshed
        )
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            (obj["quiz_score"] as? JsonPrimitive)?.intOrNull ?: 100
        } catch (_: Exception) {
            100
        }
    }

    suspend fun createPaymentOrder(
        baseUrl: String,
        session: AuthSession,
        courseId: Int,
        provider: String,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): PaymentOrderInfo? {
        val payload = buildJsonObject {
            put("course_id", courseId)
            put("provider", provider.uppercase())
        }.toString()
        val (code, raw) = executeWithRefresh(
            baseUrl, "/api/v1/orders", "POST", session, payload, onSessionRefreshed
        )
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return parsePaymentOrderJson(raw)
    }

    suspend fun sandboxPayOrder(
        baseUrl: String,
        session: AuthSession,
        orderUid: String,
        onSessionRefreshed: ((AuthSession) -> Unit)? = null
    ): PaymentOrderInfo? {
        val (code, raw) = executeWithRefresh(
            baseUrl, "/api/v1/orders/$orderUid/sandbox-pay", "POST", session, "{}", onSessionRefreshed
        )
        if (code !in 200..299 || raw.isNullOrBlank()) return null
        return parsePaymentOrderJson(raw)
    }

    internal fun parseCourseLessonsJson(raw: String): Pair<Boolean, List<CourseLessonSummary>>? {
        return try {
            val root = json.parseToJsonElement(raw).jsonObject
            val purchased = root.boolVal("purchased", false)
            val list = root["lessons"]?.jsonArray?.map { elem ->
                val l = elem.jsonObject
                CourseLessonSummary(
                    id = l.intVal("id", 0),
                    courseId = l.intVal("course_id", 1),
                    moduleNumber = l.intVal("module_number", 1),
                    lessonNumber = l.intVal("lesson_number", 1),
                    title = l.str("title", ""),
                    titleUz = l.str("title_uz", l.str("title", "")),
                    lessonType = l.str("lesson_type", "article"),
                    durationMin = l.intVal("duration_min", 10),
                    isPreview = l.boolVal("is_preview", false),
                    locked = l.boolVal("locked", true),
                    completed = l.boolVal("completed", false),
                    quizScore = (l["quiz_score"] as? JsonPrimitive)?.intOrNull
                )
            } ?: emptyList()
            purchased to list
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseLessonDetailJson(raw: String): LessonDetail? {
        return try {
            val l = json.parseToJsonElement(raw).jsonObject
            val quiz = l["quiz"]?.jsonArray?.map { elem ->
                val q = elem.jsonObject
                LessonQuizQuestion(
                    id = q.str("id", ""),
                    question = q.str("question", ""),
                    questionUz = q.str("question_uz", q.str("question", "")),
                    options = q["options"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                )
            } ?: emptyList()
            LessonDetail(
                id = l.intVal("id", 0),
                courseId = l.intVal("course_id", 1),
                title = l.str("title", ""),
                titleUz = l.str("title_uz", l.str("title", "")),
                lessonType = l.str("lesson_type", "article"),
                durationMin = l.intVal("duration_min", 10),
                videoUrl = l.nullableStr("video_url"),
                contentMd = l.str("content_md", ""),
                contentMdUz = l.str("content_md_uz", l.str("content_md", "")),
                quiz = quiz,
                completed = l.boolVal("completed", false),
                quizScore = (l["quiz_score"] as? JsonPrimitive)?.intOrNull
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun parsePaymentOrderJson(raw: String): PaymentOrderInfo? {
        return try {
            val root = json.parseToJsonElement(raw).jsonObject
            val o = root["order"]?.jsonObject ?: root
            PaymentOrderInfo(
                orderUid = o.str("order_uid", ""),
                courseId = o.intVal("course_id", 1),
                provider = o.str("provider", "PAYME"),
                amountUzs = o.intVal("amount_uzs", 0),
                status = o.str("status", "PENDING"),
                checkoutUrl = o.str("checkout_url", ""),
                sandboxEnabled = o.boolVal("sandbox_enabled", true)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractErrorDetail(raw: String, code: Int): String {
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            obj.str("detail", "HTTP $code")
        } catch (_: Exception) {
            "HTTP $code"
        }
    }

    private fun JsonObject.str(key: String, default: String): String {
        val el: JsonElement = this[key] ?: return default
        val prim = el as? JsonPrimitive ?: return default
        return if (prim.isString) prim.content else prim.content
    }

    private fun JsonObject.nullableStr(key: String): String? {
        val el = this[key] ?: return null
        val prim = el as? JsonPrimitive ?: return null
        if (prim.content == "null" && !prim.isString) return null
        return prim.content.ifBlank { null }
    }

    private fun JsonObject.intVal(key: String, default: Int): Int =
        (this[key] as? JsonPrimitive)?.intOrNull ?: default

    private fun JsonObject.doubleVal(key: String, default: Double): Double =
        (this[key] as? JsonPrimitive)?.doubleOrNull ?: default

    private fun JsonObject.floatVal(key: String, default: Float): Float =
        (this[key] as? JsonPrimitive)?.floatOrNull ?: default

    private fun JsonObject.boolVal(key: String, default: Boolean): Boolean =
        (this[key] as? JsonPrimitive)?.booleanOrNull ?: default
}
