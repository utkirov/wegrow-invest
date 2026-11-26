package uz.wegrow.invest.ui.screens.portfolio

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import uz.wegrow.invest.data.HalalStatus
import uz.wegrow.invest.data.PortfolioData
import uz.wegrow.invest.data.PriceAlertItem
import uz.wegrow.invest.data.PurificationItem
import uz.wegrow.invest.data.PurificationLedger
import uz.wegrow.invest.data.StockItem
import uz.wegrow.invest.ui.AppStrings
import uz.wegrow.invest.ui.components.AnimatedNumberText
import uz.wegrow.invest.ui.components.HalalBadge
import uz.wegrow.invest.ui.components.StaggeredReveal
import uz.wegrow.invest.ui.components.WeGrowButton
import uz.wegrow.invest.ui.components.WeGrowChip
import uz.wegrow.invest.ui.components.bouncyClickable
import uz.wegrow.invest.ui.theme.WeGrowRadius
import uz.wegrow.invest.ui.theme.WeGrowTextStyles
import uz.wegrow.invest.ui.theme.WeGrowTheme

@Composable
fun PortfolioScreen(
    portfolio: PortfolioData,
    watchlistSymbols: Set<String>,
    allStocks: List<StockItem>,
    alerts: List<PriceAlertItem>,
    purification: PurificationLedger,
    strings: AppStrings,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    onOpenStock: (StockItem) -> Unit,
    onAddTransaction: (symbol: String, side: String, quantity: Double, price: Double) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    onClearPurification: (PurificationItem) -> Unit,
    onDeleteAlert: (Int) -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    var showAddDialog by remember { mutableStateOf(false) }

    val watchlistStocks = remember(watchlistSymbols, allStocks) {
        allStocks.filter { it.symbol in watchlistSymbols }
    }

    val sectorAllocation = remember(portfolio.positions, isUz) {
        val total = portfolio.positions.sumOf { it.marketValue }.coerceAtLeast(0.01)
        portfolio.positions
            .groupBy { if (isUz) it.sectorUz else it.sector }
            .map { (sec, list) ->
                val sumVal = list.sumOf { it.marketValue }
                sec to ((sumVal / total) * 100.0).toFloat()
            }
            .sortedByDescending { it.second }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "portfolio_top") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .bouncyClickable(haptic = true, onClick = onBack)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "←", style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
                    }
                    Text(
                        text = if (isUz) "Investitsiya portfeli" else "Инвестиционный портфель",
                        style = WeGrowTextStyles.TitleL,
                        color = colors.textPrimary
                    )
                } else {
                    Text(
                        text = strings.navPortfolio,
                        style = WeGrowTextStyles.DisplayM,
                        color = colors.textPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .bouncyClickable(haptic = true) { showAddDialog = true }
                        .clip(CircleShape)
                        .background(colors.brandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", style = WeGrowTextStyles.TitleL, color = colors.textOnBrand)
                }
            }
        }

        item(key = "portfolio_summary") {
            StaggeredReveal(0) {
                val sum = portfolio.summary
                val positive = sum.totalPnlAbs >= 0
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                        .padding(18.dp)
                ) {
                    Text(
                        text = if (isUz) "Umumiy qiymat" else "Стоимость портфеля",
                        style = WeGrowTextStyles.CaptionStrong,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AnimatedNumberText(
                        targetValue = sum.totalValue,
                        formatter = { "$${String.format("%.2f", it)}" },
                        style = WeGrowTextStyles.DisplayL,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (positive) "+" else ""}$${String.format("%.2f", sum.totalPnlAbs)} (${String.format("%.2f", sum.totalPnlPct)}%)",
                            style = WeGrowTextStyles.TitleS,
                            color = if (positive) colors.statusHalal else colors.statusHaram
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(WeGrowRadius.full))
                                .background(colors.brandSoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Halal ${String.format("%.1f", sum.halalCompliancePct)}%",
                                style = WeGrowTextStyles.CaptionStrong,
                                color = if (sum.halalCompliancePct >= 99.9) colors.statusHalal else colors.saffron
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    WeGrowButton(
                        text = if (isUz) "+ Bitim qo'shish (Sotib olish / Sotish)" else "+ Добавить сделку (Покупка / Продажа)",
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (sectorAllocation.isNotEmpty()) {
            item(key = "portfolio_allocation") {
                val barPalette = listOf(colors.brandPrimary, colors.statusHalal, colors.accent, colors.saffron, colors.statusDoubt)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isUz) "Sektorlar bo'yicha taqsimot" else "Распределение по секторам",
                        style = WeGrowTextStyles.TitleS,
                        color = colors.textPrimary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(WeGrowRadius.full)),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        sectorAllocation.forEachIndexed { idx, (_, pct) ->
                            Box(
                                modifier = Modifier
                                    .weight(pct.coerceAtLeast(3f))
                                    .height(10.dp)
                                    .background(barPalette[idx % barPalette.size])
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        sectorAllocation.take(3).forEachIndexed { idx, (name, pct) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(barPalette[idx % barPalette.size])
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "$name ${pct.toInt()}%",
                                    style = WeGrowTextStyles.CaptionM,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (portfolio.warnings.isNotEmpty()) {
            item(key = "portfolio_warnings") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.lg))
                        .background(colors.surface)
                        .border(1.dp, colors.statusHaram, RoundedCornerShape(WeGrowRadius.lg))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isUz) "⚠ Shariat bo'yicha ogohlantirish" else "⚠ Внимание по халяльности портфеля",
                        style = WeGrowTextStyles.TitleS,
                        color = colors.statusHaram
                    )
                    portfolio.warnings.forEach { w ->
                        Text(
                            text = "${w.symbol} (${w.halalStatus.name}): ${w.reason}",
                            style = WeGrowTextStyles.CaptionM,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }

        item(key = "positions_header") {
            Text(
                text = if (isUz) "Aktivlar (${portfolio.positions.size})" else "Позиции (${portfolio.positions.size})",
                style = WeGrowTextStyles.TitleL,
                color = colors.textPrimary
            )
        }

        if (portfolio.positions.isEmpty()) {
            item(key = "positions_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.lg))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
                        .padding(18.dp)
                ) {
                    Text(
                        text = if (isUz) "Portfel bo'sh. Birinchi halol aksiyani qo'shing." else "Портфель пока пуст. Добавьте первую сделку по халяльной акции.",
                        style = WeGrowTextStyles.BodyS,
                        color = colors.textSecondary
                    )
                }
            }
        } else {
            items(portfolio.positions, key = { "pos_${it.symbol}" }) { pos ->
                val stockRef = allStocks.find { it.symbol == pos.symbol }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncyClickable(haptic = true) { stockRef?.let(onOpenStock) }
                        .clip(RoundedCornerShape(WeGrowRadius.lg))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = pos.symbol, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            HalalBadge(status = pos.halalStatus, strings = strings)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.2f", pos.quantity)} шт · avg $${String.format("%.2f", pos.avgPrice)}",
                            style = WeGrowTextStyles.CaptionM,
                            color = colors.textSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${String.format("%.2f", pos.marketValue)}",
                            style = WeGrowTextStyles.TitleM,
                            color = colors.textPrimary
                        )
                        val up = pos.pnlAbs >= 0
                        Text(
                            text = "${if (up) "+" else ""}$${String.format("%.2f", pos.pnlAbs)} (${String.format("%.1f", pos.pnlPct)}%)",
                            style = WeGrowTextStyles.CaptionStrong,
                            color = if (up) colors.statusHalal else colors.statusHaram
                        )
                    }
                }
            }
        }

        // Purification Ledger
        item(key = "purification_section") {
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
                        text = if (isUz) "Dividend poklash (${purification.quarterLabel})" else "Очищение дивидендов (${purification.quarterLabel})",
                        style = WeGrowTextStyles.TitleM,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "$${String.format("%.3f", purification.totalPendingUsd)}",
                        style = WeGrowTextStyles.TitleM,
                        color = if (purification.totalPendingUsd > 0) colors.saffron else colors.statusHalal
                    )
                }
                if (purification.items.isEmpty()) {
                    Text(
                        text = if (isUz) "Poklash talab etiladigan dividendlar yo'q." else "Нет дивидендов к очищению за текущий квартал.",
                        style = WeGrowTextStyles.CaptionM,
                        color = colors.textSecondary
                    )
                } else {
                    purification.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.symbol, style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
                                Text(
                                    text = "${if (isUz) "Qoldiq" else "К очищению"}: $${String.format("%.3f", item.remainingAmount)}",
                                    style = WeGrowTextStyles.CaptionM,
                                    color = colors.textSecondary
                                )
                            }
                            if (item.status == "PENDING" && item.remainingAmount > 0.0) {
                                WeGrowChip(
                                    label = if (isUz) "Poklandi ✓" else "Очищено ✓",
                                    selected = true,
                                    onClick = { onClearPurification(item) }
                                )
                            } else {
                                Text(
                                    text = if (isUz) "Poklangan" else "Выполнено",
                                    style = WeGrowTextStyles.CaptionStrong,
                                    color = colors.statusHalal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Watchlist
        item(key = "watchlist_header") {
            Text(
                text = if (isUz) "Tanlanganlar (Watchlist · ${watchlistStocks.size})" else "Избранное (Watchlist · ${watchlistStocks.size})",
                style = WeGrowTextStyles.TitleL,
                color = colors.textPrimary
            )
        }
        items(watchlistStocks, key = { "wl_${it.symbol}" }) { st ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable(haptic = true) { onOpenStock(st) }
                    .clip(RoundedCornerShape(WeGrowRadius.lg))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${st.symbol} · ${st.name}", style = WeGrowTextStyles.TitleS, color = colors.textPrimary)
                    Text(
                        text = "$${String.format("%.2f", st.price)} (${if (st.changePct >= 0) "+" else ""}${String.format("%.2f", st.changePct)}%)",
                        style = WeGrowTextStyles.CaptionM,
                        color = if (st.changePct >= 0) colors.statusHalal else colors.statusHaram
                    )
                }
                WeGrowChip(label = "★", selected = true, onClick = { onToggleWatchlist(st.symbol) })
            }
        }

        // Price Alerts
        if (alerts.isNotEmpty()) {
            item(key = "alerts_header") {
                Text(
                    text = if (isUz) "Narx ogohlantirishlari (${alerts.size})" else "Ценовые алерты (${alerts.size})",
                    style = WeGrowTextStyles.TitleL,
                    color = colors.textPrimary
                )
            }
            items(alerts, key = { "al_${it.id}" }) { al ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.lg))
                        .background(colors.surface)
                        .border(
                            1.dp,
                            if (al.triggered) colors.brandPrimary else colors.border,
                            RoundedCornerShape(WeGrowRadius.lg)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${al.symbol} · ${al.condition} $${String.format("%.2f", al.targetPrice)}",
                            style = WeGrowTextStyles.TitleS,
                            color = colors.textPrimary
                        )
                        if (al.message.isNotBlank()) {
                            Text(text = al.message, style = WeGrowTextStyles.CaptionM, color = colors.brandPrimary)
                        }
                    }
                    WeGrowChip(label = "✕", selected = false, onClick = { onDeleteAlert(al.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            isUz = isUz,
            defaultSymbol = allStocks.firstOrNull()?.symbol ?: "AAPL",
            defaultPrice = allStocks.firstOrNull()?.price ?: 228.45,
            onDismiss = { showAddDialog = false },
            onSubmit = { sym, side, qty, price ->
                showAddDialog = false
                onAddTransaction(sym, side, qty, price)
            }
        )
    }
}

