package uz.wegrow.invest.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.wegrow.invest.data.HalalStatus
import uz.wegrow.invest.ui.AppStrings
import uz.wegrow.invest.ui.theme.WeGrowRadius
import uz.wegrow.invest.ui.theme.WeGrowTextStyles
import uz.wegrow.invest.ui.theme.WeGrowTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pure Path helper (does not depend on DrawScope) for building 8-pointed Khatam Star
 */
private fun buildKhatamStarPath(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    targetPath: Path = Path()
): Path {
    val points = 16
    for (i in 0 until points) {
        val angle = (i * 2.0 * PI / points) - (PI / 2.0)
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) targetPath.moveTo(x, y) else targetPath.lineTo(x, y)
    }
    targetPath.close()
    return targetPath
}

/**
 * Spring press scale (1f -> 0.95f) rendered solely in GPU graphicsLayer (zero relayout)
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    haptic: Boolean = false,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncyScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled
        ) {
            if (haptic) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            onClick()
        }
}

/**
 * One-shot staggered entrance animation (alpha 0->1, translateY 16.dp->0.dp) in graphicsLayer
 */
@Composable
fun StaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val delayMs = (index.coerceIn(0, 6) * 35).toLong()
        if (delayMs > 0L) delay(delayMs)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )
    }
    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            translationY = (1f - p) * 16.dp.toPx()
        }
    ) {
        content()
    }
}

/**
 * Smooth numeric roll-up / interpolation text for balances, goals, and calculator outputs
 */
@Composable
fun AnimatedNumberText(
    targetValue: Double,
    formatter: (Double) -> String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedVal by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "animatedNumber"
    )
    Text(
        text = formatter(animatedVal.toDouble()),
        style = style,
        color = color,
        modifier = modifier
    )
}

/**
 * Spring-animated horizontal progress bar
 */
@Composable
fun AnimatedProgressBar(
    fraction: Float,
    barColor: Color,
    trackColor: Color,
    height: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    val animFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "progressFraction"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(WeGrowRadius.full))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animFraction)
                .fillMaxHeight()
                .clip(RoundedCornerShape(WeGrowRadius.full))
                .background(barColor)
        )
    }
}

/**
 * Subtle breathing live indicator dot (only active when mounted)
 */
@Composable
fun LivePulseDot(
    color: Color,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "livePulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier.size(size * 2f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = pulseAlpha
                }
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Brand/Logo (64x64) cached with drawWithCache
 */
@Composable
fun WeGrowBrandLogo(
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val colors = WeGrowTheme.colors
    Spacer(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                val outerR = this.size.minDimension * 0.47f
                val innerR = outerR * 0.80f
                val starPath = buildKhatamStarPath(c, outerR, innerR)
                val strokeStyle = Stroke(
                    width = this.size.minDimension * 0.04f,
                    join = StrokeJoin.Round
                )
                val unit = this.size.minDimension / 64f
                val barW = 5.6f * unit
                val baseY = 44f * unit
                val corner = CornerRadius(2f * unit, 2f * unit)

                onDrawBehind {
                    drawPath(path = starPath, color = colors.brandSoft)
                    drawPath(path = starPath, color = colors.brandPrimary, style = strokeStyle)
                    drawRoundRect(
                        color = colors.brandPrimary,
                        topLeft = Offset(19f * unit, baseY - 11f * unit),
                        size = Size(barW, 11f * unit),
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = colors.brandPrimary,
                        topLeft = Offset(29.2f * unit, baseY - 18f * unit),
                        size = Size(barW, 18f * unit),
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = colors.brandPrimary,
                        topLeft = Offset(39.4f * unit, baseY - 25f * unit),
                        size = Size(barW, 25f * unit),
                        cornerRadius = corner
                    )
                    drawCircle(
                        color = colors.statusHalal,
                        radius = 3.2f * unit,
                        center = Offset(21.8f * unit, 25.5f * unit)
                    )
                }
            }
    )
}

