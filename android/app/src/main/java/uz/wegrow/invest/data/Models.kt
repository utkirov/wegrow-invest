package uz.wegrow.invest.data

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val id: Int = 1,
    val phone: String? = null,
    val email: String? = null,
    val firstName: String = "Азиз",
    val lastName: String = "Каримов",
    val middleName: String = "Рустамович",
    val birthDate: String = "15.04.1996",
    val gender: String = "male",
    val language: String = "ru",
    val darkTheme: Boolean = true,
    val isGuest: Boolean = false
) {
    val fullName: String
        get() = "$firstName $lastName".trim()

    val contactLabel: String
        get() = phone ?: email ?: if (isGuest) "Гостевой доступ" else birthDate

    val initials: String
        get() {
            val f = firstName.firstOrNull()?.uppercaseChar() ?: 'А'
            val l = lastName.firstOrNull()?.uppercaseChar() ?: 'К'
            return "$f$l"
        }
}

@Immutable
data class AuthSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val identifier: String = "",
    val isGuest: Boolean = false
) {
    val isAuthenticated: Boolean
        get() = accessToken.isNotBlank()
}

@Immutable
data class OtpRequestResult(
    val identifier: String,
    val channel: String,
    val expiresInSeconds: Int,
    val devCode: String? = null
)

@Immutable
data class StockChartData(
    val symbol: String,
    val timeframe: String,
    val points: List<Float>,
    val currentPrice: Double,
    val periodChangeAbs: Double,
    val periodChangePct: Double,
    val dataSource: String = "seed"
)

@Immutable
enum class HalalStatus {
    HALAL, DOUBTFUL, HARAM
}

@Immutable
data class RevenueSegment(
    val label: String,
    val labelUz: String,
    val pct: Float,
    val type: String
)

@Immutable
data class HalalCriterion(
    val id: String,
    val title: String,
    val titleUz: String,
    val valueLabel: String,
    val subtitle: String,
    val subtitleUz: String,
    val pct: Float,
    val limitPct: Float,
    val status: HalalStatus
)

@Immutable
data class StockItem(
    val symbol: String,
    val name: String,
    val exchange: String,
    val sector: String,
    val sectorUz: String,
    val price: Double,
    val changeAbs: Double,
    val changePct: Double,
    val halalStatus: HalalStatus,
    val checkedDate: String,
    val marketCap: String,
    val peRatio: String,
    val dividendAnnual: String,
    val dividendYield: String,
    val purificationPerDividend: Double,
    val dividendPerQuarter: Double,
    val verdictSummary: String,
    val verdictSubtitle: String,
    val reportPeriod: String,
    val revenueSegments: List<RevenueSegment>,
    val criteria: List<HalalCriterion>,
    val chart1m: List<Float>,
    val charts: Map<String, List<Float>> = emptyMap(),
    val dataSource: String = "seed"
) {
    fun chartForTimeframe(timeframe: String): List<Float> {
        return charts[timeframe.uppercase()]?.takeIf { it.size >= 2 } ?: chart1m
    }
}

@Immutable
data class MarketIndex(
    val name: String,
    val value: String,
    val changePct: String,
    val isUp: Boolean
)

@Immutable
data class SectorHeatTile(
    val name: String,
    val nameUz: String,
    val changePct: String,
    val tone: String,
    val weight: Float
)

@Immutable
data class FinancialGoal(
    val id: Int,
    val title: String,
    val titleUz: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val monthlyContribution: Double,
    val deadlineLabel: String,
    val deadlineLabelUz: String,
    val category: String,
    val icon: String
) {
    val progressFraction: Float
        get() = if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
}

@Immutable
data class CourseModule(
    val number: Int,
    val title: String,
    val titleUz: String,
    val meta: String,
    val metaUz: String,
    val locked: Boolean
)

@Immutable
data class CourseItem(
    val id: Int,
    val title: String,
    val titleUz: String,
    val level: String,
    val levelUz: String,
    val rating: Double,
    val reviewsCount: Int,
    val author: String,
    val authorUz: String,
    val authorRole: String,
    val authorRoleUz: String,
    val previewDuration: String,
    val modulesCount: Int,
    val lessonsCount: Int,
    val accessDuration: String,
    val accessDurationUz: String,
    val priceUzs: String,
    val purchased: Boolean,
    val modules: List<CourseModule>
)

@Immutable
data class NewsItem(
    val id: Int,
    val title: String,
    val titleUz: String,
    val source: String,
    val sourceUz: String,
    val ticker: String,
    val tag: String
)

@Immutable
data class PortfolioSummary(
    val totalValue: Double = 0.0,
    val totalInvested: Double = 0.0,
    val totalPnlAbs: Double = 0.0,
    val totalPnlPct: Double = 0.0,
    val dailyPnlAbs: Double = 0.0,
    val realizedPnlAbs: Double = 0.0,
    val halalCompliancePct: Double = 100.0,
    val positionsCount: Int = 0
)

@Immutable
data class PortfolioPosition(
    val symbol: String,
    val name: String,
    val sector: String,
    val sectorUz: String,
    val halalStatus: HalalStatus,
    val quantity: Double,
    val avgPrice: Double,
    val currentPrice: Double,
    val invested: Double,
    val marketValue: Double,
    val pnlAbs: Double,
    val pnlPct: Double,
    val dailyPnl: Double,
    val weightPct: Double = 0.0
)

@Immutable
data class PortfolioWarning(
    val symbol: String,
    val name: String,
    val halalStatus: HalalStatus,
    val marketValue: Double,
    val reason: String
)

@Immutable
data class PortfolioData(
    val summary: PortfolioSummary = PortfolioSummary(),
    val positions: List<PortfolioPosition> = emptyList(),
    val warnings: List<PortfolioWarning> = emptyList()
)

@Immutable
data class PriceAlertItem(
    val id: Int,
    val symbol: String,
    val condition: String,
    val targetPrice: Double,
    val active: Boolean,
    val triggered: Boolean,
    val message: String = "",
    val currentPrice: Double? = null
)

@Immutable
data class PurificationItem(
    val symbol: String,
    val name: String,
    val quarterLabel: String,
    val shares: Double,
    val dividendTotal: Double,
    val purificationDue: Double,
    val clearedAmount: Double,
    val remainingAmount: Double,
    val status: String
)

@Immutable
data class PurificationLedger(
    val quarterLabel: String = "Q4 2026",
    val totalPendingUsd: Double = 0.0,
    val totalClearedUsd: Double = 0.0,
    val items: List<PurificationItem> = emptyList()
)

