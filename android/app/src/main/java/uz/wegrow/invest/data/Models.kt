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
    val tag: String,
    val summary: String = "",
    val summaryUz: String = "",
    val body: String = "",
    val bodyUz: String = "",
    val readMin: Int = 3,
    val publishedAt: String = "26.11.2026"
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

@Immutable
data class CourseLessonSummary(
    val id: Int,
    val courseId: Int,
    val moduleNumber: Int,
    val lessonNumber: Int,
    val title: String,
    val titleUz: String,
    val lessonType: String,
    val durationMin: Int,
    val isPreview: Boolean,
    val locked: Boolean,
    val completed: Boolean,
    val quizScore: Int? = null
)

@Immutable
data class LessonQuizQuestion(
    val id: String,
    val question: String,
    val questionUz: String,
    val options: List<String>
)

@Immutable
data class LessonDetail(
    val id: Int,
    val courseId: Int,
    val title: String,
    val titleUz: String,
    val lessonType: String,
    val durationMin: Int,
    val videoUrl: String?,
    val contentMd: String,
    val contentMdUz: String,
    val quiz: List<LessonQuizQuestion>,
    val completed: Boolean,
    val quizScore: Int? = null
)

@Immutable
data class PaymentOrderInfo(
    val orderUid: String,
    val courseId: Int,
    val provider: String,
    val amountUzs: Int,
    val status: String,
    val checkoutUrl: String,
    val sandboxEnabled: Boolean = true,
    val courseTitle: String = "Основы инвестирования в акции США",
    val courseTitleUz: String = "AQSh aksiyalariga investitsiya asoslari",
    val createdAt: String? = null,
    val paidAt: String? = null
)

@Immutable
data class StorySlide(
    val eyebrow: String,
    val eyebrowUz: String,
    val title: String,
    val titleUz: String,
    val body: String,
    val bodyUz: String,
    val ctaLabel: String,
    val ctaLabelUz: String,
    val actionRoute: String
)

@Immutable
data class StoryDeck(
    val id: String,
    val label: String,
    val labelUz: String,
    val badgeSymbol: String,
    val slides: List<StorySlide>
)

@Immutable
data class BrokerInfo(
    val id: String,
    val name: String,
    val badge: String,
    val badgeUz: String,
    val jurisdiction: String,
    val minDeposit: String,
    val commission: String,
    val usStocks: Boolean,
    val halalCashAccount: Boolean,
    val uzCardFunding: Boolean,
    val fundingMethods: String,
    val fundingMethodsUz: String,
    val halalNote: String,
    val halalNoteUz: String,
    val pros: List<String>,
    val prosUz: List<String>,
    val cons: List<String>,
    val consUz: List<String>,
    val rating: Double
)

@Immutable
data class StreamSession(
    val id: Int,
    val title: String,
    val titleUz: String,
    val speaker: String,
    val speakerRole: String,
    val speakerRoleUz: String,
    val dateLabel: String,
    val dateLabelUz: String,
    val durationMin: Int,
    val status: String, // LIVE | UPCOMING | RECORDED
    val isGoldClub: Boolean,
    val topicTag: String,
    val topicTagUz: String
)

@Immutable
data class AppNotificationItem(
    val id: String,
    val category: String, // ALERT | HALAL | PURIFICATION | DIGEST
    val title: String,
    val titleUz: String,
    val body: String,
    val bodyUz: String,
    val badge: String,
    val symbol: String? = null,
    val alertId: Int? = null,
    val isUrgent: Boolean = false
)

@Immutable
data class ZakatCalculationResult(
    val grossAssetsUsd: Double,
    val netZakatableUsd: Double,
    val nisabUsd: Double,
    val isAboveNisab: Boolean,
    val zakatDueUsd: Double,
    val zakatMonthlyProvisionUsd: Double
)

object ZakatCalculatorEngine {
    const val DEFAULT_NISAB_USD = 7200.0 // ~85g of gold
    const val ZAKAT_RATE = 0.025

    fun calculate(
        cashUsd: Double,
        halalStocksUsd: Double,
        goldAndCryptoUsd: Double,
        shortTermDebtUsd: Double,
        nisabUsd: Double = DEFAULT_NISAB_USD
    ): ZakatCalculationResult {
        val gross = (cashUsd.coerceAtLeast(0.0) +
            halalStocksUsd.coerceAtLeast(0.0) +
            goldAndCryptoUsd.coerceAtLeast(0.0))
        val net = (gross - shortTermDebtUsd.coerceAtLeast(0.0)).coerceAtLeast(0.0)
        val safeNisab = nisabUsd.coerceAtLeast(1.0)
        val above = net >= safeNisab
        val due = if (above) net * ZAKAT_RATE else 0.0
        return ZakatCalculationResult(
            grossAssetsUsd = gross,
            netZakatableUsd = net,
            nisabUsd = safeNisab,
            isAboveNisab = above,
            zakatDueUsd = due,
            zakatMonthlyProvisionUsd = due / 12.0
        )
    }
}

@Immutable
data class CalculatorProjection(
    val totalSeries: List<Float>,
    val investedSeries: List<Float>,
    val finalTotal: Double,
    val finalInvested: Double,
    val finalProfit: Double,
    val annualPassiveIncome: Double,
    val monthlyPassiveIncome: Double,
    val annualPurificationUsd: Double
)

