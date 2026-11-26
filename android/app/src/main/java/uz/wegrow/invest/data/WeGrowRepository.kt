package uz.wegrow.invest.data

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Immutable
data class BootstrapPayload(
    val profile: UserProfile,
    val stocks: List<StockItem>,
    val goals: List<FinancialGoal>,
    val courses: List<CourseItem>,
    val news: List<NewsItem>,
    val indices: List<MarketIndex>,
    val heatmap: List<SectorHeatTile>,
    val marketOpenTextRu: String,
    val marketOpenTextUz: String,
    val online: Boolean
)

object WeGrowRepository {

    fun defaultFallbackData(): BootstrapPayload {
        val stocks = listOf(
            StockItem(
                symbol = "AAPL",
                name = "Apple Inc.",
                exchange = "NASDAQ",
                sector = "Технологии",
                sectorUz = "Texnologiyalar",
                price = 228.45,
                changeAbs = 2.31,
                changePct = 1.02,
                halalStatus = HalalStatus.HALAL,
                checkedDate = "08.11.2026",
                marketCap = "$3,45 трлн",
                peRatio = "34,8",
                dividendAnnual = "$1,00",
                dividendYield = "0,44%",
                purificationPerDividend = 0.002,
                dividendPerQuarter = 0.25,
                verdictSummary = "Соответствует 4 из 4 критериев",
                verdictSubtitle = "Методика WeGrow, пересмотр раз в квартал",
                reportPeriod = "Отчётность за III квартал 2026",
                revenueSegments = listOf(
                    RevenueSegment("Устройства", "Qurilmalar", 52.0f, "halal"),
                    RevenueSegment("Сервисы", "Servislar", 26.0f, "accent"),
                    RevenueSegment("Прочее", "Boshqa", 21.1f, "muted"),
                    RevenueSegment("Недопустимо", "Nomaqbul", 0.9f, "haram")
                ),
                criteria = listOf(
                    HalalCriterion("business", "Деятельность", "Faoliyat", "OK", "допустима", "maqbul", 100f, 100f, HalalStatus.HALAL),
                    HalalCriterion("debt", "Долг", "Qarz yuki", "9,8%", "лимит 30%", "limit 30%", 9.8f, 30f, HalalStatus.HALAL),
                    HalalCriterion("deposits", "Депозиты", "Foizli depozitlar", "4,6%", "лимит 30%", "limit 30%", 4.6f, 30f, HalalStatus.HALAL),
                    HalalCriterion("impure", "Недоп. доход", "Nomaqbul daromad", "0,9%", "лимит 5%", "limit 5%", 0.9f, 5f, HalalStatus.HALAL)
                ),
                chart1m = listOf(221.0f, 222.4f, 220.8f, 223.5f, 225.1f, 224.0f, 226.8f, 225.9f, 227.4f, 229.2f, 227.8f, 228.45f)
            ),
            StockItem(
                symbol = "NVDA",
                name = "NVIDIA Corp.",
                exchange = "NASDAQ",
                sector = "Технологии",
                sectorUz = "Texnologiyalar",
                price = 134.20,
                changeAbs = 4.44,
                changePct = 3.42,
                halalStatus = HalalStatus.HALAL,
                checkedDate = "10.11.2026",
                marketCap = "$3,29 трлн",
                peRatio = "52,4",
                dividendAnnual = "$0,04",
                dividendYield = "0,03%",
                purificationPerDividend = 0.0001,
                dividendPerQuarter = 0.01,
                verdictSummary = "Соответствует 4 из 4 критериев",
                verdictSubtitle = "Методика WeGrow, пересмотр раз в квартал",
                reportPeriod = "Отчётность за III квартал 2026",
                revenueSegments = listOf(
                    RevenueSegment("Дата-центры и ИИ", "Data-markaz va AI", 87.0f, "halal"),
                    RevenueSegment("Игровые GPU", "Gaming GPU", 9.5f, "accent"),
                    RevenueSegment("Авто и прочее", "Avto va boshqa", 3.2f, "muted"),
                    RevenueSegment("Недопустимо", "Nomaqbul", 0.3f, "haram")
                ),
                criteria = listOf(
                    HalalCriterion("business", "Деятельность", "Faoliyat", "OK", "допустима", "maqbul", 100f, 100f, HalalStatus.HALAL),
                    HalalCriterion("debt", "Долг", "Qarz yuki", "3,1%", "лимит 30%", "limit 30%", 3.1f, 30f, HalalStatus.HALAL),
                    HalalCriterion("deposits", "Депозиты", "Foizli depozitlar", "8,4%", "лимит 30%", "limit 30%", 8.4f, 30f, HalalStatus.HALAL),
                    HalalCriterion("impure", "Недоп. доход", "Nomaqbul daromad", "0,3%", "лимит 5%", "limit 5%", 0.3f, 5f, HalalStatus.HALAL)
                ),
                chart1m = listOf(121.5f, 123.0f, 122.2f, 125.4f, 127.8f, 126.9f, 129.5f, 131.0f, 130.2f, 132.6f, 133.1f, 134.20f)
            ),
            StockItem(
                symbol = "MSFT",
                name = "Microsoft Corp.",
                exchange = "NASDAQ",
                sector = "Технологии",
                sectorUz = "Texnologiyalar",
                price = 426.80,
                changeAbs = 7.62,
                changePct = 1.82,
                halalStatus = HalalStatus.HALAL,
                checkedDate = "09.11.2026",
                marketCap = "$3,17 трлн",
                peRatio = "35,6",
                dividendAnnual = "$3,32",
                dividendYield = "0,78%",
                purificationPerDividend = 0.011,
                dividendPerQuarter = 0.83,
                verdictSummary = "Соответствует 4 из 4 критериев",
                verdictSubtitle = "Методика WeGrow, пересмотр раз в квартал",
                reportPeriod = "Отчётность за III квартал 2026",
                revenueSegments = listOf(
                    RevenueSegment("Azure и Облако", "Azure va Bulut", 54.0f, "halal"),
                    RevenueSegment("Office и Бизнес", "Office va Biznes", 32.0f, "accent"),
                    RevenueSegment("Windows", "Windows", 12.6f, "muted"),
                    RevenueSegment("Недопустимо", "Nomaqbul", 1.4f, "haram")
                ),
                criteria = listOf(
                    HalalCriterion("business", "Деятельность", "Faoliyat", "OK", "допустима", "maqbul", 100f, 100f, HalalStatus.HALAL),
                    HalalCriterion("debt", "Долг", "Qarz yuki", "6,2%", "лимит 30%", "limit 30%", 6.2f, 30f, HalalStatus.HALAL),
                    HalalCriterion("deposits", "Депозиты", "Foizli depozitlar", "7,1%", "лимит 30%", "limit 30%", 7.1f, 30f, HalalStatus.HALAL),
                    HalalCriterion("impure", "Недоп. доход", "Nomaqbul daromad", "1,4%", "лимит 5%", "limit 5%", 1.4f, 5f, HalalStatus.HALAL)
                ),
                chart1m = listOf(412.0f, 414.5f, 413.2f, 417.0f, 419.5f, 418.1f, 421.0f, 422.4f, 420.9f, 424.0f, 425.2f, 426.80f)
            ),
            StockItem(
                symbol = "LLY",
                name = "Eli Lilly and Co.",
                exchange = "NYSE",
                sector = "Здравоохранение",
                sectorUz = "Sog'liqni saqlash",
                price = 842.10,
                changeAbs = 12.40,
                changePct = 1.49,
                halalStatus = HalalStatus.HALAL,
                checkedDate = "07.11.2026",
                marketCap = "$802 млрд",
                peRatio = "64,2",
                dividendAnnual = "$5,20",
                dividendYield = "0,62%",
                purificationPerDividend = 0.008,
                dividendPerQuarter = 1.30,
                verdictSummary = "Соответствует 4 из 4 критериев",
                verdictSubtitle = "Методика WeGrow, пересмотр раз в квартал",
                reportPeriod = "Отчётность за III квартал 2026",
                revenueSegments = listOf(
                    RevenueSegment("Эндокринология", "Endokrinologiya", 62.0f, "halal"),
                    RevenueSegment("Онкология", "Onkologiya", 21.0f, "accent"),
                    RevenueSegment("Иммунология", "Immunologiya", 16.4f, "muted"),
                    RevenueSegment("Недопустимо", "Nomaqbul", 0.6f, "haram")
                ),
                criteria = listOf(
                    HalalCriterion("business", "Деятельность", "Faoliyat", "OK", "допустима", "maqbul", 100f, 100f, HalalStatus.HALAL),
                    HalalCriterion("debt", "Долг", "Qarz yuki", "11,4%", "лимит 30%", "limit 30%", 11.4f, 30f, HalalStatus.HALAL),
                    HalalCriterion("deposits", "Депозиты", "Foizli depozitlar", "2,1%", "лимит 30%", "limit 30%", 2.1f, 30f, HalalStatus.HALAL),
                    HalalCriterion("impure", "Недоп. доход", "Nomaqbul daromad", "0,6%", "лимит 5%", "limit 5%", 0.6f, 5f, HalalStatus.HALAL)
                ),
                chart1m = listOf(810.0f, 815.0f, 812.5f, 820.0f, 825.4f, 823.0f, 829.0f, 834.2f, 831.0f, 837.5f, 839.0f, 842.10f)
            ),
            StockItem(
                symbol = "AMZN",
                name = "Amazon.com Inc.",
                exchange = "NASDAQ",
                sector = "Потребление",
                sectorUz = "Iste'mol",
                price = 210.35,
                changeAbs = 1.85,
                changePct = 0.89,
                halalStatus = HalalStatus.DOUBTFUL,
                checkedDate = "06.11.2026",
                marketCap = "$2,21 трлн",
                peRatio = "42,1",
                dividendAnnual = "$0,00",
                dividendYield = "0,00%",
                purificationPerDividend = 0.0,
                dividendPerQuarter = 0.0,
                verdictSummary = "Пограничная доля медиа и финтех-сервисов",
                verdictSubtitle = "Методика WeGrow, пересмотр раз в квартал",
                reportPeriod = "Отчётность за III квартал 2026",
                revenueSegments = listOf(
                    RevenueSegment("AWS и Облако", "AWS va Bulut", 48.0f, "halal"),
                    RevenueSegment("Ритейл", "Riteyl", 41.0f, "accent"),
                    RevenueSegment("Медиа", "Media", 6.8f, "muted"),
                    RevenueSegment("Недопустимо", "Nomaqbul", 4.2f, "haram")
                ),
                criteria = listOf(
                    HalalCriterion("business", "Деятельность", "Faoliyat", "Спорно", "Prime Video", "Prime Video", 75f, 100f, HalalStatus.DOUBTFUL),
                    HalalCriterion("debt", "Долг", "Qarz yuki", "14,2%", "лимит 30%", "limit 30%", 14.2f, 30f, HalalStatus.HALAL),
                    HalalCriterion("deposits", "Депозиты", "Foizli depozitlar", "8,9%", "лимит 30%", "limit 30%", 8.9f, 30f, HalalStatus.HALAL),
                    HalalCriterion("impure", "Недоп. доход", "Nomaqbul daromad", "4,2%", "лимит 5%", "limit 5%", 4.2f, 5f, HalalStatus.DOUBTFUL)
                ),
                chart1m = listOf(198.0f, 200.5f, 199.2f, 202.0f, 204.5f, 203.1f, 206.0f, 207.4f, 205.9f, 208.0f, 209.2f, 210.35f)
            ),
            StockItem(
                symbol = "JPM",
                name = "JPMorgan Chase & Co.",
                exchange = "NYSE",
                sector = "Финансы",
                sectorUz = "Moliya",
                price = 238.90,
                changeAbs = -1.45,
                changePct = -0.60,
                halalStatus = HalalStatus.HARAM,
                checkedDate = "05.11.2026",
                marketCap = "$675 млрд",
                peRatio = "13,2",
                dividendAnnual = "$5,00",
                dividendYield = "2,09%",
                purificationPerDividend = 0.0,
                dividendPerQuarter = 1.25,
                verdictSummary = "Не соответствует 3 из 4 критериев (процентный банк)",
                verdictSubtitle = "Методика WeGrow, пересмотр раз в квартал",
                reportPeriod = "Отчётность за III квартал 2026",
                revenueSegments = listOf(
                    RevenueSegment("Комиссии", "Komissiyalar", 12.0f, "halal"),
                    RevenueSegment("Процентный доход", "Foizli daromad", 88.0f, "haram")
                ),
                criteria = listOf(
                    HalalCriterion("business", "Деятельность", "Faoliyat", "Харам", "традиционный банк", "an'anaviy bank", 100f, 100f, HalalStatus.HARAM),
                    HalalCriterion("debt", "Долг", "Qarz yuki", "68,0%", "лимит 30%", "limit 30%", 68.0f, 30f, HalalStatus.HARAM),
                    HalalCriterion("deposits", "Депозиты", "Foizli depozitlar", "74,5%", "лимит 30%", "limit 30%", 74.5f, 30f, HalalStatus.HARAM),
                    HalalCriterion("impure", "Недоп. доход", "Nomaqbul daromad", "88,0%", "лимит 5%", "limit 5%", 88.0f, 5f, HalalStatus.HARAM)
                ),
                chart1m = listOf(242.0f, 243.5f, 241.2f, 240.0f, 241.5f, 239.8f, 240.2f, 239.4f, 240.1f, 239.5f, 240.35f, 238.90f)
            )
        )

        val goals = listOf(
            FinancialGoal(1, "Финансовая подушка", "Moliyaviy yostiqcha", 3000.0, 2100.0, 250.0, "Март 2027 · осталось 4 мес", "Mart 2027 · 4 oy qoldi", "safety", "shield-check"),
            FinancialGoal(2, "Умра для родителей", "Ota-ona uchun Umra", 6000.0, 1950.0, 300.0, "Рамадан 2027 · по плану", "Ramazon 2027 · reja bo'yicha", "family", "plane"),
            FinancialGoal(3, "Новый ноутбук", "Yangi noutbuk", 3000.0, 800.0, 150.0, "Август 2027 · +$100 в ноябре", "Avgust 2027 · noyabrda +$100", "tech", "laptop")
        )

        val courses = listOf(
            CourseItem(
                id = 1,
                title = "Основы инвестирования в акции США",
                titleUz = "AQSh aksiyalariga investitsiya asoslari",
                level = "Начальный уровень",
                levelUz = "Boshlang'ich daraja",
                rating = 4.9,
                reviewsCount = 1240,
                author = "Команда аналитиков WeGrow",
                authorUz = "WeGrow tahlilchilar jamoasi",
                authorRole = "Авторы курса",
                authorRoleUz = "Kurs mualliflari",
                previewDuration = "2 мин 14 сек",
                modulesCount = 8,
                lessonsCount = 42,
                accessDuration = "6 месяцев",
                accessDurationUz = "6 oy",
                priceUzs = "1 290 000 сум",
                purchased = false,
                modules = listOf(
                    CourseModule(1, "Как устроен фондовый рынок США", "AQSh fond bozori qanday ishlaydi", "5 уроков · 45 мин · Бесплатно", "5 dars · 45 daq · Bepul", false),
                    CourseModule(2, "Критерии халяльности акций (AAOIFI и WeGrow)", "Aksiyalar halollik mezonlari (AAOIFI va WeGrow)", "6 уроков · 1 ч 10 мин", "6 dars · 1 s 10 daq", true),
                    CourseModule(3, "Финансовая отчётность и мультипликаторы P/E, ROE", "Moliyaviy hisobot va P/E, ROE ko'rsatkichlari", "6 уроков · 1 ч 20 мин", "6 dars · 1 s 20 daq", true),
                    CourseModule(4, "Очистка дивидендов (тазкия) и закят с инвестиций", "Dividendlarni poklash va investitsiya zakoti", "5 уроков · 50 мин", "5 dars · 50 daq", true),
                    CourseModule(5, "Выбор брокера в Узбекистане и пополнение счёта", "O'zbekistonda broker tanlash va hisobni to'ldirish", "4 урока · 40 мин", "4 dars · 40 daq", true),
                    CourseModule(6, "Сборка долгосрочного халяль-портфеля", "Uzoq muddatli halol portfel yig'ish", "6 уроков · 1 ч 15 мин", "6 dars · 1 s 15 daq", true),
                    CourseModule(7, "Управление рисками и психология инвестора", "Xatarlarni boshqarish va investor psixologiyasi", "5 уроков · 55 мин", "5 dars · 55 daq", true),
                    CourseModule(8, "Итоговый проект и сертификация WeGrow", "Yakuniy loyiha va WeGrow sertifikati", "5 уроков · Экзамен", "5 dars · Imtihon", true)
                )
            )
        )

        val news = listOf(
            NewsItem(
                id = 1,
                title = "NVIDIA обновила рекорд выручки в сегменте ИИ-чипов за III квартал",
                titleUz = "NVIDIA III chorakda AI-chiplar segmentida rekord daromad ko'rsatdi",
                source = "Рынок США · 25 мин назад",
                sourceUz = "AQSh bozori · 25 daqiqa oldin",
                ticker = "NVDA",
                tag = "Халяль",
                summary = "Выручка дата-центров выросла на 87% при долговой нагрузке всего 3,1% — бумага уверенно держит статус Халяль по AAOIFI.",
                summaryUz = "Data-markaz tushumi 87% ga oshdi, qarz yuki atigi 3,1% — aksiya AAOIFI bo'yicha Halol maqomini saqladi.",
                body = "## Ключевые итоги квартала\nNVIDIA отчиталась о рекордном спросе на ускорители Blackwell со стороны облачных провайдеров. Сегмент дата-центров сформировал **87% всей выручки**.\n\n## Проверка AAOIFI\n- **Долговая нагрузка:** 3,1% (лимит 30%).\n- **Процентные депозиты:** 8,4% (лимит 30%).\n- **Недопустимый доход:** 0,3% (лимит 5%).",
                bodyUz = "## Chorakning asosiy natijalari\nNVIDIA bulut provayderlaridan Blackwell tezlatkichlariga rekord talab haqida hisobot berdi. Data-markazlar segmenti **tushumning 87% ini** tashkil etdi.\n\n## AAOIFI tekshiruvi\n- **Qarz yuki:** 3,1% (limit 30%).\n- **Foizli depozitlar:** 8,4% (limit 30%).\n- **Nomaqbul daromad:** 0,3% (limit 5%).",
                readMin = 3,
                publishedAt = "26.11.2026"
            ),
            NewsItem(
                id = 2,
                title = "Итоги квартального пересмотра 105 компаний S&P 500 по методике AAOIFI",
                titleUz = "AAOIFI metodikasi bo'yicha S&P 500 105 kompaniyasi choraklik sharhi",
                source = "Аналитика WeGrow · 2 ч назад",
                sourceUz = "WeGrow tahlili · 2 soat oldin",
                ticker = "AAPL",
                tag = "Методика",
                summary = "82 из 105 крупнейших акций США прошли все 4 критерия AAOIFI; 3 компании находятся в пограничном статусе «Сомнительно».",
                summaryUz = "AQShning 105 yirik aksiyasidan 82 tasi barcha 4 AAOIFI mezonidan o'tdi; 3 tasi «Shubhali» maqomida.",
                body = "## Результаты аудита WeGrow\nКоманда шариатских аналитиков обновила коэффициенты 105 бумаг:\n- **82 акции — Халяль**\n- **3 акции — Сомнительно** (AMZN, WMT, SPOT)\n- **20 акций — Не халяль**\n\n> Проверьте вкладку **Портфель**, чтобы увидеть статус ваших позиций и расчёт очищения дивидендов.",
                bodyUz = "## WeGrow auditi natijalari\nShariat tahlilchilari 105 ta aksiyaning ko'rsatkichlarini yangiladi:\n- **82 ta aksiya — Halol**\n- **3 ta aksiya — Shubhali** (AMZN, WMT, SPOT)\n- **20 ta aksiya — Halol emas**\n\n> Pozitsiyalaringiz maqomini ko'rish uchun **Portfel** bo'limini tekshiring.",
                readMin = 4,
                publishedAt = "26.11.2026"
            ),
            NewsItem(
                id = 3,
                title = "Apple расширяет линейку чипов M4 и сохраняет долговую нагрузку ниже 10%",
                titleUz = "Apple M4 chiplarini kengaytirmoqda va qarz yukini 10% dan past saqlamoqda",
                source = "Технологии · 4 ч назад",
                sourceUz = "Texnologiyalar · 4 soat oldin",
                ticker = "AAPL",
                tag = "Отчётность",
                summary = "Сервисы приносят 26% выручки Apple, а долговая нагрузка остаётся на комфортном уровне 9,8%.",
                summaryUz = "Servislar Apple tushumining 26% ini keltirmoqda, qarz yuki esa 9,8% darajasida.",
                body = "## Драйверы роста\nОбновление линейки Mac на чипах M4 и стабильный рост App Store / iCloud поддерживают высокую маржинальность.\n\n## Очищение дивидендов\nПри доле недопустимого дохода **0,9%** сумма очищения составляет ~$0,002 на акцию за квартал.",
                bodyUz = "## O'sish omillari\nM4 chipli Mac liniyasi va App Store / iCloud o'sishi yuqori rentabellikni saqlamoqda.\n\n## Dividend poklash\n**0,9%** nomaqbul daromad ulushi bilan har chorakda aksiya boshiga ~$0,002 xayriyaga ajratiladi.",
                readMin = 3,
                publishedAt = "25.11.2026"
            ),
            NewsItem(
                id = 4,
                title = "Как инвестору из Узбекистана оформить форму W-8BEN и выбрать халяль-счёт",
                titleUz = "O'zbekistonlik investor W-8BEN shaklini qanday to'ldiradi va halol hisob tanlaydi",
                source = "Гид WeGrow · 1 день назад",
                sourceUz = "WeGrow qo'llanmasi · 1 kun oldin",
                ticker = "MSFT",
                tag = "Рынок",
                summary = "Пошаговый чек-лист по отключению маржинального плеча, подписанию W-8BEN и учёту дивидендов.",
                summaryUz = "Marjinal yelkani o'chirish, W-8BEN imzolash va dividendlarni hisobga olish bo'yicha chek-list.",
                body = "## Чек-лист инвестора\n1. Используйте только **Cash-счёт** без маржинального плеча.\n2. Подпишите **W-8BEN** в личном кабинете брокера.\n3. Регулярно проводите очищение дивидендов в разделе **Портфель**.",
                bodyUz = "## Investor chek-listi\n1. Faqat marjinal yelkasiz **Cash-hisob** dan foydalaning.\n2. Broker kabinetida **W-8BEN** shaklini imzolang.\n3. **Portfel** bo'limida dividendlarni muntazam poklab boring.",
                readMin = 5,
                publishedAt = "24.11.2026"
            )
        )

        val indices = listOf(
            MarketIndex("S&P 500", "5 912,4", "+0,84%", true),
            MarketIndex("NASDAQ", "19 120,8", "+1,42%", true),
            MarketIndex("Dow Jones", "43 870,4", "-0,18%", false)
        )

        val heatmap = listOf(
            SectorHeatTile("Технологии", "Texnologiyalar", "+2,14%", "UP_3", 2.2f),
            SectorHeatTile("Коммуникации", "Kommunikatsiya", "+1,12%", "UP_2", 1.6f),
            SectorHeatTile("Здравоохранение", "Sog'liqni saqlash", "+0,95%", "UP_2", 1.4f),
            SectorHeatTile("Промышленность", "Sanoat", "+0,34%", "UP_1", 1.1f),
            SectorHeatTile("Потребление", "Iste'mol", "+0,41%", "UP_1", 1.3f),
            SectorHeatTile("Сырьё", "Xomashyo", "-0,28%", "DOWN_1", 0.9f),
            SectorHeatTile("Энергетика", "Energetika", "-0,86%", "DOWN_2", 1.0f),
            SectorHeatTile("Недвижимость", "Ko'chmas mulk", "+0,19%", "UP_1", 1.0f)
        )

        return BootstrapPayload(
            profile = UserProfile(),
            stocks = stocks,
            goals = goals,
            courses = courses,
            news = news,
            indices = indices,
            heatmap = heatmap,
            marketOpenTextRu = "Биржи США открыты, до закрытия 2 ч 14 мин",
            marketOpenTextUz = "AQSh birjalari ochiq, yopilishiga 2 s 14 daq",
            online = false
        )
    }

