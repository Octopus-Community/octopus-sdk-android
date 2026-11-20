package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.lineHeight125

object OctopusOutlinedTextFieldDefaults {
    @Composable
    fun colors(): TextFieldColors {
        val textFieldDefaults = OctopusTextFieldDefaults.colors()
        val textFieldBorderDefaults = OctopusOutlinedTextFieldDefaults.borderColors()
        return OutlinedTextFieldDefaults.colors(
            // Text
            focusedTextColor = textFieldDefaults.focusedTextColor,
            unfocusedTextColor = textFieldDefaults.unfocusedTextColor,
            disabledTextColor = textFieldDefaults.disabledTextColor,
            errorTextColor = textFieldDefaults.errorTextColor,
            // Container
            focusedContainerColor = textFieldDefaults.focusedContainerColor,
            unfocusedContainerColor = textFieldDefaults.unfocusedContainerColor,
            disabledContainerColor = OctopusTheme.colorScheme.gray200,
            errorContainerColor = textFieldDefaults.errorContainerColor,
            // Placeholder
            focusedPlaceholderColor = textFieldDefaults.focusedPlaceholderColor,
            unfocusedPlaceholderColor = textFieldDefaults.unfocusedPlaceholderColor,
            disabledPlaceholderColor = textFieldDefaults.disabledPlaceholderColor,
            errorPlaceholderColor = textFieldDefaults.errorPlaceholderColor,
            // Label
            focusedLabelColor = textFieldDefaults.focusedLabelColor,
            unfocusedLabelColor = textFieldDefaults.unfocusedLabelColor,
            disabledLabelColor = textFieldDefaults.disabledLabelColor,
            errorLabelColor = textFieldDefaults.errorLabelColor,
            disabledLeadingIconColor = textFieldDefaults.disabledLeadingIconColor,
            disabledTrailingIconColor = textFieldDefaults.disabledTrailingIconColor,
            // Border
            focusedBorderColor = textFieldBorderDefaults.focusedColor,
            unfocusedBorderColor = textFieldBorderDefaults.unfocusedColor,
            disabledBorderColor = textFieldBorderDefaults.disabledColor,
            errorBorderColor = textFieldBorderDefaults.errorColor,
        )
    }

    @Composable
    fun borderColors() =
        BorderColors(
            focusedColor = OctopusTheme.colorScheme.primary,
            unfocusedColor = OctopusTheme.colorScheme.gray300,
            disabledColor = OctopusTheme.colorScheme.gray300,
            errorColor = OctopusTheme.colorScheme.alert,
        )

    data class BorderColors(
        val focusedColor: Color,
        val unfocusedColor: Color,
        val disabledColor: Color,
        val errorColor: Color,
    )
}

object OctopusTextFieldDefaults {
    @Composable
    fun colors() =
        TextFieldDefaults.colors(
            // Text
            focusedTextColor = OctopusTheme.colorScheme.gray900,
            unfocusedTextColor = OctopusTheme.colorScheme.gray900,
            disabledTextColor = OctopusTheme.colorScheme.gray500,
            errorTextColor = OctopusTheme.colorScheme.gray700,
            // Container
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            // Placeholder
            unfocusedPlaceholderColor = OctopusTheme.colorScheme.gray500,
            focusedPlaceholderColor = OctopusTheme.colorScheme.gray500,
            disabledPlaceholderColor = OctopusTheme.colorScheme.gray500,
            errorPlaceholderColor = OctopusTheme.colorScheme.gray700,
            // Label
            focusedLabelColor = OctopusTheme.colorScheme.gray700,
            unfocusedLabelColor = OctopusTheme.colorScheme.gray700,
            disabledLabelColor = OctopusTheme.colorScheme.gray500,
            errorLabelColor = OctopusTheme.colorScheme.gray700,
            disabledLeadingIconColor = OctopusTheme.colorScheme.gray500,
            disabledTrailingIconColor = OctopusTheme.colorScheme.gray500,
            // Indicator
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        )
}

