package uz.wegrow.invest.ui.screens.brokers

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.wegrow.invest.data.BrokerInfo
import uz.wegrow.invest.ui.AppStrings
import uz.wegrow.invest.ui.components.KhatamPatternBackground
import uz.wegrow.invest.ui.components.StaggeredReveal
import uz.wegrow.invest.ui.components.WeGrowChip
import uz.wegrow.invest.ui.components.WeGrowTopBar
import uz.wegrow.invest.ui.components.bouncyClickable
import uz.wegrow.invest.ui.theme.WeGrowRadius
import uz.wegrow.invest.ui.theme.WeGrowTextStyles
import uz.wegrow.invest.ui.theme.WeGrowTheme

@Composable
fun BrokersScreen(
    brokers: List<BrokerInfo>,
    strings: AppStrings,
    onBack: () -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    var filter by remember { mutableStateOf("ALL") }
    var expandedId by remember { mutableStateOf<String?>(brokers.firstOrNull()?.id) }

    val filtered = remember(brokers, filter) {
        when (filter) {
            "US" -> brokers.filter { it.usStocks }
            "UZ_CARD" -> brokers.filter { it.uzCardFunding }
            "HALAL" -> brokers.filter { it.halalCashAccount }
            else -> brokers
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "brokers_top") {
            WeGrowTopBar(title = strings.brokersTitle, onBack = onBack)
        }

        item(key = "brokers_checklist") {
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isUz) "✦ Halol hisob chek-listi" else "✦ Шариатский чек-лист при открытии счёта",
                            style = WeGrowTextStyles.TitleM,
                            color = colors.brandPrimary
                        )
                        Text(
                            text = if (isUz) "1. Faqat Cash-hisob (marjinal yelkasiz)\n2. Foiz va svop (D-hisob / Stock Yield) o'chirilgan\n3. AQSh aksiyalari uchun W-8BEN imzolangan"
                            else "1. Только Cash-счёт без маржинального плеча\n2. Отключены проценты на остаток и кредитование акций\n3. Подписана форма W-8BEN для рынка США",
                            style = WeGrowTextStyles.BodyS,
                            color = Color(0xFFF1F5EC)
                        )
                    }
                }
            }
        }

        item(key = "brokers_filters") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    WeGrowChip(strings.filterAll, selected = filter == "ALL", onClick = { filter = "ALL" })
                }
                item {
                    WeGrowChip(
                        label = if (isUz) "AQSh aksiyalari" else "Акции США",
                        selected = filter == "US",
                        onClick = { filter = "US" }
                    )
                }
                item {
                    WeGrowChip(
                        label = if (isUz) "UZ kartalar" else "Пополнение картой УЗ",
                        selected = filter == "UZ_CARD",
                        onClick = { filter = "UZ_CARD" }
                    )
                }
                item {
                    WeGrowChip(
                        label = if (isUz) "Cash-hisob" else "Cash-счёт (Халяль)",
                        selected = filter == "HALAL",
                        onClick = { filter = "HALAL" }
                    )
                }
            }
        }

        itemsIndexed(filtered, key = { _, b -> "broker_${b.id}" }) { idx, broker ->
            val isExpanded = expandedId == broker.id
            StaggeredReveal(idx + 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClickable(haptic = true) {
                            expandedId = if (isExpanded) null else broker.id
                        }
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(
                            width = if (isExpanded) 1.4.dp else 1.dp,
                            color = if (isExpanded) colors.brandPrimary else colors.border,
                            shape = RoundedCornerShape(WeGrowRadius.xl)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(WeGrowRadius.full))
                                    .background(colors.brandSoft)
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isUz) broker.badgeUz else broker.badge,
                                    style = WeGrowTextStyles.Micro,
                                    color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = broker.name, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                            Text(text = broker.jurisdiction, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "★ ", color = colors.saffron, fontSize = 15.sp)
                            Text(text = "${broker.rating}", style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BrokerMiniStat(
                            label = if (isUz) "Min. depozit" else "Мин. депозит",
                            value = broker.minDeposit,
                            modifier = Modifier.weight(1f)
                        )
                        BrokerMiniStat(
                            label = if (isUz) "Komissiya" else "Комиссия",
                            value = broker.commission,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(WeGrowRadius.md))
                                    .background(colors.statusHalalSoft)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = if (isUz) "Halol sozlama:" else "Настройка для халяль-торговли:",
                                    style = WeGrowTextStyles.CaptionStrong,
                                    color = colors.statusHalal
                                )
                                Text(
                                    text = if (isUz) broker.halalNoteUz else broker.halalNote,
                                    style = WeGrowTextStyles.CaptionM,
                                    color = colors.textPrimary
                                )
                            }

                            Text(
                                text = "${if (isUz) "To'ldirish" else "Пополнение"}: ${if (isUz) broker.fundingMethodsUz else broker.fundingMethods}",
                                style = WeGrowTextStyles.BodyS,
                                color = colors.textSecondary
                            )

                            val prosList = if (isUz) broker.prosUz else broker.pros
                            val consList = if (isUz) broker.consUz else broker.cons

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                prosList.forEach { p ->
                                    Text(text = "✓ $p", style = WeGrowTextStyles.CaptionM, color = colors.statusHalal)
                                }
                                consList.forEach { c ->
                                    Text(text = "• $c", style = WeGrowTextStyles.CaptionM, color = colors.textTertiary)
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isExpanded) (if (isUz) "Yig'ish ▲" else "Свернуть ▲") else (if (isUz) "Batafsil va halol sozlama ▼" else "Подробнее и шариат-настройка ▼"),
                        style = WeGrowTextStyles.CaptionStrong,
                        color = colors.brandPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun BrokerMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = WeGrowTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(WeGrowRadius.md))
            .background(colors.surfaceMuted)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(text = label, style = WeGrowTextStyles.Micro, color = colors.textTertiary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
    }
}
