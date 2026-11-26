package uz.wegrow.invest.ui.screens.notifications

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.wegrow.invest.data.AppNotificationItem
import uz.wegrow.invest.data.StockItem
import uz.wegrow.invest.ui.AppStrings
import uz.wegrow.invest.ui.components.StaggeredReveal
import uz.wegrow.invest.ui.components.WeGrowChip
import uz.wegrow.invest.ui.components.WeGrowTopBar
import uz.wegrow.invest.ui.components.bouncyClickable
import uz.wegrow.invest.ui.theme.WeGrowRadius
import uz.wegrow.invest.ui.theme.WeGrowTextStyles
import uz.wegrow.invest.ui.theme.WeGrowTheme

@Composable
fun NotificationsScreen(
    notifications: List<AppNotificationItem>,
    readIds: Set<String>,
    allStocks: List<StockItem>,
    strings: AppStrings,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenStock: (StockItem) -> Unit,
    onOpenPortfolio: () -> Unit,
    onDeleteAlert: (Int) -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    var filter by remember { mutableStateOf("ALL") }

    val filtered = remember(notifications, filter) {
        if (filter == "ALL") notifications else notifications.filter { it.category == filter }
    }
    val unreadCount = remember(notifications, readIds) { notifications.count { it.id !in readIds } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "notif_top") {
            WeGrowTopBar(
                title = strings.notificationsTitle,
                onBack = onBack,
                trailing = if (unreadCount > 0) {
                    {
                        WeGrowChip(
                            label = if (isUz) "O'qildi ✓" else "Прочитать все",
                            selected = false,
                            onClick = onMarkAllRead
                        )
                    }
                } else null
            )
        }

        item(key = "notif_filters") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    WeGrowChip(
                        label = if (isUz) "Barchasi (${notifications.size})" else "Все (${notifications.size})",
                        selected = filter == "ALL",
                        onClick = { filter = "ALL" }
                    )
                }
                item {
                    WeGrowChip(
                        label = if (isUz) "Alertlar" else "Алерты",
                        selected = filter == "ALERT",
                        onClick = { filter = "ALERT" }
                    )
                }
                item {
                    WeGrowChip(
                        label = if (isUz) "Halol" else "Халяль",
                        selected = filter == "HALAL",
                        onClick = { filter = "HALAL" }
                    )
                }
                item {
                    WeGrowChip(
                        label = if (isUz) "Poklash" else "Очищение",
                        selected = filter == "PURIFICATION",
                        onClick = { filter = "PURIFICATION" }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "notif_empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🔔", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isUz) "Bu bo'limda bildirishnomalar yo'q" else "В этой категории пока нет уведомлений",
                        style = WeGrowTextStyles.TitleM,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isUz) "Aksiya kartochkasidan narx alerti qo'shing"
                        else "Создайте ценовой алерт на экране любой акции — он появится здесь",
                        style = WeGrowTextStyles.BodyS,
                        color = colors.textSecondary
                    )
                }
            }
        } else {
            itemsIndexed(filtered, key = { _, n -> n.id }) { idx, item ->
                val isUnread = item.id !in readIds
                val icon = when (item.category) {
                    "ALERT" -> "🔔"
                    "HALAL" -> "⚠"
                    "PURIFICATION" -> "✦"
                    else -> "📊"
                }
                val accentColor = when {
                    item.category == "HALAL" -> colors.statusHaram
                    item.category == "PURIFICATION" -> colors.saffron
                    item.isUrgent -> colors.brandPrimary
                    else -> colors.statusHalal
                }

                StaggeredReveal(idx) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyClickable(haptic = true) {
                                when {
                                    item.category == "PURIFICATION" -> onOpenPortfolio()
                                    item.symbol != null -> {
                                        allStocks.find { it.symbol == item.symbol }?.let(onOpenStock)
                                    }
                                    else -> onOpenPortfolio()
                                }
                            }
                            .clip(RoundedCornerShape(WeGrowRadius.xl))
                            .background(colors.surface)
                            .border(
                                width = if (isUnread) 1.2.dp else 1.dp,
                                color = if (isUnread) accentColor.copy(alpha = 0.65f) else colors.border,
                                shape = RoundedCornerShape(WeGrowRadius.xl)
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = icon, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUz) item.titleUz else item.title,
                                    style = WeGrowTextStyles.TitleS,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(WeGrowRadius.full))
                                        .background(accentColor.copy(alpha = 0.16f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = item.badge,
                                        style = WeGrowTextStyles.Micro,
                                        color = accentColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isUz) item.bodyUz else item.body,
                                style = WeGrowTextStyles.BodyS,
                                color = colors.textSecondary
                            )
                            if (item.alertId != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WeGrowChip(
                                        label = if (isUz) "O'chirish ✕" else "Удалить алерт ✕",
                                        selected = false,
                                        onClick = { onDeleteAlert(item.alertId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
