package com.memento.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.memento.app.R
import com.memento.app.ui.theme.MementoSpacing

@Composable
fun RatingSelector(
    ratingHalfStars: Int?,
    onRatingChanged: (Int?) -> Unit,
    enabled: Boolean = true,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(MementoSpacing.small)) {
        item {
            FilterChip(
                selected = ratingHalfStars == null,
                onClick = { onRatingChanged(null) },
                enabled = enabled,
                label = { Text(stringResource(R.string.no_rating)) },
            )
        }
        items((1..10).toList()) { value ->
            FilterChip(
                selected = ratingHalfStars == value,
                onClick = { onRatingChanged(value) },
                enabled = enabled,
                label = { Text(formatHalfStars(value)) },
            )
        }
    }
}