object FinancialCalculatorEngine {
    /**
     * mode: 0 = Compound Growth, 1 = Dividend Income (reinvested yield + purification), 2 = Safe Withdrawal (FIRE 4% / yield)
     */
    fun compute(
        mode: Int,
        initialAmount: Double,
        monthlyTopUp: Double,
        annualYieldPct: Double,
        years: Int,
        periodsPerYear: Int,
        avgImpureRatioPct: Double = 1.2
    ): CalculatorProjection {
        val yInt = years.coerceIn(1, 40)
        val n = periodsPerYear.coerceAtLeast(1).toDouble()
        val rate = (annualYieldPct / 100.0).coerceAtLeast(0.0)

        val totals = mutableListOf<Float>()
        val invs = mutableListOf<Float>()

        when (mode) {
            1 -> {
                // Dividend Growth mode: annual dividend payout grows via capital appreciation (rate * 0.65) + dividend yield (rate * 0.35)
                val divYield = (annualYieldPct.coerceIn(1.0, 12.0) / 100.0)
                val priceGrowth = (rate * 0.6).coerceAtLeast(0.02)
                var portfolioVal = initialAmount
                var cumulativeDividendsNet = 0.0
                totals.add(portfolioVal.toFloat())
                invs.add(initialAmount.toFloat())
                for (y in 1..yInt) {
                    val yearlyContrib = monthlyTopUp * 12.0
                    portfolioVal = (portfolioVal + yearlyContrib) * (1.0 + priceGrowth)
                    val grossDiv = portfolioVal * divYield
                    val impureCut = grossDiv * (avgImpureRatioPct / 100.0)
                    val netDiv = (grossDiv - impureCut).coerceAtLeast(0.0)
                    cumulativeDividendsNet += netDiv
                    portfolioVal += netDiv
                    totals.add(portfolioVal.toFloat())
                    invs.add((initialAmount + yearlyContrib * y).toFloat())
                }
                val finalTotal = totals.last().toDouble()
                val finalInv = invs.last().toDouble()
                val finalGrossDiv = finalTotal * divYield
                val finalPurif = finalGrossDiv * (avgImpureRatioPct / 100.0)
                val finalNetAnnualDiv = (finalGrossDiv - finalPurif).coerceAtLeast(0.0)
                return CalculatorProjection(
                    totalSeries = totals,
                    investedSeries = invs,
                    finalTotal = finalTotal,
                    finalInvested = finalInv,
                    finalProfit = (finalTotal - finalInv).coerceAtLeast(0.0),
                    annualPassiveIncome = finalNetAnnualDiv,
                    monthlyPassiveIncome = finalNetAnnualDiv / 12.0,
                    annualPurificationUsd = finalPurif
                )
            }

            2 -> {
                // FIRE / Safe Withdrawal mode: initialAmount is starting capital, monthlyTopUp is desired monthly withdrawal,
                // annualYieldPct is portfolio return. Tracks capital sustainability over years.
                var balance = initialAmount.coerceAtLeast(1000.0)
                val annualDraw = monthlyTopUp * 12.0
                var cumulativeWithdrawn = 0.0
                totals.add(balance.toFloat())
                invs.add(0f)
                for (y in 1..yInt) {
                    val grown = balance * (1.0 + rate)
                    val actualDraw = annualDraw.coerceAtMost(grown)
                    balance = (grown - actualDraw).coerceAtLeast(0.0)
                    cumulativeWithdrawn += actualDraw
                    totals.add(balance.toFloat())
                    invs.add(cumulativeWithdrawn.toFloat())
                }
                val safeAnnual4Pct = balance * 0.04
                return CalculatorProjection(
                    totalSeries = totals,
                    investedSeries = invs,
                    finalTotal = balance,
                    finalInvested = cumulativeWithdrawn,
                    finalProfit = (balance + cumulativeWithdrawn - initialAmount).coerceAtLeast(0.0),
                    annualPassiveIncome = safeAnnual4Pct,
                    monthlyPassiveIncome = safeAnnual4Pct / 12.0,
                    annualPurificationUsd = 0.0
                )
            }

            else -> {
                for (y in 0..yInt) {
                    val inv = initialAmount + monthlyTopUp * 12.0 * y
                    invs.add(inv.toFloat())
                    if (y == 0) {
                        totals.add(initialAmount.toFloat())
                    } else {
                        val rPerPeriod = rate / n
                        val totalPeriods = n * y
                        val compoundPrincipal = initialAmount * Math.pow(1.0 + rPerPeriod, totalPeriods)
                        val contribPerPeriod = monthlyTopUp * (12.0 / n)
                        val compoundContrib = if (rPerPeriod > 0.00001) {
                            contribPerPeriod * ((Math.pow(1.0 + rPerPeriod, totalPeriods) - 1.0) / rPerPeriod)
                        } else {
                            contribPerPeriod * totalPeriods
                        }
                        totals.add((compoundPrincipal + compoundContrib).toFloat())
                    }
                }
                val finalTotal = totals.last().toDouble()
                val finalInv = invs.last().toDouble()
                val safeAnnual = finalTotal * 0.04
                return CalculatorProjection(
                    totalSeries = totals,
                    investedSeries = invs,
                    finalTotal = finalTotal,
                    finalInvested = finalInv,
                    finalProfit = (finalTotal - finalInv).coerceAtLeast(0.0),
                    annualPassiveIncome = safeAnnual,
                    monthlyPassiveIncome = safeAnnual / 12.0,
                    annualPurificationUsd = 0.0
                )
            }
        }
    }
}