    fun defaultStories(): List<StoryDeck> = listOf(
        StoryDeck(
            id = "story_halal_audit",
            label = "Халяль",
            labelUz = "Halol",
            badgeSymbol = "✦",
            slides = listOf(
                StorySlide(
                    eyebrow = "Аудит Q3 2026",
                    eyebrowUz = "Q3 2026 auditi",
                    title = "82 из 105 акций США признаны Халяль",
                    titleUz = "AQShning 105 aksiyasidan 82 tasi Halol",
                    body = "Обновлённый скрининг AAOIFI №21: отрасль, долговая нагрузка < 30%, процентные депозиты < 30% и недопустимый доход < 5%.",
                    bodyUz = "Yangilangan AAOIFI №21 skriningi: soha, qarz yuki < 30%, foizli depozitlar < 30% va nomaqbul tushum < 5%.",
                    ctaLabel = "Открыть методику AAOIFI",
                    ctaLabelUz = "AAOIFI metodikasini ochish",
                    actionRoute = "METHODOLOGY"
                ),
                StorySlide(
                    eyebrow = "Тазкия дивидендов",
                    eyebrowUz = "Dividend poklash",
                    title = "Авторасчёт очищения в Портфеле",
                    titleUz = "Portfelda avtomatik poklash hisobi",
                    body = "Добавьте купленные акции в Портфель — WeGrow рассчитает точную сумму очищения на благотворительность.",
                    bodyUz = "Sotib olingan aksiyalarni Portfelga qo'shing — WeGrow xayriyaga ajratiladigan aniq summani hisoblaydi.",
                    ctaLabel = "Открыть Портфель",
                    ctaLabelUz = "Portfelni ochish",
                    actionRoute = "PORTFOLIO"
                )
            )
        ),
        StoryDeck(
            id = "story_news",
            label = "Новости",
            labelUz = "Yangiliklar",
            badgeSymbol = "📰",
            slides = listOf(
                StorySlide(
                    eyebrow = "Дайджест рынка",
                    eyebrowUz = "Bozor dayjesti",
                    title = "NVIDIA, Apple и Eli Lilly обновляют максимумы",
                    titleUz = "NVIDIA, Apple va Eli Lilly rekord o'rnatmoqda",
                    body = "Читайте свежие разборы квартальных отчётов и шариатские комментарии аналитиков WeGrow.",
                    bodyUz = "Choraklik hisobotlar tahlili va WeGrow shariat sharhlarini o'qing.",
                    ctaLabel = "Перейти к новостям",
                    ctaLabelUz = "Yangiliklarga o'tish",
                    actionRoute = "NEWS_FEED"
                )
            )
        ),
        StoryDeck(
            id = "story_courses",
            label = "Курсы",
            labelUz = "Kurslar",
            badgeSymbol = "🎓",
            slides = listOf(
                StorySlide(
                    eyebrow = "WeGrow Academy",
                    eyebrowUz = "WeGrow Academy",
                    title = "8 модулей практики + именной сертификат",
                    titleUz = "8 amaliy modul + shaxsiy sertifikat",
                    body = "От выбора брокера в Узбекистане до сборки халяль-портфеля, квизов и расчёта Закята.",
                    bodyUz = "O'zbekistonda broker tanlashdan halol portfel yig'ish, testlar va Zakot hisoblashgacha.",
                    ctaLabel = "Открыть обучение",
                    ctaLabelUz = "Kursni ochish",
                    actionRoute = "COURSES"
                )
            )
        ),
        StoryDeck(
            id = "story_gold_club",
            label = "Gold Club",
            labelUz = "Gold Club",
            badgeSymbol = "👑",
            slides = listOf(
                StorySlide(
                    eyebrow = "Премиум-клуб",
                    eyebrowUz = "Premium klub",
                    title = "WeGrow Gold Club для системных инвесторов",
                    titleUz = "Tizimli investorlar uchun WeGrow Gold Club",
                    body = "Еженедельные инвест-идеи, ранний доступ к аудиту AAOIFI и закрытые разборы с аналитиками.",
                    bodyUz = "Haftalik invest-g'oyalar, AAOIFI auditiga erta kirish va tahlilchilar bilan yopiq tahlillar.",
                    ctaLabel = "Подробнее о Gold Club",
                    ctaLabelUz = "Gold Club haqida",
                    actionRoute = "GOLD_CLUB"
                )
            )
        ),
        StoryDeck(
            id = "story_streams",
            label = "Эфиры",
            labelUz = "Efirlar",
            badgeSymbol = "▶",
            slides = listOf(
                StorySlide(
                    eyebrow = "Прямые эфиры",
                    eyebrowUz = "Jonli efirlar",
                    title = "Разбор халяль-портфеля на $10 000 в прямом эфире",
                    titleUz = "Jonli efirda $10 000 lik halol portfel tahlili",
                    body = "Присоединяйтесь к прямым вебинарам аналитиков или смотрите записи в удобное время.",
                    bodyUz = "Tahlilchilarning jonli vebinarlariga qo'shiling yoki yozuvlarni qulay vaqtda ko'ring.",
                    ctaLabel = "Расписание эфиров",
                    ctaLabelUz = "Efirlar jadvali",
                    actionRoute = "STREAMS"
                )
            )
        )
    )