/**
 * Pattern/Khatam background motif: builds a SINGLE merged Path once in drawWithCache (1 GPU draw call)
 */
@Composable
fun KhatamPatternBackground(
    modifier: Modifier = Modifier
) {
    val patternColor = WeGrowTheme.colors.onDeepPattern
    Spacer(
        modifier = modifier.drawWithCache {
            val step = 82.dp.toPx()
            val cols = (size.width / step).toInt() + 2
            val rows = (size.height / step).toInt() + 2
            val mergedPath = Path()
            val outerR = step * 0.42f
            val innerR = step * 0.31f
            for (r in 0..rows) {
                for (c in 0..cols) {
                    val cx = c * step + (if (r % 2 == 1) step / 2f else 0f)
                    val cy = r * step
                    buildKhatamStarPath(Offset(cx, cy), outerR, innerR, mergedPath)
                }
            }
            val strokeStyle = Stroke(width = 1.1.dp.toPx())

            onDrawBehind {
                drawPath(path = mergedPath, color = patternColor, style = strokeStyle)
            }
        }
    )
}

/**
 * Halal seal component cached with drawWithCache; optional subtle breathing animation when pulsing = true
 */
@Composable
fun HalalSeal(
    status: HalalStatus,
    size: Dp = 48.dp,
    pulsing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = WeGrowTheme.colors
    val fgColor = when (status) {
        HalalStatus.HALAL -> colors.statusHalal
        HalalStatus.DOUBTFUL -> colors.statusDoubt
        HalalStatus.HARAM -> colors.statusHaram
    }
    val bgColor = when (status) {
        HalalStatus.HALAL -> colors.statusHalalSoft
        HalalStatus.DOUBTFUL -> colors.statusDoubtSoft
        HalalStatus.HARAM -> colors.statusHaramSoft
    }

    val pulseScale = if (pulsing) {
        val infinite = rememberInfiniteTransition(label = "sealPulse")
        infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.045f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "sealScale"
        ).value
    } else {
        1f
    }

    Spacer(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (pulsing) {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
            }
            .drawWithCache {
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                val outerR = this.size.minDimension * 0.46f
                val innerR = outerR * 0.80f
                val starPath = buildKhatamStarPath(c, outerR, innerR)
                val starStroke = Stroke(
                    width = (this.size.minDimension * 0.045f).coerceAtLeast(1.5f),
                    join = StrokeJoin.Round
                )
                val iconScale = this.size.minDimension * 0.20f
                val strokeW = (this.size.minDimension * 0.055f).coerceAtLeast(1.8f)
                val iconStroke = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val checkPath = if (status == HalalStatus.HALAL) {
                    Path().apply {
                        moveTo(c.x - iconScale * 0.9f, c.y + iconScale * 0.05f)
                        lineTo(c.x - iconScale * 0.2f, c.y + iconScale * 0.75f)
                        lineTo(c.x + iconScale * 1.0f, c.y - iconScale * 0.65f)
                    }
                } else null

                onDrawBehind {
                    drawPath(path = starPath, color = bgColor)
                    drawPath(path = starPath, color = fgColor, style = starStroke)
                    when (status) {
                        HalalStatus.HALAL -> {
                            checkPath?.let { drawPath(it, color = fgColor, style = iconStroke) }
                        }
                        HalalStatus.DOUBTFUL -> {
                            drawLine(
                                color = fgColor,
                                start = Offset(c.x, c.y - iconScale * 0.85f),
                                end = Offset(c.x, c.y + iconScale * 0.25f),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = fgColor,
                                radius = strokeW * 0.65f,
                                center = Offset(c.x, c.y + iconScale * 0.85f)
                            )
                        }
                        HalalStatus.HARAM -> {
                            drawLine(
                                color = fgColor,
                                start = Offset(c.x - iconScale * 0.7f, c.y - iconScale * 0.7f),
                                end = Offset(c.x + iconScale * 0.7f, c.y + iconScale * 0.7f),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = fgColor,
                                start = Offset(c.x + iconScale * 0.7f, c.y - iconScale * 0.7f),
                                end = Offset(c.x - iconScale * 0.7f, c.y + iconScale * 0.7f),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
    )
}

/**
 * Halal badge component (Status = Halal / Doubtful / Not halal)
 */
@Composable
fun HalalBadge(
    status: HalalStatus,
    strings: AppStrings,
    small: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = WeGrowTheme.colors
    val (bg, fg, label) = when (status) {
        HalalStatus.HALAL -> Triple(colors.statusHalalSoft, colors.statusHalal, strings.statusHalal)
        HalalStatus.DOUBTFUL -> Triple(colors.statusDoubtSoft, colors.statusDoubt, strings.statusDoubtful)
        HalalStatus.HARAM -> Triple(colors.statusHaramSoft, colors.statusHaram, strings.statusHaram)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(WeGrowRadius.full))
            .background(bg)
            .padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HalalSeal(status = status, size = if (small) 14.dp else 16.dp)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = if (small) WeGrowTextStyles.CaptionStrong else WeGrowTextStyles.LabelM,
            color = fg
        )
    }
}

/**
 * Primary / Outline WeGrow button with spring bounce & haptic feedback
 */
@Composable
fun WeGrowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    enabled: Boolean = true
) {
    val colors = WeGrowTheme.colors
    val haptics = LocalHapticFeedback.current
    val bg = when {
        !enabled -> colors.surfaceMuted
        outlined -> Color.Transparent
        else -> colors.brandPrimary
    }
    val fg = when {
        !enabled -> colors.textTertiary
        outlined -> colors.textPrimary
        else -> colors.textOnBrand
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .bouncyClickable(enabled = enabled, haptic = false) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .clip(RoundedCornerShape(WeGrowRadius.lg))
            .background(bg)
            .then(
                if (outlined) Modifier.border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.lg))
                else Modifier
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = WeGrowTextStyles.TitleM,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Chip component with spring bounce and smooth color transition
 */
@Composable
fun WeGrowChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WeGrowTheme.colors
    val bg by animateColorAsState(
        targetValue = if (selected) colors.brandPrimary else colors.surface,
        animationSpec = tween(180),
        label = "chipBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) colors.textOnBrand else colors.textPrimary,
        animationSpec = tween(180),
        label = "chipFg"
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .bouncyClickable(haptic = true, onClick = onClick)
            .clip(RoundedCornerShape(WeGrowRadius.full))
            .background(bg)
            .then(
                if (!selected) Modifier.border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.full))
                else Modifier
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = WeGrowTextStyles.TitleS,
            color = fg,
            maxLines = 1
        )
    }
}