@Composable
internal fun OctopusOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = OctopusTheme.typography.body2.lineHeight125,
    label: String? = null,
    placeholder: String? = null,
    prefix: String? = null,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = RoundedCornerShape(8.dp),
    colors: TextFieldColors =
        if (onClick != null && enabled) {
            OctopusOutlinedTextFieldDefaults.colors().let {
                it.copy(
                    disabledTextColor = it.unfocusedTextColor,
                    disabledContainerColor = it.unfocusedContainerColor,
                    disabledPlaceholderColor = it.unfocusedPlaceholderColor,
                    disabledLabelColor = it.unfocusedLabelColor,
                )
            }
        } else {
            OctopusOutlinedTextFieldDefaults.colors()
        },
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        modifier =
            modifier.then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier,
            ),
        value = value,
        onValueChange = onValueChange,
        enabled = enabled && onClick == null,
        readOnly = readOnly || onClick != null,
        textStyle = textStyle,
        label =
            label?.let {
                {
                    Text(
                        text = it,
                        style = OctopusTheme.typography.body2.lineHeight125,
                    )
                }
            },
        placeholder =
            placeholder?.let {
                {
                    Text(
                        text = it,
                        style = OctopusTheme.typography.body2.lineHeight125,
                    )
                }
            },
        prefix =
            prefix?.let {
                {
                    Text(
                        text = it,
                        color = if (enabled) colors.focusedTextColor else colors.disabledTextColor,
                        style = OctopusTheme.typography.body2.lineHeight125,
                    )
                }
            },
        supportingText =
            error?.let {
                {
                    Text(
                        text = it,
                        color = OctopusTheme.colorScheme.alert,
                        style = OctopusTheme.typography.caption2,
                    )
                }
            },
        trailingIcon = trailingIcon,
        isError = error != null,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = colors,
    )
}

@Composable
internal fun OctopusOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = OctopusTheme.typography.body2.lineHeight125,
    label: String? = null,
    placeholder: String? = null,
    prefix: String? = null,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = RoundedCornerShape(8.dp),
    colors: TextFieldColors =
        if (onClick != null && enabled) {
            OctopusOutlinedTextFieldDefaults.colors().let {
                it.copy(
                    disabledTextColor = it.unfocusedTextColor,
                    disabledContainerColor = it.unfocusedContainerColor,
                    disabledPlaceholderColor = it.unfocusedPlaceholderColor,
                    disabledLabelColor = it.unfocusedLabelColor,
                )
            }
        } else {
            OctopusOutlinedTextFieldDefaults.colors()
        },
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        modifier =
            modifier.then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier,
            ),
        value = value,
        onValueChange = onValueChange,
        enabled = enabled && onClick == null,
        readOnly = readOnly || onClick != null,
        textStyle = textStyle,
        label =
            label?.let {
                {
                    Text(
                        text = it,
                        style = OctopusTheme.typography.body2.lineHeight125,
                    )
                }
            },
        placeholder =
            placeholder?.let {
                {
                    Text(
                        text = it,
                        style = OctopusTheme.typography.body2.lineHeight125,
                    )
                }
            },
        prefix =
            prefix?.let {
                {
                    Text(
                        text = it,
                        color = if (enabled) colors.focusedTextColor else colors.disabledTextColor,
                        style = OctopusTheme.typography.body2.lineHeight125,
                    )
                }
            },
        supportingText =
            error?.let {
                {
                    Text(
                        text = it,
                        color = OctopusTheme.colorScheme.alert,
                        style = OctopusTheme.typography.caption2,
                    )
                }
            },
        trailingIcon = trailingIcon,
        isError = error != null,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = colors,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OctopusTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = OctopusTheme.typography.body2,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    error: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = OctopusTextFieldDefaults.colors(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    var isFocused by remember { mutableStateOf(false) }
    BasicTextField(
        modifier =
            modifier.then(
                Modifier.onFocusEvent {
                    isFocused = it.isFocused
                },
            ),
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        textStyle =
            textStyle.copy(
                color =
                    when {
                        isFocused -> colors.focusedTextColor
                        !enabled -> colors.disabledTextColor
                        error -> colors.errorTextColor
                        else -> colors.unfocusedTextColor
                    },
            ),
        cursorBrush = SolidColor(OctopusTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = value.text,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                innerTextField = innerTextField,
                placeholder =
                    placeholder?.let {
                        {
                            Text(
                                text = it,
                                style =
                                    textStyle.copy(
                                        color =
                                            when {
                                                isFocused -> colors.focusedPlaceholderColor
                                                !enabled -> colors.disabledPlaceholderColor
                                                error -> colors.errorPlaceholderColor
                                                else -> colors.focusedPlaceholderColor
                                            },
                                    ),
                            )
                        }
                    },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                prefix = prefix,
                suffix = suffix,
                supportingText = supportingText,
                singleLine = singleLine,
                enabled = enabled,
                isError = error,
                shape = shape,
                colors = colors,
                contentPadding = contentPadding,
            )
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
    )
}

@Preview(showBackground = true)
@Composable
private fun OctopusTextFieldPreview() {
    OctopusTheme {
        OctopusTextField(
            value = remember { TextFieldValue() },
            onValueChange = {},
        )

        OctopusOutlinedTextField(
            value = remember { "" },
            onValueChange = {},
        )
    }
}