    fun defaultBrokers(): List<BrokerInfo> = listOf(
        BrokerInfo(
            id = "ibkr",
            name = "Interactive Brokers (IBKR)",
            badge = "Выбор для долгосрока",
            badgeUz = "Uzoq muddat uchun tanlov",
            jurisdiction = "США · SEC / FINRA / SIPC ($500K)",
            minDeposit = "$0",
            commission = "от $0,35 (Tiered)",
            usStocks = true,
            halalCashAccount = true,
            uzCardFunding = false,
            fundingMethods = "SWIFT из банков УЗ (Kapitalbank, Ipak Yo'li, TBC)",
            fundingMethodsUz = "O'zbekiston banklaridan SWIFT (Kapitalbank, Ipak Yo'li, TBC)",
            halalNote = "Выберите тип счёта «Cash» (без плеча) и отключите программу кредитования акций.",
            halalNoteUz = "«Cash» hisob turini tanlang (yelkasiz) va aksiyalarni qarzga berishni o'chiring.",
            pros = listOf("Защита SIPC до $500 000", "Минимальные комиссии на NASDAQ/NYSE", "Дробные акции S&P 500"),
            prosUz = listOf("$500 000 gacha SIPC himoyasi", "NASDAQ/NYSE minimal komissiya", "Kasr aksiyalar"),
            cons = listOf("Пополнение через SWIFT", "Сложный интерфейс для новичка"),
            consUz = listOf("SWIFT orqali to'ldirish", "Yangi boshlovchilar uchun murakkab interfeys"),
            rating = 4.9
        ),
        BrokerInfo(
            id = "freedom",
            name = "Freedom Finance Global",
            badge = "Удобный старт из УЗ",
            badgeUz = "O'zbekistondan qulay start",
            jurisdiction = "МФЦА (Астана) · AFSA",
            minDeposit = "$0",
            commission = "$0,02 / акция + $2",
            usStocks = true,
            halalCashAccount = true,
            uzCardFunding = true,
            fundingMethods = "Visa/Mastercard банков УЗ, SWIFT-перевод",
            fundingMethodsUz = "O'zbekiston banklari Visa/Mastercard, SWIFT",
            halalNote = "Отключите маржинальную торговлю и D-счёт (своп-проценты) в настройках.",
            halalNoteUz = "Sozlamalarda marjinal savdo va D-hisob foizlarini o'chiring.",
            pros = listOf("Открытие по паспорту УЗ", "Поддержка RU/UZ", "Пополнение картой Visa/Mastercard"),
            prosUz = listOf("O'zbekiston pasporti bilan ochish", "RU/UZ qo'llab-quvvatlash", "Visa/Mastercard to'ldirish"),
            cons = listOf("Комиссия выше IBKR на малых чеках", "Нужно отключить D-счёт вручную"),
            consUz = listOf("Kichik summalarda komissiya yuqoriroq", "D-hisobni qo'lda o'chirish kerak"),
            rating = 4.7
        ),
        BrokerInfo(
            id = "jett",
            name = "Jett / Инвестиции в УЗ",
            badge = "Локальный рынок УЗ",
            badgeUz = "O'zbekiston lokal bozori",
            jurisdiction = "Узбекистан · НАПП (РФБ «Тошкент»)",
            minDeposit = "100 000 сум",
            commission = "от 0,5% за сделку",
            usStocks = false,
            halalCashAccount = true,
            uzCardFunding = true,
            fundingMethods = "Uzcard / Humo через приложения банков",
            fundingMethodsUz = "Bank ilovalari orqali Uzcard / Humo",
            halalNote = "Для локальных бумаг Узбекистана проверяйте деятельность эмитента на отсутствие риба.",
            halalNoteUz = "Mahalliy qog'ozlar uchun emitent faoliyatini ribodan xoliligini tekshiring.",
            pros = listOf("Пополнение в сумах (Uzcard/Humo)", "Юрисдикция Узбекистана", "Верификация через MyID"),
            prosUz = listOf("So'mda to'ldirish (Uzcard/Humo)", "O'zbekiston yurisdiktsiyasi", "MyID tasdiqlash"),
            cons = listOf("Нет акций США (NASDAQ/NYSE)", "Ограниченная ликвидность"),
            consUz = listOf("AQSh aksiyalari yo'q", "Cheklangan likvidlik"),
            rating = 4.5
        ),
        BrokerInfo(
            id = "tabadul_global",
            name = "Исламский брокер (Шарья-счёт)",
            badge = "100% без риба",
            badgeUz = "100% ribosiz",
            jurisdiction = "Международные партнёры · AAOIFI",
            minDeposit = "$100",
            commission = "0,25% (мин. $1)",
            usStocks = true,
            halalCashAccount = true,
            uzCardFunding = true,
            fundingMethods = "Международные карты Visa/Mastercard, SWIFT",
            fundingMethodsUz = "Xalqaro Visa/Mastercard, SWIFT",
            halalNote = "Нативный Cash-счёт без овернайт-свопов, кредитного плеча и автокредитования акций.",
            halalNoteUz = "Overnayt-svop, kredit yelkasi va aksiyalarni qarzga berishsiz tabiiy Cash-hisob.",
            pros = listOf("Нет скрытых процентов", "Встроенный халяль-фильтр", "Удобный отчёт по Закяту"),
            prosUz = listOf("Yashirin foizlar yo'q", "O'rnatilgan halol filtr", "Zakot uchun qulay hisobot"),
            cons = listOf("Меньше тикеров, чем в IBKR", "Комиссия за ввод картой 1–2%"),
            consUz = listOf("IBKR dan kamroq tiker", "Karta komissiyasi 1–2%"),
            rating = 4.8
        )
    )

