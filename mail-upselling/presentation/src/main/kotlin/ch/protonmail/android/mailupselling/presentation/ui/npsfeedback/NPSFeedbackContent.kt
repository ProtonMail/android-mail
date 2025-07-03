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

package ch.protonmail.android.mailupselling.presentation.ui.npsfeedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.NPSFeedbackUIState
import ch.protonmail.android.mailupselling.presentation.model.NPSFeedbackViewEvent
import me.proton.core.compose.component.ProtonButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.component.protonButtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun NPSFeedbackContent(
    state: NPSFeedbackUIState,
    onEvent: (NPSFeedbackViewEvent) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val inputFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.selection) {
        if (state.selection != null) {
            inputFocusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nps_feedback_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCloseClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null
                        )
                    }
                }
            )
        },
        bottomBar = {
            ProtonButton(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(min = ButtonDefaults.MinHeight)
                    .padding(ProtonDimens.DefaultSpacing),
                elevation = null,
                shape = ProtonTheme.shapes.medium,
                border = null,
                colors = ButtonDefaults.protonButtonColors(
                    disabledBackgroundColor = ProtonTheme.colors.interactionDisabled
                ),
                onClick = {
                    onEvent(NPSFeedbackViewEvent.SubmitClicked)
                },
                enabled = state.submitEnabled
            ) {
                Text(text = stringResource(R.string.nps_cta))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(ProtonDimens.DefaultSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_illustration_nps),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(ProtonDimens.LargeSpacing))
            Text(
                text = stringResource(R.string.nps_feedback_headline),
                style = ProtonTheme.typography.hero.copy(fontSize = 24.sp),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(ProtonDimens.LargeSpacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(R.string.nps_label_left), textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.nps_label_right), textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
            val isNarrowScreen = LocalConfiguration.current.screenWidthDp <= MailDimens.NarrowScreenWidth.value
            FlowRow(
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.Center,
                maxItemsInEachRow = if (isNarrowScreen) 4 else 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ProtonDimens.SmallSpacing)
            ) {
                for (i in 0..NPSFeedbackUIState.MAX_VALUE) {
                    ValueOptionButton(
                        value = i,
                        isSelected = state.selection == i,
                        onClick = { onEvent(NPSFeedbackViewEvent.OptionSelected(i)) },
                        modifier = Modifier.padding(ProtonDimens.ExtraSmallSpacing)
                    )
                }
            }

            AnimatedVisibility(
                visible = state.selection != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ProtonDimens.LargeSpacing)
                    .focusRequester(inputFocusRequester)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.nps_why_description),
                        style = ProtonTheme.typography.captionMedium
                    )
                    Spacer(Modifier.height(ProtonDimens.SmallSpacing))
                    OutlinedTextField(
                        value = state.feedbackText,
                        onValueChange = { onEvent(NPSFeedbackViewEvent.FeedbackChanged(it)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = ProtonTheme.colors.interactionNorm,
                            unfocusedBorderColor = ProtonTheme.colors.interactionWeakNorm,
                            cursorColor = ProtonTheme.colors.interactionNorm
                        )
                    )
                    Spacer(Modifier.height(ProtonDimens.SmallSpacing))
                    Text(
                        text = stringResource(R.string.nps_optional),
                        style = ProtonTheme.typography.captionRegular,
                        color = ProtonTheme.colors.textWeak
                    )
                }
            }
        }
    }
}

@Composable
private fun ValueOptionButton(
    value: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(ProtonDimens.LargeCornerRadius),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) {
                ProtonTheme.colors.interactionNorm
            } else {
                ProtonTheme.colors.interactionWeakNorm
            }
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier.size(ProtonDimens.DefaultIconWithPadding),
        elevation = null
    ) {
        Text(
            text = "$value",
            color = if (isSelected) ProtonTheme.colors.textInverted
            else ProtonTheme.colors.textNorm
        )
    }
}

@AdaptivePreviews
@Composable
private fun NPSFeedbackContentPreview() {
    ProtonTheme {
        NPSFeedbackContent(
            state = NPSFeedbackUIState(
                selection = null,
                feedbackText = TextFieldValue(""),
                showSuccess = Effect.empty(),
                submitted = false,
                submitEnabled = false
            ),
            onEvent = {},
            onCloseClick = {}
        )
    }
}

@AdaptivePreviews
@Composable
private fun NPSFeedbackContentPreview_Filled() {
    ProtonTheme {
        NPSFeedbackContent(
            state = NPSFeedbackUIState(
                selection = 10,
                feedbackText = TextFieldValue("Text"),
                showSuccess = Effect.empty(),
                submitted = false,
                submitEnabled = true
            ),
            onEvent = {},
            onCloseClick = {}
        )
    }
}
