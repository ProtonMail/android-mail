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

package ch.protonmail.android.mailcommon.presentation.ui.spotlight

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.protonButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Suppress("UseComposableActions")
@Composable
fun SpotlightTooltip(
    modifier: Modifier = Modifier,
    dialogState: SpotlightTooltipState,
    ctaClick: () -> Unit,
    dismiss: () -> Unit,
    displayed: () -> Unit
) {
    val state = dialogState as? SpotlightTooltipState.Shown ?: return
    val orientation = LocalConfiguration.current.orientation
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        return
    }
    var dismissed by remember { mutableStateOf(false) }
    if (dismissed) return
    val model = state.model

    LaunchedEffect(key1 = Unit) {
        displayed()
    }

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    dismiss()
                }
                .padding(
                    bottom = ProtonDimens.SmallSpacing + ProtonDimens.DefaultIconWithPadding
                ),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bgColor = ProtonTheme.colors.backgroundNorm
            Surface(
                shape = RoundedCornerShape(ProtonDimens.LargeCornerRadius),
                color = bgColor,
                modifier = modifier
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
            ) {
                Column(
                    modifier = Modifier
                        .padding(ProtonDimens.DefaultSpacing)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(
                                rememberScrollState()
                            )
                    ) {
                        Image(
                            modifier = Modifier.size(ProtonDimens.DefaultIconWithPadding),
                            painter = painterResource(id = R.drawable.ic_wand),
                            contentDescription = null
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                modifier = Modifier.padding(start = ProtonDimens.SmallSpacing),
                                text = model.title.string(),
                                style = ProtonTheme.typography.body2Medium,
                                color = ProtonTheme.colors.textNorm,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                modifier = Modifier
                                    .padding(start = ProtonDimens.SmallSpacing)
                                    .padding(top = ProtonDimens.SmallSpacing)
                                    .fillMaxWidth(),
                                text = model.message.string(),
                                style = ProtonTheme.typography.body2Regular,
                                color = ProtonTheme.colors.textNorm,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    Spacer(Modifier.height(ProtonDimens.SmallSpacing))
                    ProtonButton(
                        onClick = {
                            dismissed = true
                            dismiss()
                            ctaClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = ButtonDefaults.MinHeight),
                        shape = ProtonTheme.shapes.medium,
                        border = null,
                        elevation = null,
                        colors = ButtonDefaults.protonButtonColors(
                            backgroundColor = ProtonTheme.colors.interactionWeakNorm
                        ),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text(text = model.cta.string(), color = ProtonTheme.colors.textNorm)
                    }
                }
            }
            Canvas(
                modifier = Modifier
                    .width(ProtonDimens.LargeSpacing)
                    .height(ProtonDimens.DefaultSpacing)
            ) {
                val radius = 8f
                val offset = -1f
                drawPath(
                    Path().apply {
                        moveTo(0f, offset)
                        lineTo(size.width, offset)
                        lineTo(size.width / 2f + radius, size.height - radius)
                        quadraticTo(
                            size.width / 2f, size.height,
                            size.width / 2f - radius, size.height - radius
                        )
                        close()
                    },
                    color = bgColor
                )
            }
        }
    }
}

@AdaptivePreviews
@Composable
private fun SpotlightTooltipPreview() {
    ProtonTheme {
        SpotlightTooltip(
            dialogState = SpotlightTooltipState.Shown(
                SpotlightUiModel(
                    title = TextUiModel.Text("Customize toolbar"),
                    message = TextUiModel.Text(
                        "You can now choose and rearrange the actions in this toolbar"
                    ),
                    cta = TextUiModel.Text("Show me")
                )
            ),
            ctaClick = {},
            dismiss = {},
            displayed = {}
        )
    }
}
