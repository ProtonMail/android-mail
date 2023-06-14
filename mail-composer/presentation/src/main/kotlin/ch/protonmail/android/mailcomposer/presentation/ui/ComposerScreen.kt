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

package ch.protonmail.android.mailcomposer.presentation.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.compose.FocusableForm
import ch.protonmail.android.mailcommon.presentation.compose.dismissKeyboard
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailcomposer.presentation.ui.Composer.MessageBodyPortraitMinLines
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel
import ch.protonmail.android.uicomponents.chips.ChipItem
import ch.protonmail.android.uicomponents.chips.ChipsListField
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.defaultNorm

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ComposerScreen(onCloseComposerClick: () -> Unit, viewModel: ComposerViewModel = hiltViewModel()) {
    val maxWidthModifier = Modifier.fillMaxWidth()
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val emailNextKeyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Next,
        keyboardType = KeyboardType.Email
    )
    val recipientsButtonRotation = remember { Animatable(0F) }
    var recipientsOpen by rememberSaveable { mutableStateOf(false) }
    val state = viewModel.state.collectAsState()

    Column {
        ComposerTopBar(
            onCloseComposerClick = {
                dismissKeyboard(context, view, keyboardController)
                onCloseComposerClick()
            }
        )
        FocusableForm(
            fieldList = listOf(
                FocusedFieldType.FROM,
                FocusedFieldType.TO,
                FocusedFieldType.CC,
                FocusedFieldType.BCC,
                FocusedFieldType.SUBJECT,
                FocusedFieldType.BODY
            ),
            initialFocus = FocusedFieldType.TO
        ) { fieldFocusRequesters ->
            Column(
                modifier = maxWidthModifier
                    .testTag(ComposerTestTags.RootItem)
                    .verticalScroll(rememberScrollState(), reverseScrolling = true)
            ) {
                PrefixedEmailTextField(
                    prefixStringResource = R.string.from_prefix,
                    modifier = maxWidthModifier
                        .testTag(ComposerTestTags.FromSender)
                        .retainFieldFocusOnConfigurationChange(FocusedFieldType.FROM)
                )
                MailDivider()
                Row(
                    modifier = maxWidthModifier,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ChipsListField(
                        label = stringResource(id = R.string.to_prefix),
                        value = when (val currentState = state.value) {
                            is ComposerDraftState.Submittable -> {
                                currentState.to.map { it.toChipItem() }
                            }
                        },
                        onListChanged = {
                            viewModel.submit(
                                ComposerAction.RecipientsToChanged(
                                    it.mapNotNull { chipItem -> chipItem.toRecipientUiModel() }
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = ProtonDimens.DefaultSpacing)
                            .testTag(ComposerTestTags.ToRecipient)
                            .retainFieldFocusOnConfigurationChange(FocusedFieldType.TO),
                        keyboardOptions = emailNextKeyboardOptions,
                        focusRequester = fieldFocusRequesters[FocusedFieldType.TO]
                    )
                    Spacer(modifier = Modifier.size(ProtonDimens.DefaultSpacing))
                    IconButton(
                        modifier = Modifier,
                        onClick = { recipientsOpen = !recipientsOpen }
                    ) {
                        Icon(
                            modifier = Modifier.rotate(recipientsButtonRotation.value),
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = stringResource(id = R.string.composer_expand_recipients_button)
                        )
                    }
                }
                AnimatedVisibility(
                    visible = recipientsOpen,
                    modifier = Modifier.animateContentSize()
                ) {
                    Column {
                        MailDivider()
                        ChipsListField(
                            label = stringResource(id = R.string.cc_prefix),
                            value = when (val currentState = state.value) {
                                is ComposerDraftState.Submittable -> {
                                    currentState.cc.map { it.toChipItem() }
                                }
                            },
                            onListChanged = {
                                viewModel.submit(
                                    ComposerAction.RecipientsCcChanged(
                                        it.mapNotNull { chipItem -> chipItem.toRecipientUiModel() }
                                    )
                                )
                            },
                            modifier = Modifier
                                .padding(start = ProtonDimens.DefaultSpacing)
                                .testTag(ComposerTestTags.CcRecipient)
                                .retainFieldFocusOnConfigurationChange(FocusedFieldType.CC),
                            keyboardOptions = emailNextKeyboardOptions,
                            focusRequester = fieldFocusRequesters[FocusedFieldType.CC]
                        )
                        MailDivider()
                        ChipsListField(
                            label = stringResource(id = R.string.bcc_prefix),
                            value = when (val currentState = state.value) {
                                is ComposerDraftState.Submittable -> {
                                    currentState.bcc.map { it.toChipItem() }
                                }
                            },
                            onListChanged = {
                                viewModel.submit(
                                    ComposerAction.RecipientsBccChanged(
                                        it.mapNotNull { chipItem -> chipItem.toRecipientUiModel() }
                                    )
                                )
                            },
                            modifier = Modifier
                                .padding(start = ProtonDimens.DefaultSpacing)
                                .testTag(ComposerTestTags.BccRecipient)
                                .retainFieldFocusOnConfigurationChange(FocusedFieldType.BCC),
                            keyboardOptions = emailNextKeyboardOptions,
                            focusRequester = fieldFocusRequesters[FocusedFieldType.BCC]
                        )
                    }
                }
                MailDivider()
                SubjectTextField(
                    maxWidthModifier
                        .testTag(ComposerTestTags.Subject)
                        .retainFieldFocusOnConfigurationChange(FocusedFieldType.SUBJECT)
                )
                MailDivider()
                BodyTextField(
                    maxWidthModifier
                        .testTag(ComposerTestTags.MessageBody)
                        .retainFieldFocusOnConfigurationChange(FocusedFieldType.BODY)
                )
            }
        }
    }

    LaunchedEffect(key1 = recipientsOpen) {
        recipientsButtonRotation.animateTo(
            if (recipientsOpen) RECIPIENTS_OPEN_ROTATION else RECIPIENTS_CLOSED_ROTATION
        )
    }
}

@Composable
private fun ComposerTopBar(onCloseComposerClick: () -> Unit) {
    ProtonTopAppBar(
        modifier = Modifier.testTag(ComposerTestTags.TopAppBar),
        title = {},
        navigationIcon = {
            IconButton(
                modifier = Modifier.testTag(ComposerTestTags.CloseButton),
                onClick = onCloseComposerClick
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    tint = ProtonTheme.colors.iconNorm,
                    contentDescription = stringResource(R.string.close_composer_content_description)
                )
            }
        },
        actions = {
            IconButton(
                modifier = Modifier.testTag(ComposerTestTags.SendButton),
                onClick = {},
                enabled = false
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_proton_paper_plane),
                    tint = ProtonTheme.colors.iconDisabled,
                    contentDescription = stringResource(R.string.send_message_content_description)
                )
            }
        }
    )
}

