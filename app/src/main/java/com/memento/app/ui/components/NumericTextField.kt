package com.memento.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun NumericTextField(
    value: String,
    onValueChanged: (String) -> Unit,
    @StringRes label: Int,
    decimal: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { incoming ->
            onValueChanged(
                incoming
                    .filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }
                    .replace(',', '.'),
            )
        },
        modifier = modifier,
        label = { Text(stringResource(label)) },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        singleLine = true,
    )
}
