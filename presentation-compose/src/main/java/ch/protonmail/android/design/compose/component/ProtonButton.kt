/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */
package ch.protonmail.android.design.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme

@Composable
fun ProtonSolidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ButtonColors = ButtonDefaults.protonButtonColors(loading),
    shape: Shape = ProtonTheme.shapes.medium,
    content: @Composable () -> Unit
) {
    ProtonButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonDefaults.MinHeight),
        enabled = enabled,
        loading = loading,
        contained = contained,
        interactionSource = interactionSource,
        elevation = ButtonDefaults.protonElevation(),
        shape = shape,
        border = null,
        colors = colors,
        contentPadding = ButtonDefaults.ContentPadding,
        content = content
    )
}

@Composable
fun ButtonDefaults.protonElevation() = buttonElevation()

@Composable
fun ButtonDefaults.protonButtonColors(
    loading: Boolean = false,
    backgroundColor: Color = ProtonTheme.colors.interactionBrandDefaultNorm,
    contentColor: Color = Color.White,
    disabledBackgroundColor: Color = if (loading) {
        ProtonTheme.colors.interactionBrandDefaultPressed
    } else {
        ProtonTheme.colors.brandMinus40
    },
    disabledContentColor: Color = if (loading) {
        Color.White
    } else {
        Color.White.copy(alpha = 0.5f)
    }
): ButtonColors = buttonColors(
    containerColor = backgroundColor,
    contentColor = contentColor,
    disabledContainerColor = disabledBackgroundColor,
    disabledContentColor = disabledContentColor
)

@Composable
fun ProtonOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ButtonColors = ButtonDefaults.protonOutlinedButtonColors(loading),
    content: @Composable () -> Unit
) {
    ProtonButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonDefaults.MinHeight),
        enabled = enabled,
        loading = loading,
        contained = contained,
        interactionSource = interactionSource,
        elevation = null,
        shape = ProtonTheme.shapes.medium,
        border = ButtonDefaults.protonOutlinedBorder(enabled, loading),
        colors = colors,
        contentPadding = ButtonDefaults.ContentPadding,
        content = content
    )
}

private val outlinedBorderSize = 1.dp

@Composable
fun ButtonDefaults.protonOutlinedBorder(enabled: Boolean = true, loading: Boolean = false) = BorderStroke(
    outlinedBorderSize,
    when {
        loading -> ProtonTheme.colors.interactionBrandDefaultPressed
        !enabled -> ProtonTheme.colors.interactionBrandDefaultDisabled
        else -> ProtonTheme.colors.brandNorm
    }
)

@Composable
fun ButtonDefaults.protonOutlinedButtonColors(
    loading: Boolean = false,
    backgroundColor: Color = ProtonTheme.colors.backgroundNorm,
    contentColor: Color = ProtonTheme.colors.textAccent,
    disabledBackgroundColor: Color = if (loading) {
        ProtonTheme.colors.backgroundSecondary
    } else {
        ProtonTheme.colors.backgroundNorm
    },
    disabledContentColor: Color = if (loading) {
        ProtonTheme.colors.interactionBrandDefaultPressed
    } else {
        ProtonTheme.colors.interactionBrandDefaultDisabled
    }
): ButtonColors = buttonColors(
    containerColor = backgroundColor,
    contentColor = contentColor,
    disabledContainerColor = disabledBackgroundColor,
    disabledContentColor = disabledContentColor
)

@Composable
fun ProtonTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ButtonColors = ButtonDefaults.protonTextButtonColors(loading),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable () -> Unit
) {
    ProtonButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonDefaults.MinHeight),
        enabled = enabled,
        loading = loading,
        contained = contained,
        interactionSource = interactionSource,
        elevation = null,
        shape = ProtonTheme.shapes.small,
        border = null,
        colors = colors,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun ButtonDefaults.protonTextButtonColors(
    loading: Boolean = false,
    backgroundColor: Color = if (loading) {
        ProtonTheme.colors.backgroundSecondary
    } else {
        Color.Transparent
    },
    contentColor: Color = ProtonTheme.colors.textAccent,
    disabledBackgroundColor: Color = if (loading) {
        ProtonTheme.colors.backgroundSecondary
    } else {
        Color.Transparent
    },
    disabledContentColor: Color = if (loading) {
        ProtonTheme.colors.interactionBrandDefaultPressed
    } else {
        ProtonTheme.colors.textDisabled
    }
): ButtonColors = buttonColors(
    containerColor = backgroundColor,
    contentColor = contentColor,
    disabledContainerColor = disabledBackgroundColor,
    disabledContentColor = disabledContentColor
)