    fun defaultStreams(): List<StreamSession> = listOf(
        StreamSession(
            id = 1,
            title = "Итоги Q3 2026: Топ-10 халяльных акций США перед новым годом",
            titleUz = "Q3 2026 yakunlari: Yangi yil oldidan AQShning Top-10 halol aksiyalari",
            speaker = "Тимур Рахимов, CFA",
            speakerRole = "Ведущий аналитик WeGrow",
            speakerRoleUz = "WeGrow yetakchi tahlilchisi",
            dateLabel = "Сейчас в эфире · LIVE",
            dateLabelUz = "Hozir efirda · LIVE",
            durationMin = 55,
            status = "LIVE",
            isGoldClub = false,
            topicTag = "Халяль-аудит",
            topicTagUz = "Halol-audit"
        ),
        StreamSession(
            id = 2,
            title = "Как собрать портфель на $10 000 и рассчитать Закят с акций",
            titleUz = "$10 000 lik portfel yig'ish va aksiyalardan Zakot hisoblash",
            speaker = "Сардор Усманов",
            speakerRole = "Шариатский консультант AAOIFI",
            speakerRoleUz = "AAOIFI shariat maslahatchisi",
            dateLabel = "29 ноября · 20:00 (Ташкент)",
            dateLabelUz = "29-noyabr · 20:00 (Toshkent)",
            durationMin = 60,
            status = "UPCOMING",
            isGoldClub = false,
            topicTag = "Закят и Тазкия",
            topicTagUz = "Zakot va Tazkiya"
        ),
        StreamSession(
            id = 3,
            title = "Закрытый разбор полупроводников: NVDA, AVGO, AMD и TSMC",
            titleUz = "Yarimo'tkazgichlar yopiq tahlili: NVDA, AVGO, AMD va TSMC",
            speaker = "Команда WeGrow Research",
            speakerRole = "Gold Club Analyst Desk",
            speakerRoleUz = "Gold Club tahlil markazi",
            dateLabel = "3 декабря · 20:30 (Ташкент)",
            dateLabelUz = "3-dekabr · 20:30 (Toshkent)",
            durationMin = 75,
            status = "UPCOMING",
            isGoldClub = true,
            topicTag = "Gold Club",
            topicTagUz = "Gold Club"
        ),
        StreamSession(
            id = 4,
            title = "Запись: Пополнение IBKR из Узбекистана и заполнение W-8BEN",
            titleUz = "Yozuv: O'zbekistondan IBKR ni to'ldirish va W-8BEN to'ldirish",
            speaker = "Азиза Каримова",
            speakerRole = "Эксперт по международным брокерам",
            speakerRoleUz = "Xalqaro brokerlar eksperti",
            dateLabel = "Запись · 18 ноября",
            dateLabelUz = "Yozuv · 18-noyabr",
            durationMin = 42,
            status = "RECORDED",
            isGoldClub = false,
            topicTag = "Брокеры",
            topicTagUz = "Brokerlar"
        )
    )

