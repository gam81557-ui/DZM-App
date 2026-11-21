package com.dzm.manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dzm.manager.ui.theme.AmberSignal
import com.dzm.manager.ui.theme.CrimsonAlert
import com.dzm.manager.ui.theme.TextPrimary
import com.dzm.manager.ui.theme.TextSecondary

@Immutable
sealed interface DescriptionLine {
    data class InfoLine(
        val key: String,
        val value: String,
        val separator: String = ":"
    ) : DescriptionLine

    data class SectionHeader(val title: String) : DescriptionLine
    data class FeatureLine(val text: String, val showVipBadge: Boolean = false) : DescriptionLine
    data class StatusLine(val text: String) : DescriptionLine
    data class PlainLine(val text: String) : DescriptionLine
    data object SpacerLine : DescriptionLine
}

private val BULLET_PREFIXES = listOf("- ", "• ", "* ", "+ ", "✓ ", "✔ ", "▸ ", "> ")
private val STATUS_KEY_PREFIXES = listOf("status:", "safety:", "note:", "warning:", "notice:")
private val STATUS_KEYWORDS = listOf(
    "safe",
    "ban",
    "main account",
    "online update",
    "termination",
    "observation",
    "antib",
    "tested",
    "undetected",
    "risk"
)

private val DetailBoxBg = Color(0xFF252427)
private val DetailBoxBorder = Color(0xFF6B5335)
private val VipGoldBorder = Color(0xFFD4A145)
private val VipGoldFillTop = Color(0xFF473416)
private val VipGoldFillBottom = Color(0xFF261B0A)
private val VipGoldBrush = Brush.verticalGradient(
    colors = listOf(VipGoldFillTop, VipGoldFillBottom)
)
private val VipGoldText = Color(0xFFF6C85F)
private val ToggleBulletBlue = Color(0xFF2186FF)
private val VerifiedSkyBlue = Color(0xFF38BDF8)
private val DashedDividerColor = Color(0xFF9EA4B0)
private val QuoteAmber = Color(0xFFD99B38)

