/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailcommon.presentation.compose

import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun HyperlinkText(
    modifier: Modifier = Modifier,
    @StringRes textResource: Int,
    textStyle: TextStyle = TextStyle.Default,
    linkTextColor: Color = Color.Blue
) {
    val resolver = LocalFontFamilyResolver.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setLinkTextColor(linkTextColor.toArgb())
                setTextColor(textStyle.color.toArgb())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, textStyle.fontSize.value)
                letterSpacing = textStyle.letterSpacing.value
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                typeface = resolver.resolve(textStyle.fontFamily).value as android.graphics.Typeface
                movementMethod = LinkMovementMethod.getInstance()
                setText(textResource)
            }
        }
    )
}