/**
 * Story ring item (72x92) with spring bounce
 */
@Composable
fun StoryRingItem(
    label: String,
    seen: Boolean,
    badgeSymbol: String,
    onClick: () -> Unit
) {
    val colors = WeGrowTheme.colors
    Column(
        modifier = Modifier
            .width(74.dp)
            .bouncyClickable(haptic = true, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(
                    width = if (seen) 1.5.dp else 2.2.dp,
                    color = if (seen) colors.border else colors.brandPrimary,
                    shape = CircleShape
                )
                .padding(5.dp)
                .clip(CircleShape)
                .background(colors.surfaceMuted),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeSymbol,
                style = WeGrowTextStyles.TitleL,
                color = if (seen) colors.textSecondary else colors.brandPrimary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = WeGrowTextStyles.CaptionM,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Interactive Price Chart:
 * - Static Path & verticalGradient cached in drawWithCache
 * - Smooth left-to-right draw reveal animation
 * - Tap & horizontal drag scrubbing with spring-interpolated scrubber x-position & glowing point
 */
@Composable
fun InteractivePriceChart(
    points: List<Float>,
    modifier: Modifier = Modifier
) {
    val colors = WeGrowTheme.colors
    var selectedIdx by remember(points) { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }
    val haptics = LocalHapticFeedback.current

    val revealProgress = remember(points) { Animatable(0f) }
    LaunchedEffect(points) {
        revealProgress.snapTo(0f)
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
        )
    }

    val animIdx by animateFloatAsState(
        targetValue = selectedIdx.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scrubberIdx"
    )

    val minVal = remember(points) { (points.minOrNull() ?: 100f) * 0.992f }
    val maxVal = remember(points) { (points.maxOrNull() ?: 200f) * 1.008f }
    val range = (maxVal - minVal).coerceAtLeast(1f)

    Column(modifier = modifier) {
        val activePrice = points.getOrElse(selectedIdx) { points.lastOrNull() ?: 0f }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(WeGrowRadius.sm))
                    .background(colors.brandSoft)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$${String.format("%.2f", activePrice)} · День ${(selectedIdx * 2 + 2).coerceAtMost(30)} ноя",
                    style = WeGrowTextStyles.CaptionStrong,
                    color = if (colors.isDark) colors.brandPressed else colors.textPrimary
                )
            }
            Text(
                text = "$${maxVal.roundToInt()} / $${minVal.roundToInt()}",
                style = WeGrowTextStyles.CaptionM,
                color = colors.textTertiary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.size > 1 && size.width > 0) {
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val newIdx = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                            if (newIdx != selectedIdx) {
                                selectedIdx = newIdx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        if (points.size > 1 && size.width > 0) {
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            val newIdx = (fraction * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                            if (newIdx != selectedIdx) {
                                selectedIdx = newIdx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }
                .drawWithCache {
                    if (points.size < 2) {
                        return@drawWithCache onDrawBehind { }
                    }
                    val w = size.width
                    val h = size.height - 12.dp.toPx()
                    val stepX = w / (points.size - 1).toFloat()

                    val coords = points.mapIndexed { i, v ->
                        val x = i * stepX
                        val y = h - ((v - minVal) / range) * (h - 14.dp.toPx())
                        Offset(x, y)
                    }

                    val fillPath = Path().apply {
                        moveTo(coords.first().x, h)
                        coords.forEach { lineTo(it.x, it.y) }
                        lineTo(coords.last().x, h)
                        close()
                    }
                    val linePath = Path().apply {
                        coords.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    val fillBrush = Brush.verticalGradient(
                        colors = listOf(
                            colors.brandPrimary.copy(alpha = 0.28f),
                            colors.brandPrimary.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = h
                    )
                    val gridEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    val markerEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    val lineStroke = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val gridWidth = 1.dp.toPx()
                    val markerWidth = 1.2.dp.toPx()
                    val glowRadius = 11.dp.toPx()
                    val outerRadius = 6.dp.toPx()
                    val innerRadius = 4.5.dp.toPx()

                    onDrawBehind {
                        // Grid lines
                        for (g in 0..2) {
                            val gy = (h / 2f) * g
                            drawLine(
                                color = colors.border,
                                start = Offset(0f, gy),
                                end = Offset(w, gy),
                                strokeWidth = gridWidth,
                                pathEffect = gridEffect
                            )
                        }

                        // Progressive left-to-right reveal (read animated state inside onDrawBehind!)
                        val clipRight = w * revealProgress.value
                        clipRect(right = clipRight) {
                            drawPath(path = fillPath, brush = fillBrush)
                            drawPath(path = linePath, color = colors.brandPrimary, style = lineStroke)
                        }

                        // Interpolated scrubber point
                        val idxFloat = animIdx.coerceIn(0f, (coords.size - 1).toFloat())
                        val low = idxFloat.toInt().coerceIn(0, coords.lastIndex)
                        val high = (low + 1).coerceAtMost(coords.lastIndex)
                        val t = idxFloat - low
                        val selX = coords[low].x + (coords[high].x - coords[low].x) * t
                        val selY = coords[low].y + (coords[high].y - coords[low].y) * t
                        val selPt = Offset(selX, selY)

                        if (selX <= clipRight + 2f) {
                            drawLine(
                                color = colors.brandPrimary.copy(alpha = 0.55f),
                                start = Offset(selPt.x, 0f),
                                end = Offset(selPt.x, h),
                                strokeWidth = markerWidth,
                                pathEffect = markerEffect
                            )
                            drawCircle(
                                color = colors.brandPrimary.copy(alpha = 0.22f),
                                radius = glowRadius,
                                center = selPt
                            )
                            drawCircle(color = colors.bg, radius = outerRadius, center = selPt)
                            drawCircle(color = colors.brandPrimary, radius = innerRadius, center = selPt)
                        }
                    }
                }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("1", "8", "15", "22", "30").forEach { d ->
                Text(text = d, style = WeGrowTextStyles.CaptionM, color = colors.textTertiary)
            }
        }
    }
}

/**
 * Dual-series Compound Interest Chart (Total vs Invested) cached in drawWithCache with smooth reveal
 */
@Composable
fun CompoundGrowthChart(
    totalSeries: List<Float>,
    investedSeries: List<Float>,
    years: Int,
    modifier: Modifier = Modifier
) {
    val colors = WeGrowTheme.colors
    val maxVal = remember(totalSeries) { (totalSeries.maxOrNull() ?: 1000f).coerceAtLeast(1f) }
    val reveal = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .drawWithCache {
                    if (totalSeries.size < 2 || investedSeries.size < 2) {
                        return@drawWithCache onDrawBehind { }
                    }
                    val w = size.width
                    val h = size.height - 8.dp.toPx()
                    val stepX = w / (totalSeries.size - 1).toFloat()

                    val totalPts = totalSeries.mapIndexed { i, v ->
                        Offset(i * stepX, h - (v / maxVal) * (h - 12.dp.toPx()))
                    }
                    val invPts = investedSeries.mapIndexed { i, v ->
                        Offset(i * stepX, h - (v / maxVal) * (h - 12.dp.toPx()))
                    }
                    val fillPath = Path().apply {
                        moveTo(totalPts.first().x, h)
                        totalPts.forEach { lineTo(it.x, it.y) }
                        lineTo(totalPts.last().x, h)
                        close()
                    }
                    val invPath = Path().apply {
                        invPts.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    val totalPath = Path().apply {
                        totalPts.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    val fillBrush = Brush.verticalGradient(
                        listOf(colors.brandPrimary.copy(alpha = 0.25f), Color.Transparent)
                    )
                    val invStroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    val totalStroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    val endRadius = 5.dp.toPx()
                    val endPt = totalPts.last()

                    onDrawBehind {
                        clipRect(right = w * reveal.value) {
                            drawPath(path = fillPath, brush = fillBrush)
                            drawPath(path = invPath, color = colors.chartSeries2, style = invStroke)
                            drawPath(path = totalPath, color = colors.brandPrimary, style = totalStroke)
                        }
                        if (reveal.value > 0.95f) {
                            drawCircle(color = colors.brandPrimary, radius = endRadius, center = endPt)
                        }
                    }
                }
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = (years / 5f).coerceAtLeast(1f)
            for (i in 0..5) {
                val yLabel = if (i == 0) "0" else "${(i * step).roundToInt()} л"
                Text(text = yLabel, style = WeGrowTextStyles.CaptionM, color = colors.textTertiary)
            }
        }
    }
}

/**
 * Circular Ring Progress with drawWithCache & AnimatedNumberText
 */
@Composable
fun CircularRingProgress(
    fraction: Float,
    size: Dp = 104.dp,
    modifier: Modifier = Modifier
) {
    val colors = WeGrowTheme.colors
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ringProgress"
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val strokeW = 9.dp.toPx()
                    val strokeStyle = Stroke(width = strokeW, cap = StrokeCap.Round)
                    onDrawBehind {
                        drawArc(
                            color = colors.surfaceMuted,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = strokeStyle
                        )
                        drawArc(
                            color = colors.brandPrimary,
                            startAngle = -90f,
                            sweepAngle = 360f * animated,
                            useCenter = false,
                            style = strokeStyle
                        )
                    }
                }
        )
        AnimatedNumberText(
            targetValue = (fraction.coerceIn(0f, 1f) * 100.0),
            formatter = { "${it.roundToInt()}%" },
            style = WeGrowTextStyles.HeadlineM.copy(fontWeight = FontWeight.Medium),
            color = colors.textPrimary
        )
    }
}

/**
 * Single-Canvas 15-bar animated audio waveform preview (60/120fps without 15 Box composables)
 */
@Composable
fun CourseWaveformPreview(
    isPlaying: Boolean,
    barColor: Color,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val phase = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "wavePhase")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        ).value
    } else {
        0f
    }

    val staticHeights = remember {
        floatArrayOf(12f, 24f, 36f, 20f, 44f, 28f, 16f, 32f, 40f, 18f, 26f, 12f, 22f, 34f, 14f)
    }

    Spacer(
        modifier = modifier
            .width(102.dp)
            .height(46.dp)
            .drawWithCache {
                val barWidth = 3.dp.toPx()
                val gap = 4.dp.toPx()
                val corner = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                val maxH = size.height

                onDrawBehind {
                    for (i in staticHeights.indices) {
                        val baseDp = staticHeights[i]
                        val animatedFactor = if (isPlaying) {
                            0.55f + 0.45f * sin(phase + i * 0.65f)
                        } else {
                            1f
                        }
                        val barH = (baseDp.dp.toPx() * animatedFactor).coerceIn(6.dp.toPx(), maxH)
                        val x = i * (barWidth + gap)
                        val y = (maxH - barH) / 2f
                        val c = if (isPlaying && i % 2 == 0) activeColor else barColor
                        drawRoundRect(
                            color = c,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barH),
                            cornerRadius = corner
                        )
                    }
                }
            }
    )
}

