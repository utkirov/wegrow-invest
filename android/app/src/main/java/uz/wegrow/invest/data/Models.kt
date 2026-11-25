package uz.wegrow.invest.data

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val firstName: String = "Азиз",
    val lastName: String = "Каримов",
    val middleName: String = "Рустамович",
    val birthDate: String = "15.04.1996",
    val gender: String = "male",
    val language: String = "ru",
    val darkTheme: Boolean = true
) {
    val fullName: String
        get() = "$firstName $lastName".trim()

    val initials: String
        get() {
            val f = firstName.firstOrNull()?.uppercaseChar() ?: 'А'
            val l = lastName.firstOrNull()?.uppercaseChar() ?: 'К'
            return "$f$l"
        }
}

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
    val chart1m: List<Float>
)

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
