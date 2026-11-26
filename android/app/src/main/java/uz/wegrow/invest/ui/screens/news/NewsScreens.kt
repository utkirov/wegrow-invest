package uz.wegrow.invest.ui.screens.news

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.wegrow.invest.data.NewsItem
import uz.wegrow.invest.data.StockItem
import uz.wegrow.invest.ui.AppStrings
import uz.wegrow.invest.ui.components.HalalBadge
import uz.wegrow.invest.ui.components.HalalSeal
import uz.wegrow.invest.ui.components.KhatamPatternBackground
import uz.wegrow.invest.ui.components.StaggeredReveal
import uz.wegrow.invest.ui.components.WeGrowButton
import uz.wegrow.invest.ui.components.WeGrowChip
import uz.wegrow.invest.ui.components.WeGrowMarkdownText
import uz.wegrow.invest.ui.components.WeGrowTopBar
import uz.wegrow.invest.ui.components.bouncyClickable
import uz.wegrow.invest.ui.screens.NewsCardItem
import uz.wegrow.invest.ui.theme.WeGrowRadius
import uz.wegrow.invest.ui.theme.WeGrowTextStyles
import uz.wegrow.invest.ui.theme.WeGrowTheme

@Composable
fun NewsFeedScreen(
    news: List<NewsItem>,
    strings: AppStrings,
    onBack: () -> Unit,
    onOpenArticle: (NewsItem) -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    var search by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("ALL") }

    val tags = remember(news) { news.map { it.tag }.filter { it.isNotBlank() }.distinct() }

    val filtered = remember(news, search, selectedTag) {
        val q = search.trim().lowercase()
        news.filter { item ->
            val passTag = selectedTag == "ALL" || item.tag.equals(selectedTag, ignoreCase = true)
            val passQ = q.isBlank() ||
                item.title.lowercase().contains(q) ||
                item.titleUz.lowercase().contains(q) ||
                item.ticker.lowercase().contains(q) ||
                item.summary.lowercase().contains(q)
            passTag && passQ
        }
    }

    val featured = filtered.firstOrNull()
    val rest = if (filtered.size > 1) filtered.drop(1) else emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "news_top") {
            WeGrowTopBar(title = strings.newsFeedTitle, onBack = onBack)
        }

        item(key = "news_search_and_tags") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = if (isUz) "Tiker yoki mavzu bo'yicha qidirish..." else "Поиск по тикеру или теме...",
                            style = WeGrowTextStyles.BodyS
                        )
                    },
                    shape = RoundedCornerShape(WeGrowRadius.lg),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brandPrimary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "tag_all") {
                        WeGrowChip(
                            label = strings.filterAll,
                            selected = selectedTag == "ALL",
                            onClick = { selectedTag = "ALL" }
                        )
                    }
                    items(tags, key = { it }) { tag ->
                        WeGrowChip(
                            label = tag,
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag }
                        )
                    }
                }
            }
        }

        if (featured != null) {
            item(key = "news_featured_${featured.id}") {
                StaggeredReveal(0) {
                    val deepBg = if (colors.isDark) Color(0xFF153123) else colors.brandDeep
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bouncyClickable(haptic = true) { onOpenArticle(featured) }
                            .clip(RoundedCornerShape(WeGrowRadius.xxl))
                            .background(deepBg)
                            .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xxl))
                    ) {
                        KhatamPatternBackground(modifier = Modifier.matchParentSize())
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(WeGrowRadius.full))
                                        .background(colors.brandPrimary)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${featured.ticker} · ${featured.tag}",
                                        style = WeGrowTextStyles.CaptionStrong,
                                        color = colors.textOnBrand
                                    )
                                }
                                Text(
                                    text = "${featured.readMin} ${if (isUz) "daq" else "мин"}",
                                    style = WeGrowTextStyles.CaptionM,
                                    color = colors.onDeepSecondary
                                )
                            }
                            Text(
                                text = if (isUz) featured.titleUz else featured.title,
                                style = WeGrowTextStyles.TitleL,
                                color = Color(0xFFF1F5EC)
                            )
                            val sumText = if (isUz) featured.summaryUz else featured.summary
                            if (sumText.isNotBlank()) {
                                Text(
                                    text = sumText,
                                    style = WeGrowTextStyles.BodyS,
                                    color = colors.onDeepSecondary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${if (isUz) featured.sourceUz else featured.source} · ${featured.publishedAt}",
                                style = WeGrowTextStyles.CaptionM,
                                color = colors.brandPrimary
                            )
                        }
                    }
                }
            }
        }

        itemsIndexed(rest, key = { _, n -> "feed_${n.id}" }) { idx, item ->
            StaggeredReveal(idx + 1) {
                NewsCardItem(
                    item = item,
                    isUz = isUz,
                    onClick = { onOpenArticle(item) }
                )
            }
        }
    }
}