internal fun parseStructuredDescription(rawDescription: String): List<DescriptionLine> {
    val normalized = rawDescription
        .replace("\r\n", "\n")
        .replace("\\n", "\n")

    val rawLines = normalized.split('\n').map { it.trim() }
    val firstNonBlank = rawLines.indexOfFirst { it.isNotEmpty() }
    if (firstNonBlank == -1) return emptyList()
    val lastNonBlank = rawLines.indexOfLast { it.isNotEmpty() }
    val lines = rawLines.subList(firstNonBlank, lastNonBlank + 1)

    val result = mutableListOf<DescriptionLine>()
    var inFeaturesSection = false
    var seenFeaturesHeader = false
    var seenAnyFeatureInSection = false

    for (line in lines) {
        if (line.isEmpty()) {
            inFeaturesSection = false
            if (result.lastOrNull() != DescriptionLine.SpacerLine) {
                result.add(DescriptionLine.SpacerLine)
            }
            continue
        }

        val headerCandidate = line
            .removePrefix("→A")
            .removePrefix("->")
            .trim()
        val lowerHeader = headerCandidate.lowercase()
            .removeSuffix("vip")
            .removeSuffix("[vip]")
            .trim()

        if (lowerHeader == "features:" || lowerHeader == "features :" || lowerHeader == "features") {
            inFeaturesSection = true
            seenFeaturesHeader = true
            seenAnyFeatureInSection = false
            result.add(DescriptionLine.SectionHeader("Features :"))
            continue
        }

        val lower = line.lowercase()
        val bulletMatched = BULLET_PREFIXES.firstOrNull { line.startsWith(it) }
        if (inFeaturesSection) {
            val isExplicitStatusKey = STATUS_KEY_PREFIXES.any { lower.startsWith(it) }
            val looksLikeTrailingStatus = bulletMatched == null &&
                seenAnyFeatureInSection &&
                STATUS_KEYWORDS.any { it in lower }

            if (isExplicitStatusKey || looksLikeTrailingStatus) {
                inFeaturesSection = false
                result.add(DescriptionLine.StatusLine(line))
            } else {
                val cleanFeature = if (bulletMatched != null) {
                    line.removePrefix(bulletMatched).trim()
                } else {
                    line
                }
                if (cleanFeature.isNotEmpty()) {
                    seenAnyFeatureInSection = true
                    result.add(
                        DescriptionLine.FeatureLine(
                            text = cleanFeature,
                            showVipBadge = shouldShowFeatureVipBadge(cleanFeature)
                        )
                    )
                }
            }
            continue
        }

        if (seenFeaturesHeader) {
            result.add(DescriptionLine.StatusLine(line))
            continue
        }

        if (bulletMatched != null) {
            val cleanFeature = line.removePrefix(bulletMatched).trim()
            if (cleanFeature.isNotEmpty()) {
                result.add(
                    DescriptionLine.FeatureLine(
                        text = cleanFeature,
                        showVipBadge = shouldShowFeatureVipBadge(cleanFeature)
                    )
                )
            }
            continue
        }

        val dashColonIndex = line.indexOf(":-")
        val colonIndex = line.indexOf(':')
        if (dashColonIndex in 1..24 && !line.contains("://")) {
            val key = line.substring(0, dashColonIndex).trim()
            val value = line.substring(dashColonIndex + 2).trim()
            if (key.isNotEmpty() && value.isNotEmpty()) {
                if (STATUS_KEY_PREFIXES.any { lower.startsWith(it) }) {
                    result.add(DescriptionLine.StatusLine(line))
                } else {
                    result.add(DescriptionLine.InfoLine(key = key, value = value, separator = ":-"))
                }
                continue
            }
        } else if (colonIndex in 1..24 && !line.contains("://")) {
            val key = line.substring(0, colonIndex).trim()
            val value = line.substring(colonIndex + 1).trim()
            if (key.isNotEmpty() && value.isNotEmpty()) {
                if (STATUS_KEY_PREFIXES.any { lower.startsWith(it) }) {
                    result.add(DescriptionLine.StatusLine(line))
                } else {
                    result.add(DescriptionLine.InfoLine(key = key, value = value, separator = ":"))
                }
                continue
            }
        }

        if (STATUS_KEYWORDS.any { it in lower }) {
            result.add(DescriptionLine.StatusLine(line))
        } else {
            result.add(DescriptionLine.PlainLine(line))
        }
    }

    return result
}

private fun shouldShowFeatureVipBadge(text: String): Boolean {
    val lower = text.lowercase()
    return "wall hack customize" in lower ||
        "wallhack customize" in lower ||
        "[vip]" in lower
}

private data class DescriptionSections(
    val topInfoLines: List<DescriptionLine>,
    val featuresHeader: DescriptionLine.SectionHeader?,
    val featureLines: List<DescriptionLine.FeatureLine>,
    val statusLines: List<DescriptionLine.StatusLine>
)

private fun groupDescriptionSections(lines: List<DescriptionLine>): DescriptionSections {
    val topInfo = mutableListOf<DescriptionLine>()
    var header: DescriptionLine.SectionHeader? = null
    val features = mutableListOf<DescriptionLine.FeatureLine>()
    val statuses = mutableListOf<DescriptionLine.StatusLine>()

    for (line in lines) {
        when (line) {
            is DescriptionLine.InfoLine -> {
                if (header == null && features.isEmpty() && statuses.isEmpty()) {
                    topInfo.add(line)
                } else {
                    val sepText = if (line.separator == ":-") " :- " else ": "
                    statuses.add(DescriptionLine.StatusLine("${line.key}$sepText${line.value}"))
                }
            }
            is DescriptionLine.PlainLine -> {
                if (header == null && features.isEmpty() && statuses.isEmpty()) {
                    topInfo.add(line)
                } else {
                    statuses.add(DescriptionLine.StatusLine(line.text))
                }
            }
            is DescriptionLine.SectionHeader -> {
                header = line
            }
            is DescriptionLine.FeatureLine -> {
                features.add(line)
            }
            is DescriptionLine.StatusLine -> {
                statuses.add(line)
            }
            DescriptionLine.SpacerLine -> Unit
        }
    }

    return DescriptionSections(
        topInfoLines = topInfo,
        featuresHeader = header,
        featureLines = features,
        statusLines = statuses
    )
}

