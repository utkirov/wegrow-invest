package uz.wegrow.invest.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.wegrow.invest.data.HalalStatus
import uz.wegrow.invest.data.PaymentOrderInfo
import uz.wegrow.invest.ui.AppStrings
import uz.wegrow.invest.ui.components.HalalSeal
import uz.wegrow.invest.ui.components.KhatamPatternBackground
import uz.wegrow.invest.ui.components.StaggeredReveal
import uz.wegrow.invest.ui.components.WeGrowButton
import uz.wegrow.invest.ui.components.WeGrowTopBar
import uz.wegrow.invest.ui.theme.WeGrowRadius
import uz.wegrow.invest.ui.theme.WeGrowTextStyles
import uz.wegrow.invest.ui.theme.WeGrowTheme

/**
 * Payment Orders & Receipts History Screen (Payme / Click)
 */
@Composable
fun OrdersHistoryScreen(
    orders: List<PaymentOrderInfo>,
    strings: AppStrings,
    onBack: () -> Unit,
    onSandboxPay: (orderUid: String, courseId: Int) -> Unit,
    onOpenCourses: () -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "orders_top") {
            WeGrowTopBar(title = strings.ordersHistoryTitle, onBack = onBack)
        }

        if (orders.isEmpty()) {
            item(key = "orders_empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🧾", fontSize = 34.sp)
                    Text(
                        text = if (isUz) "Hozircha buyurtmalar yo'q" else "Заказов пока нет",
                        style = WeGrowTextStyles.TitleM,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (isUz) "Payme yoki Click orqali kurs xarid qilganingizda cheklar shu yerda saqlanadi"
                        else "После оформления курса через Payme или Click все чеки и статусы появятся здесь",
                        style = WeGrowTextStyles.BodyS,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    WeGrowButton(
                        text = if (isUz) "Kurslarga o'tish" else "Перейти к курсам",
                        onClick = onOpenCourses,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            itemsIndexed(orders, key = { _, o -> o.orderUid }) { idx, order ->
                val isPaid = order.status == "PAID"
                val isPending = order.status == "PENDING"
                val statusColor = when {
                    isPaid -> colors.statusHalal
                    isPending -> colors.statusDoubt
                    else -> colors.statusHaram
                }
                val statusSoft = when {
                    isPaid -> colors.statusHalalSoft
                    isPending -> colors.statusDoubtSoft
                    else -> colors.statusHaramSoft
                }
                val statusLabel = when {
                    isPaid -> if (isUz) "To'langan ✓" else "Оплачено ✓"
                    isPending -> if (isUz) "Kutilmoqda" else "Ожидает оплаты"
                    else -> if (isUz) "Bekor qilingan" else "Отменён"
                }

                StaggeredReveal(idx) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(WeGrowRadius.xl))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.orderUid,
                                style = WeGrowTextStyles.CaptionStrong,
                                color = colors.textSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(WeGrowRadius.full))
                                    .background(statusSoft)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(text = statusLabel, style = WeGrowTextStyles.CaptionStrong, color = statusColor)
                            }
                        }

                        Text(
                            text = if (isUz) order.courseTitleUz else order.courseTitle,
                            style = WeGrowTextStyles.TitleM,
                            color = colors.textPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${order.provider} · ${order.createdAt?.take(10) ?: "2026"}",
                                style = WeGrowTextStyles.CaptionM,
                                color = colors.textTertiary
                            )
                            Text(
                                text = "${String.format("%,d", order.amountUzs).replace(',', ' ')} сум",
                                style = WeGrowTextStyles.TitleM,
                                color = colors.brandPrimary
                            )
                        }

                        if (isPending && order.sandboxEnabled) {
                            WeGrowButton(
                                text = if (isUz) "Sandbox to'lovni tasdiqlash ✓" else "Подтвердить тестовую оплату ✓",
                                onClick = { onSandboxPay(order.orderUid, order.courseId) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed AAOIFI Standard #21 & WeGrow Methodology / About Screen
 */
@Composable
fun MethodologyScreen(
    strings: AppStrings,
    onBack: () -> Unit,
    onOpenCalculator: () -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        WeGrowTopBar(title = strings.methodologyTitle, onBack = onBack)

        StaggeredReveal(0) {
            val deepBg = if (colors.isDark) Color(0xFF153123) else colors.brandDeep
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xxl))
                    .background(deepBg)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xxl))
            ) {
                KhatamPatternBackground(modifier = Modifier.matchParentSize())
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HalalSeal(status = HalalStatus.HALAL, size = 56.dp, pulsing = true)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUz) "AAOIFI №21 standarti" else "Стандарт AAOIFI №21",
                            style = WeGrowTextStyles.TitleL,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isUz) "Har chorakda AQShning 105 ta yirik aksiyasi 4 ta qat'iy mezon bo'yicha tekshiriladi."
                            else "Каждая из 105 компаний США ежеквартально проходит 4-ступенчатую шариатскую проверку.",
                            style = WeGrowTextStyles.BodyS,
                            color = colors.onDeepSecondary
                        )
                    }
                }
            }
        }

        val criteriaList = if (isUz) {
            listOf(
                Triple("1", "Asosiy faoliyat (Biznes)", "An'anaviy banklar, sug'urta, alkogol, tamaki, qimor va qurol-yarog' ishlab chiqarish taqiqlangan."),
                Triple("< 30%", "Foizli qarz yuki", "Jami foizli qarzlar kompaniya bozor kapitalizatsiyasining 30% idan oshmasligi shart."),
                Triple("< 30%", "Foizli depozitlar va aktivlar", "Foiz keltiruvchi depozitlar va obligatsiyalar kapitalizatsiyaning 30% idan kam bo'lishi kerak."),
                Triple("< 5%", "Nomaqbul tushum", "Shariatga zid manbalardan tushum umumiy daromadning 5% idan oshmasligi va dividenddan poklanishi lozim.")
            )
        } else {
            listOf(
                Triple("1", "Основная деятельность", "Исключаются классические банки (риба), страхование, алкоголь, табак, азартные игры и производство вооружений."),
                Triple("< 30%", "Долговая нагрузка", "Совокупный процентный долг компании должен составлять строго менее 30% от рыночной капитализации."),
                Triple("< 30%", "Процентные депозиты", "Сумма процентных депозитов и облигаций на балансе компании должна быть менее 30% от капитализации."),
                Triple("< 5%", "Недопустимый доход", "Доля непрофильных процентных/недопустимых поступлений — строго ниже 5% с последующим очищением дивидендов.")
            )
        }

        criteriaList.forEachIndexed { idx, (badge, title, desc) ->
            StaggeredReveal(idx + 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.lg))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(WeGrowRadius.md))
                            .background(colors.brandSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badge,
                            style = WeGrowTextStyles.TitleS,
                            color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = desc, style = WeGrowTextStyles.BodyS, color = colors.textSecondary)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(WeGrowRadius.xl))
                .background(colors.brandSoft)
                .border(1.dp, colors.brandPrimary.copy(alpha = 0.4f), RoundedCornerShape(WeGrowRadius.xl))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isUz) "✦ Tazkiya (Dividend poklash) va Zakot" else "✦ Очищение дивидендов (Тазкия) и Закят",
                style = WeGrowTextStyles.TitleM,
                color = colors.textPrimary
            )
            Text(
                text = if (isUz) "Formula: (Aksiya soni × Dividend) × Nomaqbul tushum %. Zakot: nisobdan ($7 200) yuqori sof aktivlardan yiliga 2,5%."
                else "Формула очищения: (Акции × Дивиденд) × % недопустимого дохода. Закят: 2,5% в год от чистых активов выше нисаба ($7 200).",
                style = WeGrowTextStyles.BodyS,
                color = colors.textSecondary
            )
        }

        WeGrowButton(
            text = if (isUz) "Zakot kalkulyatorini ochish" else "Открыть калькулятор Закята",
            onClick = onOpenCalculator,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "WeGrow Invest v4.1 · ${strings.notInvestmentAdvice}",
            style = WeGrowTextStyles.CaptionM,
            color = colors.textTertiary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