/**
 * Floating Pill Navigation Bar with sliding spring pill indicator (zero relayout via graphicsLayer)
 */
@Composable
fun WeGrowFloatingNavBar(
    selectedTab: Int,
    strings: AppStrings,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = WeGrowTheme.colors
    val haptics = LocalHapticFeedback.current
    val items = remember(strings) {
        listOf(
            Pair("⌂", strings.navHome),
            Pair("📈", strings.navMarket),
            Pair("🎓", strings.navCourses),
            Pair("👤", strings.navProfile)
        )
    }

    val animatedIndex by animateFloatAsState(
        targetValue = selectedTab.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "navPillSlide"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .width(304.dp)
                .height(72.dp)
                .shadow(16.dp, RoundedCornerShape(WeGrowRadius.full))
                .clip(RoundedCornerShape(WeGrowRadius.full))
                .background(colors.navBg)
                .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.full))
                .padding(horizontal = 8.dp)
        ) {
            val density = LocalDensity.current
            val slotWidth = maxWidth / items.size
            val pillSize = 58.dp
            val slotWidthPx = with(density) { slotWidth.toPx() }
            val centerOffsetPx = with(density) { ((slotWidth - pillSize) / 2f).toPx() }

            // Sliding pill indicator behind selected icon
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(pillSize)
                    .graphicsLayer {
                        translationX = animatedIndex * slotWidthPx + centerOffsetPx
                    }
                    .clip(CircleShape)
                    .background(colors.brandPrimary)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { idx, (icon, title) ->
                    val isSelected = selectedTab == idx
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) colors.textOnBrand else colors.textSecondary,
                        animationSpec = tween(180),
                        label = "navItemFg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .bouncyClickable(haptic = false) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTab(idx)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = icon, fontSize = 18.sp, color = contentColor)
                            Text(
                                text = title,
                                style = WeGrowTextStyles.Micro,
                                color = contentColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
