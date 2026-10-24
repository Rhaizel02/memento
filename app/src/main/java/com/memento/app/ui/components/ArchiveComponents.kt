package com.memento.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import com.memento.app.R
import com.memento.app.ui.theme.MementoSpacing
import com.memento.app.ui.theme.EditorialQuoteStyle
import com.memento.app.ui.theme.mementoThemeInfo

@Composable
fun MementoScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null,
    compact: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = MementoSpacing.normal, vertical = if (compact) 4.dp else MementoSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            onBack?.let {
                IconButton(onClick = it) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
            }
            leading?.let { it(); Spacer(Modifier.width(10.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                    maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (compact && subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            actions()
            onSettings?.let {
                IconButton(onClick = it) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) }
            }
        }
        subtitle?.takeUnless { compact }?.let {
            Text(it, Modifier.padding(horizontal = MementoSpacing.normal), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EditorialSectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        action?.invoke()
    }
}

@Composable
fun MementoPrimaryButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) = Button(onClick, modifier.heightIn(min = 48.dp), enabled, shape = shape, colors = colors,
    contentPadding = contentPadding, content = content)

@Composable
fun MementoSecondaryButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable RowScope.() -> Unit,
) = OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled, shape = shape, colors = colors,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), content = content)

@Composable
fun MementoFilterChip(
    selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) = FilterChip(
    selected, onClick, label, modifier.heightIn(min = 48.dp), enabled,
    leadingIcon = leadingIcon, shape = CircleShape,
    colors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        labelColor = MaterialTheme.colorScheme.onSurface,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
    ),
    border = null,
)

@Composable
fun MementoTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, readOnly: Boolean = false,
    labelText: String? = null,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1, visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (labelText != null) Text(labelText, style = MaterialTheme.typography.labelLarge)
        else label?.let {
            androidx.compose.runtime.CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) { it() }
        }
        OutlinedTextField(
            value, onValueChange, Modifier.fillMaxWidth().semantics {
                if (labelText != null) contentDescription = labelText
            }, enabled, readOnly,
            textStyle = MaterialTheme.typography.bodyLarge, placeholder = placeholder,
            leadingIcon = leadingIcon, trailingIcon = trailingIcon, supportingText = supportingText,
            isError = isError, keyboardOptions = keyboardOptions, singleLine = singleLine,
            maxLines = maxLines, minLines = minLines, shape = MaterialTheme.shapes.medium,
            visualTransformation = visualTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

@Composable
fun EditorialQuote(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(MementoSpacing.normal), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("“", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text(text, style = EditorialQuoteStyle)
        }
    }
}

@Composable
fun EditorialGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        content()
    }
}

@Composable
fun ArchiveLoading(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(MementoSpacing.normal), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) { index ->
            Surface(Modifier.fillMaxWidth(if (index == 0) 0.65f else 1f).height(if (index == 1) 100.dp else 20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {}
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MementoBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest, modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = if (MaterialTheme.mementoThemeInfo.isDark) MaterialTheme.colorScheme.surfaceContainer
            else MaterialTheme.colorScheme.surfaceContainerLowest,
        content = content,
    )
}

@Composable
fun ProgressIncrements(values: List<Double>, current: String, onChanged: (String) -> Unit, max: Double? = null) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { increment ->
            val label = java.text.DecimalFormat("0.#").format(increment)
            MementoSecondaryButton(
                onClick = {
                    val next = ((current.toDoubleOrNull() ?: 0.0) + increment).let { if (max != null) it.coerceAtMost(max) else it }
                    onChanged(if (next % 1.0 == 0.0) next.toLong().toString() else next.toString())
                },
                modifier = Modifier.semantics { contentDescription = "Añadir $label" },
            ) { Text("+$label") }
        }
    }
}