@Composable
fun ProtonSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ButtonColors = ButtonDefaults.protonSecondaryButtonColors(loading),
    shape: Shape = ProtonTheme.shapes.medium,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable () -> Unit
) {
    ProtonButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        interactionSource = interactionSource,
        elevation = null,
        shape = shape,
        border = null,
        colors = colors,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun ButtonDefaults.protonSecondaryButtonColors(
    loading: Boolean = false,
    backgroundColor: Color = ProtonTheme.colors.interactionWeakNorm,
    contentColor: Color = ProtonTheme.colors.textNorm,
    disabledBackgroundColor: Color = if (loading) {
        ProtonTheme.colors.interactionWeakPressed
    } else {
        ProtonTheme.colors.interactionWeakDisabled
    },
    disabledContentColor: Color = if (loading) {
        ProtonTheme.colors.textNorm
    } else {
        ProtonTheme.colors.textDisabled
    }
): ButtonColors = buttonColors(
    containerColor = backgroundColor,
    contentColor = contentColor,
    disabledContainerColor = disabledBackgroundColor,
    disabledContentColor = disabledContentColor
)

@Suppress("LongParameterList")
@Composable
fun ProtonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    contained: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation?,
    shape: Shape,
    border: BorderStroke?,
    colors: ButtonColors,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonDefaults.MinHeight),
        enabled = !loading && enabled,
        interactionSource = interactionSource,
        elevation = elevation,
        shape = shape,
        border = border,
        colors = colors,
        contentPadding = contentPadding
    ) {
        ProtonButtonContent(
            loading = loading,
            contained = contained,
            content = content,
            progressColor = colors.disabledContentColor
        )
    }
}

@Composable
private fun ProtonButtonContent(
    loading: Boolean = false,
    contained: Boolean = true,
    progressColor: Color,
    content: @Composable () -> Unit
) {
    if (!contained) {
        Box(Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                content()
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(LoadingIndicatorSize)
                        .align(Alignment.CenterEnd),
                    color = progressColor,
                    strokeWidth = LoadingIndicatorStroke
                )
            }
        }
    } else {
        content()
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(horizontal = ProtonDimens.Spacing.Large)
                    .size(LoadingIndicatorSize),
                color = progressColor,
                strokeWidth = LoadingIndicatorStroke
            )
        }
    }
}

private val LoadingIndicatorSize = 14.dp
private val LoadingIndicatorStroke = 1.dp

@Preview(widthDp = 640, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(widthDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun PreviewProtonSolidButton() {
    PreviewHelper { enabled, contained, loading ->
        ProtonSolidButton(
            enabled = enabled,
            contained = contained,
            loading = loading,
            onClick = { }
        ) {
            Text(text = "Button")
        }
    }
}

@Preview(widthDp = 640, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(widthDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun PreviewProtonOutlinedButton() {
    PreviewHelper { enabled, contained, loading ->
        ProtonOutlinedButton(
            enabled = enabled,
            contained = contained,
            loading = loading,
            onClick = { }
        ) {
            Text(text = "Button")
        }
    }
}

@Preview(widthDp = 640, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(widthDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun PreviewProtonTextButton() {
    PreviewHelper { enabled, contained, loading ->
        ProtonTextButton(
            enabled = enabled,
            contained = contained,
            loading = loading,
            onClick = { }
        ) {
            Text(text = "Button")
        }
    }
}

@Preview(widthDp = 640, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(widthDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun PreviewProtonSecondaryButton() {
    PreviewHelper { enabled, _, loading ->
        ProtonSecondaryButton(
            enabled = enabled,
            loading = loading,
            onClick = { }
        ) {
            Text(text = "Button")
        }
    }
}

@Composable
private inline fun PreviewHelper(
    crossinline button: @Composable (enabled: Boolean, contained: Boolean, loading: Boolean) -> Unit
) {
    ProtonTheme {
        Column(Modifier.padding(10.dp)) {
            PreviewRowHelper(
                enabled = true,
                loading = false,
                button
            )
            PreviewRowHelper(
                enabled = false,
                loading = false,
                button
            )
            PreviewRowHelper(
                enabled = true,
                loading = true,
                button
            )
        }
    }
}

@Composable
private inline fun PreviewRowHelper(
    enabled: Boolean,
    loading: Boolean,
    button: @Composable (enabled: Boolean, contained: Boolean, loading: Boolean) -> Unit
) {
    Row(Modifier.padding(bottom = 20.dp)) {
        Box(Modifier.width(320.dp)) {
            button(enabled, false, loading)
        }
        Box(
            Modifier
                .width(320.dp)
                .padding(start = 20.dp)
        ) {
            button(enabled, true, loading)
        }
    }
}