    suspend fun fetchBootstrap(baseUrl: String): BootstrapPayload = withContext(Dispatchers.IO) {
        try {
            val cleanBase = baseUrl.trim().trimEnd('/')
            val url = URL("$cleanBase/api/bootstrap")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                parseBootstrapJson(JSONObject(body))
            } else {
                defaultFallbackData()
            }
        } catch (_: Exception) {
            defaultFallbackData()
        }
    }

    suspend fun saveProfileRemote(baseUrl: String, profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanBase = baseUrl.trim().trimEnd('/')
            val payload = JSONObject().apply {
                put("first_name", profile.firstName)
                put("last_name", profile.lastName)
                put("middle_name", profile.middleName)
                put("birth_date", profile.birthDate)
                put("gender", profile.gender)
                put("language", profile.language)
                put("dark_theme", profile.darkTheme)
            }
            postJson("$cleanBase/api/profile", payload) != null
        } catch (_: Exception) {
            false
        }
    }

    suspend fun createGoalRemote(baseUrl: String, title: String, targetAmount: Double, monthly: Double): FinancialGoal? = withContext(Dispatchers.IO) {
        try {
            val cleanBase = baseUrl.trim().trimEnd('/')
            val payload = JSONObject().apply {
                put("title", title)
                put("target_amount", targetAmount)
                put("current_amount", 0.0)
                put("monthly_contribution", monthly)
                put("deadline_label", "Цель из калькулятора")
                put("category", "savings")
                put("icon", "target")
            }
            val resp = postJson("$cleanBase/api/goals", payload) ?: return@withContext null
            val g = resp.getJSONObject("goal")
            parseGoal(g)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun contributeGoalRemote(baseUrl: String, goalId: Int, amount: Double): FinancialGoal? = withContext(Dispatchers.IO) {
        try {
            val cleanBase = baseUrl.trim().trimEnd('/')
            val payload = JSONObject().apply {
                put("amount", amount)
            }
            val resp = postJson("$cleanBase/api/goals/$goalId/contribute", payload) ?: return@withContext null
            parseGoal(resp.getJSONObject("goal"))
        } catch (_: Exception) {
            null
        }
    }

    suspend fun purchaseCourseRemote(baseUrl: String, courseId: Int, provider: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanBase = baseUrl.trim().trimEnd('/')
            val payload = JSONObject().apply {
                put("provider", provider)
            }
            postJson("$cleanBase/api/courses/$courseId/purchase", payload) != null
        } catch (_: Exception) {
            false
        }
    }

    private fun postJson(urlStr: String, json: JSONObject): JSONObject? {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 6000
            readTimeout = 6000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
            it.write(json.toString())
            it.flush()
        }
        return if (conn.responseCode in 200..299) {
            val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            JSONObject(body)
        } else {
            null
        }
    }

    private fun parseBootstrapJson(root: JSONObject): BootstrapPayload {
        val fallback = defaultFallbackData()
        val pObj = root.optJSONObject("profile")
        val profile = if (pObj != null) {
            UserProfile(
                firstName = pObj.optString("first_name", "Азиз"),
                lastName = pObj.optString("last_name", "Каримов"),
                middleName = pObj.optString("middle_name", "Рустамович"),
                birthDate = pObj.optString("birth_date", "15.04.1996"),
                gender = pObj.optString("gender", "male"),
                language = pObj.optString("language", "ru"),
                darkTheme = pObj.optBoolean("dark_theme", true)
            )
        } else fallback.profile

        val stocksArr = root.optJSONArray("stocks")
        val stocks = if (stocksArr != null && stocksArr.length() > 0) {
            (0 until stocksArr.length()).map { i -> parseStock(stocksArr.getJSONObject(i)) }
        } else fallback.stocks

        val goalsArr = root.optJSONArray("goals")
        val goals = if (goalsArr != null && goalsArr.length() > 0) {
            (0 until goalsArr.length()).map { i -> parseGoal(goalsArr.getJSONObject(i)) }
        } else fallback.goals

        val coursesArr = root.optJSONArray("courses")
        val courses = if (coursesArr != null && coursesArr.length() > 0) {
            (0 until coursesArr.length()).map { i -> parseCourse(coursesArr.getJSONObject(i)) }
        } else fallback.courses

        val newsArr = root.optJSONArray("news")
        val news = if (newsArr != null && newsArr.length() > 0) {
            (0 until newsArr.length()).map { i ->
                val n = newsArr.getJSONObject(i)
                NewsItem(
                    id = n.optInt("id", i + 1),
                    title = n.optString("title"),
                    titleUz = n.optString("title_uz", n.optString("title")),
                    source = n.optString("source"),
                    sourceUz = n.optString("source_uz", n.optString("source")),
                    ticker = n.optString("ticker", "AAPL"),
                    tag = n.optString("tag", "Халяль")
                )
            }
        } else fallback.news

        return fallback.copy(
            profile = profile,
            stocks = stocks,
            goals = goals,
            courses = courses,
            news = news,
            online = true
        )
    }

    private fun parseStock(s: JSONObject): StockItem {
        val statusStr = s.optString("halal_status", "HALAL").uppercase()
        val status = when (statusStr) {
            "DOUBTFUL" -> HalalStatus.DOUBTFUL
            "HARAM" -> HalalStatus.HARAM
            else -> HalalStatus.HALAL
        }
        val segArr = s.optJSONArray("revenue_segments") ?: JSONArray()
        val segments = (0 until segArr.length()).map { i ->
            val seg = segArr.getJSONObject(i)
            RevenueSegment(
                label = seg.optString("label"),
                labelUz = seg.optString("label_uz", seg.optString("label")),
                pct = seg.optDouble("pct", 0.0).toFloat(),
                type = seg.optString("type", "halal")
            )
        }
        val critArr = s.optJSONArray("criteria") ?: JSONArray()
        val criteria = (0 until critArr.length()).map { i ->
            val c = critArr.getJSONObject(i)
            val cs = when (c.optString("status", "HALAL").uppercase()) {
                "DOUBTFUL" -> HalalStatus.DOUBTFUL
                "HARAM" -> HalalStatus.HARAM
                else -> HalalStatus.HALAL
            }
            HalalCriterion(
                id = c.optString("id"),
                title = c.optString("title"),
                titleUz = c.optString("title_uz", c.optString("title")),
                valueLabel = c.optString("value_label"),
                subtitle = c.optString("subtitle"),
                subtitleUz = c.optString("subtitle_uz", c.optString("subtitle")),
                pct = c.optDouble("pct", 0.0).toFloat(),
                limitPct = c.optDouble("limit_pct", 30.0).toFloat(),
                status = cs
            )
        }
        val chartArr = s.optJSONArray("chart_1m") ?: JSONArray()
        val chart = (0 until chartArr.length()).map { i -> chartArr.optDouble(i, 100.0).toFloat() }

        return StockItem(
            symbol = s.optString("symbol"),
            name = s.optString("name"),
            exchange = s.optString("exchange", "NASDAQ"),
            sector = s.optString("sector", "Технологии"),
            sectorUz = s.optString("sector_uz", "Texnologiyalar"),
            price = s.optDouble("price", 100.0),
            changeAbs = s.optDouble("change_abs", 1.0),
            changePct = s.optDouble("change_pct", 1.0),
            halalStatus = status,
            checkedDate = s.optString("checked_date", "08.11.2026"),
            marketCap = s.optString("market_cap", "$1,0 трлн"),
            peRatio = s.optString("pe_ratio", "25,0"),
            dividendAnnual = s.optString("dividend_annual", "$1,00"),
            dividendYield = s.optString("dividend_yield", "0,50%"),
            purificationPerDividend = s.optDouble("purification_per_dividend", 0.002),
            dividendPerQuarter = s.optDouble("dividend_per_quarter", 0.25),
            verdictSummary = s.optString("verdict_summary", "Соответствует 4 из 4 критериев"),
            verdictSubtitle = s.optString("verdict_subtitle", "Методика WeGrow, пересмотр раз в квартал"),
            reportPeriod = s.optString("report_period", "Отчётность за III квартал 2026"),
            revenueSegments = segments,
            criteria = criteria,
            chart1m = if (chart.isNotEmpty()) chart else listOf(100f, 105f, 110f)
        )
    }

    private fun parseGoal(g: JSONObject): FinancialGoal {
        return FinancialGoal(
            id = g.optInt("id", 1),
            title = g.optString("title"),
            titleUz = g.optString("title_uz", g.optString("title")),
            targetAmount = g.optDouble("target_amount", 1000.0),
            currentAmount = g.optDouble("current_amount", 0.0),
            monthlyContribution = g.optDouble("monthly_contribution", 100.0),
            deadlineLabel = g.optString("deadline_label", "2027"),
            deadlineLabelUz = g.optString("deadline_label_uz", g.optString("deadline_label", "2027")),
            category = g.optString("category", "savings"),
            icon = g.optString("icon", "target")
        )
    }

    private fun parseCourse(c: JSONObject): CourseItem {
        val mArr = c.optJSONArray("modules") ?: JSONArray()
        val modules = (0 until mArr.length()).map { i ->
            val m = mArr.getJSONObject(i)
            CourseModule(
                number = m.optInt("number", i + 1),
                title = m.optString("title"),
                titleUz = m.optString("title_uz", m.optString("title")),
                meta = m.optString("meta"),
                metaUz = m.optString("meta_uz", m.optString("meta")),
                locked = m.optBoolean("locked", i > 0)
            )
        }
        return CourseItem(
            id = c.optInt("id", 1),
            title = c.optString("title"),
            titleUz = c.optString("title_uz", c.optString("title")),
            level = c.optString("level", "Начальный уровень"),
            levelUz = c.optString("level_uz", "Boshlang'ich daraja"),
            rating = c.optDouble("rating", 4.9),
            reviewsCount = c.optInt("reviews_count", 1240),
            author = c.optString("author", "Команда аналитиков WeGrow"),
            authorUz = c.optString("author_uz", "WeGrow tahlilchilar jamoasi"),
            authorRole = c.optString("author_role", "Авторы курса"),
            authorRoleUz = c.optString("author_role_uz", "Kurs mualliflari"),
            previewDuration = c.optString("preview_duration", "2 мин 14 сек"),
            modulesCount = c.optInt("modules_count", 8),
            lessonsCount = c.optInt("lessons_count", 42),
            accessDuration = c.optString("access_duration", "6 месяцев"),
            accessDurationUz = c.optString("access_duration_uz", "6 oy"),
            priceUzs = c.optString("price_uzs", "1 290 000 сум"),
            purchased = c.optBoolean("purchased", false),
            modules = modules
        )
    }
}