@Composable
private fun PrefixedEmailTextField(@StringRes prefixStringResource: Int, modifier: Modifier = Modifier) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier,
        textStyle = ProtonTheme.typography.defaultNorm,
        prefix = {
            Row {
                Text(
                    modifier = Modifier.testTag(ComposerTestTags.FieldPrefix),
                    text = stringResource(prefixStringResource),
                    color = ProtonTheme.colors.textWeak,
                    style = ProtonTheme.typography.defaultNorm
                )
                Spacer(modifier = Modifier.size(ProtonDimens.ExtraSmallSpacing))
            }
        },
        colors = TextFieldDefaults.composerTextFieldColors(),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Email
        )
    )
}

@Composable
private fun SubjectTextField(modifier: Modifier = Modifier) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier,
        textStyle = ProtonTheme.typography.defaultNorm,
        colors = TextFieldDefaults.composerTextFieldColors(),
        maxLines = 3,
        placeholder = {
            Text(
                modifier = Modifier.testTag(ComposerTestTags.SubjectPlaceholder),
                text = stringResource(R.string.subject_placeholder),
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.defaultNorm
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}

@Composable
private fun BodyTextField(modifier: Modifier = Modifier) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val screenOrientation = LocalConfiguration.current.orientation
    val bodyMinLines = if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) MessageBodyPortraitMinLines else 1

    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier.fillMaxSize(),
        textStyle = ProtonTheme.typography.defaultNorm,
        minLines = bodyMinLines,
        colors = TextFieldDefaults.composerTextFieldColors(),
        placeholder = {
            Text(
                modifier = Modifier.testTag(ComposerTestTags.MessageBodyPlaceholder),
                text = stringResource(R.string.compose_message_placeholder),
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.defaultNorm
            )
        }
    )
}

@Composable
private fun TextFieldDefaults.composerTextFieldColors(): TextFieldColors = colors(
    focusedTextColor = ProtonTheme.colors.textNorm,
    focusedContainerColor = ProtonTheme.colors.backgroundNorm,
    unfocusedContainerColor = ProtonTheme.colors.backgroundNorm,
    focusedLabelColor = ProtonTheme.colors.textNorm,
    unfocusedLabelColor = ProtonTheme.colors.textHint,
    disabledLabelColor = ProtonTheme.colors.textDisabled,
    errorLabelColor = ProtonTheme.colors.notificationError,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

@Composable
@AdaptivePreviews
private fun MessageDetailScreenPreview() {
    ProtonTheme3 {
        ComposerScreen(onCloseComposerClick = {})
    }
}

private object Composer {

    const val MessageBodyPortraitMinLines = 6
}

private enum class FocusedFieldType {
    FROM,
    TO,
    CC,
    BCC,
    SUBJECT,
    BODY
}

object ComposerTestTags {

    const val RootItem = "ComposerScreenRootItem"
    const val TopAppBar = "ComposerTopAppBar"
    const val FieldPrefix = "FieldPrefix"
    const val FromSender = "FromTextField"
    const val ToRecipient = "ToTextField"
    const val CcRecipient = "CcTextField"
    const val BccRecipient = "BccTextField"
    const val Subject = "Subject"
    const val SubjectPlaceholder = "SubjectPlaceholder"
    const val MessageBody = "MessageBody"
    const val MessageBodyPlaceholder = "MessageBodyPlaceholder"
    const val CloseButton = "CloseButton"
    const val SendButton = "SendButton"
}

private const val RECIPIENTS_OPEN_ROTATION = 180f
private const val RECIPIENTS_CLOSED_ROTATION = 0F

private fun ChipItem.toRecipientUiModel(): RecipientUiModel? = when (this) {
    is ChipItem.Counter -> null
    is ChipItem.Invalid -> RecipientUiModel.Invalid(value)
    is ChipItem.Valid -> RecipientUiModel.Valid(value)
}

private fun RecipientUiModel.toChipItem(): ChipItem = when (this) {
    is RecipientUiModel.Invalid -> ChipItem.Invalid(address)
    is RecipientUiModel.Valid -> ChipItem.Valid(address)
}
