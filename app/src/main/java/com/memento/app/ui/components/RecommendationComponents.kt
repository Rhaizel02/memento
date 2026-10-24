package com.memento.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.memento.app.R
import com.memento.app.domain.recommendation.*
import com.memento.app.ui.theme.MementoSpacing

@Composable
fun RecommendationTierLabel(category: RecommendationCategory) {
    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Text(stringResource(when (category) {
            RecommendationCategory.VERY_AFFINE -> R.string.recommendation_very_affine
            RecommendationCategory.GOOD_BET -> R.string.recommendation_good_bet
            RecommendationCategory.EXPLORATION -> R.string.recommendation_exploration
        }), Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun recommendationReasonText(reason: RecommendationReason): String = when (reason) {
    is RecommendationReason.AnchorWorks -> stringResource(R.string.reason_anchors, reason.titles.joinToString(" y "))
    is RecommendationReason.Genre -> stringResource(R.string.reason_genre, reason.name)
    is RecommendationReason.Creator -> stringResource(R.string.reason_creator, reason.name)
    is RecommendationReason.MediaKind -> stringResource(R.string.reason_type, mediaTypeLabel(reason.type))
    is RecommendationReason.Exploration -> reason.anchorTitle?.let { stringResource(R.string.reason_exploration_anchor, it) }
        ?: stringResource(R.string.reason_exploration)
}

@Composable
fun RecommendationPreview(recommendation: Recommendation, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val work = recommendation.candidate
    Surface(
        modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { RecommendationTierLabel(recommendation.category) }
                Text(listOfNotNull(mediaTypeLabel(work.type), work.releaseYear?.toString()).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PosterArtwork(work.type, work.title, work.posterUrl, Modifier.width(92.dp).height(138.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(work.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    work.creators.firstOrNull()?.let { Text(it, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (work.genres.isNotEmpty()) Text(work.genres.take(2).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    work.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3, overflow = TextOverflow.Ellipsis) }
                }
            }
            recommendation.reasons.firstOrNull()?.let { reason ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(recommendationReasonText(reason), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, stringResource(R.string.archive_open_recommendation),
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