@Composable
fun StructuredGreenDescription(
    description: String,
    itemId: String,
    modifier: Modifier = Modifier
) {
    val parsedLines = remember(description) { parseStructuredDescription(description) }
    if (parsedLines.isEmpty()) return

    val sections = remember(parsedLines) { groupDescriptionSections(parsedLines) }
    val hasTopSection = sections.topInfoLines.isNotEmpty()
    val hasFeatureSection = sections.featuresHeader != null || sections.featureLines.isNotEmpty()
    val hasStatusSection = sections.statusLines.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("file_description_$itemId"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (hasTopSection) {
            DetailSectionCard(
                modifier = Modifier.testTag("description_info_box_$itemId")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sections.topInfoLines.forEach { entry ->
                            when (entry) {
                                is DescriptionLine.InfoLine -> {
                                    val sepText = if (entry.separator == ":-") " :- " else ": "
                                    Text(
                                        text = "${entry.key}$sepText${entry.value}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                is DescriptionLine.PlainLine -> {
                                    Text(
                                        text = entry.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                                else -> Unit
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberSignal.copy(alpha = 0.14f))
                            .border(1.dp, DetailBoxBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SportsEsports,
                            contentDescription = null,
                            tint = VipGoldText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (hasTopSection && (hasFeatureSection || hasStatusSection)) {
            DashedSectionDivider()
        }

        if (hasFeatureSection) {
            DetailSectionCard(
                modifier = Modifier.testTag("description_features_box_$itemId")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "→A",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Features",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline,
                                color = TextPrimary
                            )
                            Text(
                                text = ":",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            VipPillBadge()
                        }

                        QuoteAccentMark()
                    }

                    sections.featureLines.forEach { feature ->
                        FeatureBulletRow(feature = feature)
                    }
                }
            }
        }

        if (hasStatusSection) {
            DetailSectionCard(
                modifier = Modifier.testTag("description_status_box_$itemId")
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    QuoteAccentMark(
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        sections.statusLines.forEach { status ->
                            StatusLineRow(text = status.text)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DetailBoxBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DetailBoxBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun DashedSectionDivider(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val strokePx = remember(density) { with(density) { 4.dp.toPx() } }
    val dashEffect = remember(density) {
        with(density) {
            PathEffect.dashPathEffect(floatArrayOf(15.dp.toPx(), 10.dp.toPx()), 0f)
        }
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .padding(horizontal = 2.dp)
    ) {
        drawLine(
            color = DashedDividerColor,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
            pathEffect = dashEffect
        )
    }
}

@Composable
private fun QuoteAccentMark(modifier: Modifier = Modifier) {
    Text(
        text = "❞",
        color = QuoteAmber,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
private fun VipPillBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(brush = VipGoldBrush)
            .border(1.dp, VipGoldBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "VIP",
            color = VipGoldText,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeatureBulletRow(feature: DescriptionLine.FeatureLine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // Blue capsule toggle-style bullet matching reference screenshot
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .width(22.dp)
                .height(13.dp)
                .clip(RoundedCornerShape(50))
                .background(ToggleBulletBlue)
                .padding(2.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = feature.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            if (feature.showVipBadge) {
                VipPillBadge(modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusLineRow(text: String) {
    val lower = text.lowercase()
    val showVerifiedBadge = "online update" in lower || "verified" in lower
    val showHundredBadge = "ban" in lower || "termination" in lower || "observation" in lower

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.AccessTimeFilled,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp)
        )

        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            if (showVerifiedBadge && !text.contains("✅") && !text.contains("☑")) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = VerifiedSkyBlue,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(16.dp)
                )
            }

            if (showHundredBadge && !text.contains("💯")) {
                HundredBadge()
            }
        }
    }
}

@Composable
private fun HundredBadge(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(top = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(CrimsonAlert.copy(alpha = 0.2f))
            .border(1.dp, CrimsonAlert.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "100",
            color = CrimsonAlert,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(1.5.dp)
                .background(CrimsonAlert)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(1.dp)
                .background(CrimsonAlert.copy(alpha = 0.85f))
        )
    }
}
