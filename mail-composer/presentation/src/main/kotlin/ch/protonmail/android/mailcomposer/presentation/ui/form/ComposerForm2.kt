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

package ch.protonmail.android.mailcomposer.presentation.ui.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.compose.FocusableForm
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailcomposer.domain.model.StyledHtmlQuote
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.FocusedFieldType
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsStateManager
import ch.protonmail.android.mailcomposer.presentation.ui.BodyHtmlQuote
import ch.protonmail.android.mailcomposer.presentation.ui.BodyTextField2
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerTestTags
import ch.protonmail.android.mailcomposer.presentation.ui.PrefixedEmailSelector
import ch.protonmail.android.mailcomposer.presentation.ui.RespondInlineButton
import ch.protonmail.android.mailcomposer.presentation.ui.SubjectTextField2
import ch.protonmail.android.mailcomposer.presentation.viewmodel.RecipientsViewModel
import ch.protonmail.android.uicomponents.keyboardVisibilityAsState
import me.proton.core.compose.theme.ProtonDimens
import timber.log.Timber

@Composable
internal fun ComposerForm2(
    changeFocusToField: Effect<FocusedFieldType>,
    senderEmail: String,
    recipientsStateManager: RecipientsStateManager,
    subjectTextField: TextFieldState,
    bodyTextField: TextFieldState,
    quotedHtmlContent: StyledHtmlQuote?,
    shouldRestrictWebViewHeight: Boolean,
    focusTextBody: Effect<Unit>,
    actions: ComposerForm2.Actions,
    modifier: Modifier = Modifier
) {

    val recipientsViewModel = hiltViewModel<RecipientsViewModel, RecipientsViewModel.Factory> { factory ->
        factory.create(recipientsStateManager)
    }

    val isKeyboardVisible by keyboardVisibilityAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val maxWidthModifier = Modifier.fillMaxWidth()

    var showSubjectAndBody by remember { mutableStateOf(true) }

    FocusableForm(
        fieldList = listOf(
            FocusedFieldType.TO,
            FocusedFieldType.CC,
            FocusedFieldType.BCC,
            FocusedFieldType.SUBJECT,
            FocusedFieldType.BODY
        ),
        initialFocus = FocusedFieldType.TO,
        onFocusedField = {
            Timber.d("Focus changed: onFocusedField: $it")
        }
    ) { fieldFocusRequesters ->

        ConsumableLaunchedEffect(effect = changeFocusToField) {
            fieldFocusRequesters[it]?.requestFocus()
            if (!isKeyboardVisible) {
                keyboardController?.show()
            }
        }

        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            PrefixedEmailSelector(
                prefixStringResource = R.string.from_prefix,
                modifier = maxWidthModifier.testTag(ComposerTestTags.FromSender),
                selectedEmail = senderEmail,
                onChangeSender = actions.onChangeSender
            )
            MailDivider()

            RecipientFields2(
                fieldFocusRequesters = fieldFocusRequesters,
                onToggleSuggestions = { isShown -> showSubjectAndBody = isShown },
                viewModel = recipientsViewModel
            )

            if (showSubjectAndBody) {
                MailDivider()
                SubjectTextField2(
                    textFieldState = subjectTextField,
                    modifier = maxWidthModifier
                        .padding(ProtonDimens.DefaultSpacing)
                        .testTag(ComposerTestTags.Subject)
                        .retainFieldFocusOnConfigurationChange(FocusedFieldType.SUBJECT)
                )
                MailDivider()

                BodyTextField2(
                    textFieldState = bodyTextField,
                    shouldRequestFocus = focusTextBody,
                    modifier = maxWidthModifier
                        .testTag(ComposerTestTags.MessageBody)
                        .retainFieldFocusOnConfigurationChange(FocusedFieldType.BODY)
                )

                if (quotedHtmlContent != null) {
                    RespondInlineButton(actions.onRespondInline)
                    BodyHtmlQuote(
                        value = quotedHtmlContent.value,
                        modifier = maxWidthModifier.testTag(ComposerTestTags.MessageHtmlQuotedBody),
                        shouldRestrictWebViewHeight = shouldRestrictWebViewHeight
                    )
                }
            }
        }
    }
}

internal object ComposerForm2 {
    data class Actions(
        val onChangeSender: () -> Unit,
        val onRespondInline: () -> Unit
    )
}
