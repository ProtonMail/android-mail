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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.dismissKeyboard
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState.NotSubmittable
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState.Submittable
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonTheme3

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ComposerScreen(onCloseComposerClick: () -> Unit, viewModel: ComposerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val state by viewModel.state.collectAsState()
    var recipientsOpen by rememberSaveable { mutableStateOf(false) }
    var focusedField by rememberSaveable { mutableStateOf(FocusedFieldType.TO) }
    val snackbarHostState = remember { ProtonSnackbarHostState() }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState(), reverseScrolling = true)
            .testTag(ComposerTestTags.RootItem)
    ) {
        ComposerTopBar(
            onCloseComposerClick = {
                dismissKeyboard(context, view, keyboardController)
                onCloseComposerClick()
            }
        )
        when (val currentState = state) {
            is NotSubmittable -> NotSubmittableComposerForm(
                emailValidator = viewModel::validateEmailAddress,
                recipientsOpen = recipientsOpen,
                initialFocus = focusedField,
                state = currentState,
                actions = buildActions(viewModel, { recipientsOpen = it }, { focusedField = it })
            )

            is Submittable -> SubmittableComposerForm(
                emailValidator = viewModel::validateEmailAddress,
                recipientsOpen = recipientsOpen,
                initialFocus = focusedField,
                state = currentState,
                actions = buildActions(viewModel, { recipientsOpen = it }, { focusedField = it })
            )
        }
    }

    ProtonSnackbarHost(snackbarHostState)

    ConsumableTextEffect(effect = state.premiumFeatureMessage) { message ->
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.NORM, message = message)
    }
}

private fun buildActions(
    viewModel: ComposerViewModel,
    onToggleRecipients: (Boolean) -> Unit,
    onFocusChanged: (FocusedFieldType) -> Unit
): ComposerFormActions = ComposerFormActions(
    onSenderChanged = { viewModel.submit(ComposerAction.SenderChanged(it)) },
    onToChanged = { viewModel.submit(ComposerAction.RecipientsToChanged(it)) },
    onCcChanged = { viewModel.submit(ComposerAction.RecipientsCcChanged(it)) },
    onBccChanged = { viewModel.submit(ComposerAction.RecipientsBccChanged(it)) },
    onSubjectChanged = { viewModel.submit(ComposerAction.SubjectChanged(it)) },
    onBodyChanged = { viewModel.submit(ComposerAction.DraftBodyChanged(DraftBody(it))) },
    onToggleRecipients = onToggleRecipients,
    onFocusChanged = onFocusChanged,
    onChangeSender = { viewModel.submit(ComposerAction.OnChangeSender) }
)

@Composable
@AdaptivePreviews
private fun MessageDetailScreenPreview() {
    ProtonTheme3 {
        ComposerScreen(onCloseComposerClick = {})
    }
}
