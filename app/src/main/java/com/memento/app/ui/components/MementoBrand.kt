package com.memento.app.ui.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.memento.app.R

/** Decorative beside the accessible name. Tint follows the app theme, not OS night resources. */
@Composable
fun MementoBrandMark(modifier: Modifier = Modifier.width(44.dp)) {
    Icon(
        painter = painterResource(R.drawable.ic_memento_symbol),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier.aspectRatio(1060f / 654f),
    )
}
