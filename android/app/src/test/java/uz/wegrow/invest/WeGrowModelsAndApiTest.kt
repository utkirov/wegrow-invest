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

    @Test
    fun parseLmsLessonsAndPaymentOrderJson_decodesQuizAndOrder() {
        val rawCourse = """
            {
              "purchased": true,
              "lessons": [
                {
                  "id": 1,
                  "course_id": 1,
                  "module_number": 1,
                  "lesson_number": 1,
                  "title": "Как устроен рынок США",
                  "title_uz": "AQSh bozori",
                  "lesson_type": "video",
                  "duration_min": 12,
                  "is_preview": true,
                  "locked": false,
                  "completed": true,
                  "quiz_score": 100
                }
              ]
            }
        """.trimIndent()

        val lms = WeGrowApiClient.parseCourseLessonsJson(rawCourse)
        assertNotNull(lms)
        assertTrue(lms!!.first)
        assertEquals(1, lms.second.size)
        assertEquals(100, lms.second.first().quizScore)

        val rawOrder = """
            {
              "order": {
                "order_uid": "WG-ABC123456789",
                "course_id": 1,
                "provider": "PAYME",
                "amount_uzs": 1290000,
                "status": "PENDING",
                "checkout_url": "https://checkout.paycom.uz/demo",
                "sandbox_enabled": true
              }
            }
        """.trimIndent()

        val order = WeGrowApiClient.parsePaymentOrderJson(rawOrder)
        assertNotNull(order)
        assertEquals("WG-ABC123456789", order!!.orderUid)
        assertEquals("PAYME", order.provider)
        assertEquals(1290000, order.amountUzs)
    }

    @Test
    fun zakatCalculatorEngine_checksNisabAndDebtDeduction() {
        val below = uz.wegrow.invest.data.ZakatCalculatorEngine.calculate(
            cashUsd = 2000.0,
            halalStocksUsd = 3000.0,
            goldAndCryptoUsd = 500.0,
            shortTermDebtUsd = 1000.0,
            nisabUsd = 7200.0
        )
        assertEquals(false, below.isAboveNisab)
        assertEquals(0.0, below.zakatDueUsd, 0.001)

        val above = uz.wegrow.invest.data.ZakatCalculatorEngine.calculate(
            cashUsd = 4000.0,
            halalStocksUsd = 8000.0,
            goldAndCryptoUsd = 2000.0,
            shortTermDebtUsd = 4000.0,
            nisabUsd = 7200.0
        )
        assertTrue(above.isAboveNisab)
        assertEquals(10000.0, above.netZakatableUsd, 0.001)
        assertEquals(250.0, above.zakatDueUsd, 0.001)
    }

    @Test
    fun financialCalculatorEngine_computesCompoundDividendAndFireModes() {
        val compound = uz.wegrow.invest.data.FinancialCalculatorEngine.compute(
            mode = 0, initialAmount = 1000.0, monthlyTopUp = 200.0, annualYieldPct = 8.0, years = 10, periodsPerYear = 12
        )
        assertTrue(compound.finalTotal > compound.finalInvested)
        assertEquals(11, compound.totalSeries.size)

        val dividend = uz.wegrow.invest.data.FinancialCalculatorEngine.compute(
            mode = 1, initialAmount = 10000.0, monthlyTopUp = 200.0, annualYieldPct = 4.0, years = 5, periodsPerYear = 4
        )
        assertTrue(dividend.annualPassiveIncome > 0.0)
        assertTrue(dividend.annualPurificationUsd > 0.0)

        val fire = uz.wegrow.invest.data.FinancialCalculatorEngine.compute(
            mode = 2, initialAmount = 100000.0, monthlyTopUp = 300.0, annualYieldPct = 8.0, years = 10, periodsPerYear = 12
        )
        assertTrue(fire.finalTotal > 100000.0)
        assertEquals(36000.0, fire.finalInvested, 1.0)
    }

    @Test
    fun parseV41ContentAndOrdersJson_decodesOrdersStoriesBrokersStreams() {
        val rawOrders = """
            {"orders":[{"order_uid":"WG-999","course_id":1,"course_title":"Курс США","course_title_uz":"AQSh kursi","provider":"CLICK","amount_uzs":1290000,"status":"PAID","checkout_url":"https://my.click.uz"}]}
        """.trimIndent()
        val orders = WeGrowApiClient.parseOrdersListJson(rawOrders)
        assertNotNull(orders)
        assertEquals(1, orders!!.size)
        assertEquals("Курс США", orders.first().courseTitle)
        assertEquals("PAID", orders.first().status)

        val rawBrokers = """
            {"brokers":[{"id":"ibkr","name":"IBKR","badge":"Top","badge_uz":"Top","jurisdiction":"US","min_deposit":"$0","commission":"$0.35","us_stocks":true,"halal_cash_account":true,"uz_card_funding":false,"funding_methods":"SWIFT","funding_methods_uz":"SWIFT","halal_note":"Cash","halal_note_uz":"Cash","pros":["SIPC"],"pros_uz":["SIPC"],"cons":["SWIFT"],"cons_uz":["SWIFT"],"rating":4.9}]}
        """.trimIndent()
        val brokers = WeGrowApiClient.parseBrokersJson(rawBrokers)
        assertNotNull(brokers)
        assertEquals("IBKR", brokers!!.first().name)
        assertTrue(brokers.first().usStocks)

        val rawStreams = """
            {"streams":[{"id":1,"title":"Эфир","title_uz":"Efir","speaker":"CFA","speaker_role":"Analyst","speaker_role_uz":"Tahlilchi","date_label":"LIVE","date_label_uz":"LIVE","duration_min":50,"status":"LIVE","is_gold_club":false,"topic_tag":"Halal","topic_tag_uz":"Halol"}]}
        """.trimIndent()
        val streams = WeGrowApiClient.parseStreamsJson(rawStreams)
        assertNotNull(streams)
        assertEquals("LIVE", streams!!.first().status)
    }
}