@Composable
fun NewsDetailScreen(
    article: NewsItem,
    relatedStock: StockItem?,
    otherNews: List<NewsItem>,
    strings: AppStrings,
    onBack: () -> Unit,
    onOpenStock: (StockItem) -> Unit,
    onOpenHalalAnalysis: (StockItem) -> Unit,
    onOpenArticle: (NewsItem) -> Unit
) {
    val colors = WeGrowTheme.colors
    val isUz = strings.langCode == "uz"
    val bodyText = (if (isUz) article.bodyUz else article.body).ifBlank {
        if (isUz) article.summaryUz.ifBlank { article.titleUz } else article.summary.ifBlank { article.title }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        WeGrowTopBar(
            title = if (isUz) "Maqola" else "Статья",
            onBack = onBack,
            trailing = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(WeGrowRadius.full))
                        .background(colors.brandSoft)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = article.tag.ifBlank { article.ticker },
                        style = WeGrowTextStyles.CaptionStrong,
                        color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                    )
                }
            }
        )

        StaggeredReveal(0) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isUz) article.titleUz else article.title,
                    style = WeGrowTextStyles.HeadlineM,
                    color = colors.textPrimary
                )
                Text(
                    text = "${if (isUz) article.sourceUz else article.source} · ${article.publishedAt} · ${article.readMin} ${if (isUz) "daq o'qish" else "мин чтения"}",
                    style = WeGrowTextStyles.CaptionM,
                    color = colors.textTertiary
                )
            }
        }

        val summary = if (isUz) article.summaryUz else article.summary
        if (summary.isNotBlank()) {
            StaggeredReveal(1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.lg))
                        .background(colors.brandSoft)
                        .border(1.dp, colors.brandPrimary.copy(alpha = 0.35f), RoundedCornerShape(WeGrowRadius.lg))
                        .padding(14.dp)
                ) {
                    Text(
                        text = summary,
                        style = WeGrowTextStyles.BodyM,
                        color = colors.textPrimary
                    )
                }
            }
        }

        StaggeredReveal(2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                    .padding(18.dp)
            ) {
                WeGrowMarkdownText(markdown = bodyText)
            }
        }

        if (relatedStock != null) {
            StaggeredReveal(3) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WeGrowRadius.xl))
                        .background(colors.surface)
                        .border(1.dp, colors.brandPrimary.copy(alpha = 0.5f), RoundedCornerShape(WeGrowRadius.xl))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isUz) "Maqoladagi aksiya" else "Акция из материала",
                        style = WeGrowTextStyles.CaptionStrong,
                        color = colors.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.brandSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = relatedStock.symbol.take(1),
                                style = WeGrowTextStyles.TitleM,
                                color = if (colors.isDark) colors.brandPrimary else colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = relatedStock.symbol, style = WeGrowTextStyles.TitleM, color = colors.textPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                HalalSeal(status = relatedStock.halalStatus, size = 16.dp)
                            }
                            Text(text = relatedStock.name, style = WeGrowTextStyles.CaptionM, color = colors.textSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${String.format("%.2f", relatedStock.price)}",
                                style = WeGrowTextStyles.TitleM,
                                color = colors.textPrimary
                            )
                            HalalBadge(status = relatedStock.halalStatus, strings = strings, small = true)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        WeGrowButton(
                            text = if (isUz) "Aksiyani ochish" else "Открыть акцию",
                            onClick = { onOpenStock(relatedStock) },
                            modifier = Modifier.weight(1f)
                        )
                        WeGrowButton(
                            text = "AAOIFI",
                            outlined = true,
                            onClick = { onOpenHalalAnalysis(relatedStock) },
                            modifier = Modifier.weight(0.75f)
                        )
                    }
                }
            }
        }

        if (otherNews.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isUz) "Boshqa yangiliklar" else "Читайте также",
                style = WeGrowTextStyles.TitleL,
                color = colors.textPrimary
            )
            otherNews.take(3).forEach { nextItem ->
                NewsCardItem(
                    item = nextItem,
                    isUz = isUz,
                    onClick = { onOpenArticle(nextItem) }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