@Composable
fun AddTransactionDialog(
    isUz: Boolean,
    defaultSymbol: String,
    defaultPrice: Double,
    onDismiss: () -> Unit,
    onSubmit: (symbol: String, side: String, qty: Double, price: Double) -> Unit
) {
    val colors = WeGrowTheme.colors
    var symbol by remember { mutableStateOf(defaultSymbol) }
    var side by remember { mutableStateOf("BUY") }
    var qtyText by remember { mutableStateOf("5") }
    var priceText by remember { mutableStateOf(String.format("%.2f", defaultPrice).replace(',', '.')) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceSolid,
        title = {
            Text(
                text = if (isUz) "Yangi bitim" else "Новая сделка",
                style = WeGrowTextStyles.TitleL,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeGrowChip(
                        label = if (isUz) "Sotib olish" else "Покупка",
                        selected = side == "BUY",
                        onClick = { side = "BUY" },
                        modifier = Modifier.weight(1f)
                    )
                    WeGrowChip(
                        label = if (isUz) "Sotish" else "Продажа",
                        selected = side == "SELL",
                        onClick = { side = "SELL" },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase().take(10) },
                    label = { Text(if (isUz) "Tiker" else "Тикер") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text(if (isUz) "Miqdor" else "Количество") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(if (isUz) "Narx (USD)" else "Цена (USD)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val qty = qtyText.replace(',', '.').toDoubleOrNull() ?: return@TextButton
                val pr = priceText.replace(',', '.').toDoubleOrNull() ?: return@TextButton
                if (symbol.isNotBlank() && qty > 0 && pr > 0) {
                    onSubmit(symbol.trim().uppercase(), side, qty, pr)
                }
            }) {
                Text(text = if (isUz) "Saqlash" else "Сохранить", color = colors.brandPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isUz) "Bekor" else "Отмена", color = colors.textSecondary)
            }
        }
    )
}
