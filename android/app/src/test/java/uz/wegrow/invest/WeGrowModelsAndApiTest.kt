package uz.wegrow.invest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.wegrow.invest.data.FinancialGoal
import uz.wegrow.invest.data.HalalStatus
import uz.wegrow.invest.data.UserProfile
import uz.wegrow.invest.data.WeGrowApiClient

class WeGrowModelsAndApiTest {

    @Test
    fun parseAuthLoginJson_extractsTokensAndUser() {
        val raw = """
            {
              "access_token": "jwt.access.token",
              "refresh_token": "jwt.refresh.token",
              "user": {
                "id": 7,
                "phone": "+998901112233",
                "first_name": "Сардор",
                "last_name": "Алиев",
                "language": "uz",
                "dark_theme": false,
                "is_guest": false
              }
            }
        """.trimIndent()

        val parsed = WeGrowApiClient.parseAuthLoginJson(raw)
        assertNotNull(parsed)
        assertEquals("jwt.access.token", parsed!!.session.accessToken)
        assertEquals("jwt.refresh.token", parsed.session.refreshToken)
        assertEquals("+998901112233", parsed.profile.phone)
        assertEquals("Сардор Алиев", parsed.profile.fullName)
        assertEquals("СА", parsed.profile.initials)
        assertTrue(parsed.session.isAuthenticated)
    }

    @Test
    fun parseChartJson_parsesTimeframePoints() {
        val raw = """
            {
              "symbol": "AAPL",
              "timeframe": "6M",
              "points": [195.2, 201.4, 214.8, 228.45],
              "current_price": 228.45,
              "period_change_abs": 33.25,
              "period_change_pct": 17.03,
              "data_source": "live"
            }
        """.trimIndent()

        val chart = WeGrowApiClient.parseChartJson(raw)
        assertNotNull(chart)
        assertEquals("AAPL", chart!!.symbol)
        assertEquals("6M", chart.timeframe)
        assertEquals(4, chart.points.size)
        assertEquals(228.45f, chart.points.last(), 0.01f)
        assertEquals("live", chart.dataSource)
    }

    @Test
    fun parseStocksSearchJson_handlesPaginationAndChartsMap() {
        val raw = """
            {
              "stocks": [
                {
                  "symbol": "NVDA",
                  "name": "NVIDIA Corp.",
                  "exchange": "NASDAQ",
                  "sector": "Технологии",
                  "sector_uz": "Texnologiyalar",
                  "price": 134.2,
                  "change_abs": 4.44,
                  "change_pct": 3.42,
                  "halal_status": "HALAL",
                  "chart_1m": [121.5, 134.2],
                  "charts": {
                    "1D": [132.0, 134.2],
                    "1Y": [88.0, 110.5, 134.2]
                  }
                }
              ],
              "pagination": { "page": 1, "page_size": 20, "total": 105, "total_pages": 6 },
              "sectors": ["Технологии", "Здравоохранение"]
            }
        """.trimIndent()

        val result = WeGrowApiClient.parseStocksSearchJson(raw)
        assertNotNull(result)
        assertEquals(105, result!!.total)
        assertEquals(6, result.totalPages)
        assertEquals(1, result.stocks.size)
        val nvda = result.stocks.first()
        assertEquals(HalalStatus.HALAL, nvda.halalStatus)
        assertEquals(3, nvda.chartForTimeframe("1Y").size)
        assertEquals(2, nvda.chartForTimeframe("1D").size)
    }

    @Test
    fun financialGoalAndProfileHelpers_computeProgressAndContact() {
        val goal = FinancialGoal(
            id = 1,
            title = "Подушка",
            titleUz = "Yostiqcha",
            targetAmount = 4000.0,
            currentAmount = 1000.0,
            monthlyContribution = 200.0,
            deadlineLabel = "2027",
            deadlineLabelUz = "2027",
            category = "safety",
            icon = "shield"
        )
        assertEquals(0.25f, goal.progressFraction, 0.001f)

        val guest = UserProfile(firstName = "Гость", lastName = "WeGrow", isGuest = true)
        assertEquals("Гостевой доступ", guest.contactLabel)
    }

    @Test
    fun parsePortfolioAndPurificationJson_decodesPositionsWarningsAndLedger() {
        val rawPortfolio = """
            {
              "summary": {
                "total_value": 5420.5,
                "total_invested": 4900.0,
                "total_pnl_abs": 520.5,
                "total_pnl_pct": 10.62,
                "daily_pnl_abs": 42.1,
                "realized_pnl_abs": 100.0,
                "halal_compliance_pct": 85.4,
                "positions_count": 2
              },
              "positions": [
                {
                  "symbol": "AAPL",
                  "name": "Apple Inc.",
                  "sector": "Технологии",
                  "sector_uz": "Texnologiyalar",
                  "halal_status": "HALAL",
                  "quantity": 15.0,
                  "avg_price": 210.0,
                  "current_price": 228.45,
                  "invested": 3150.0,
                  "market_value": 3426.75,
                  "pnl_abs": 276.75,
                  "pnl_pct": 8.79,
                  "daily_pnl": 34.65,
                  "weight_pct": 63.2
                }
              ],
              "non_halal_warnings": [
                {
                  "symbol": "JPM",
                  "name": "JPMorgan Chase",
                  "halal_status": "HARAM",
                  "market_value": 1194.5,
                  "reason": "Не соответствует 4 из 4 критериев AAOIFI"
                }
              ]
            }
        """.trimIndent()

        val p = WeGrowApiClient.parsePortfolioJson(rawPortfolio)
        assertNotNull(p)
        assertEquals(5420.5, p!!.summary.totalValue, 0.01)
        assertEquals(85.4, p.summary.halalCompliancePct, 0.01)
        assertEquals(1, p.positions.size)
        assertEquals(1, p.warnings.size)
        assertEquals(HalalStatus.HARAM, p.warnings.first().halalStatus)

        val rawPurification = """
            {
              "quarter_label": "Q4 2026",
              "total_pending_usd": 1.16,
              "total_cleared_usd": 0.25,
              "items": [
                {
                  "symbol": "MSFT",
                  "name": "Microsoft Corp.",
                  "quarter_label": "Q4 2026",
                  "shares": 100.0,
                  "dividend_total": 83.0,
                  "purification_due": 1.16,
                  "cleared_amount": 0.0,
                  "remaining_amount": 1.16,
                  "status": "PENDING"
                }
              ]
            }
        """.trimIndent()

        val ledger = WeGrowApiClient.parsePurificationJson(rawPurification)
        assertNotNull(ledger)
        assertEquals("Q4 2026", ledger!!.quarterLabel)
        assertEquals(1.16, ledger.totalPendingUsd, 0.001)
        assertEquals(1, ledger.items.size)
        assertEquals("MSFT", ledger.items.first().symbol)
    }
}
