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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.dismissKeyboard
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonTheme3

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
fun ComposerScreen(onCloseComposerClick: () -> Unit, viewModel: ComposerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val state by viewModel.state.collectAsState()
    var recipientsOpen by rememberSaveable { mutableStateOf(false) }
    var focusedField by rememberSaveable { mutableStateOf(FocusedFieldType.TO) }
    val snackbarHostState = remember { ProtonSnackbarHostState() }
    val changeSenderBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    fun onCloseComposer() {
        viewModel.submit(ComposerAction.OnCloseComposer)
        onCloseComposerClick()
    }

    ProtonModalBottomSheetLayout(
        sheetContent = {
            ChangeSenderBottomSheetContent(
                state.senderAddresses,
                { sender -> viewModel.submit(ComposerAction.SenderChanged(sender)) }
            )
        },
        sheetState = changeSenderBottomSheetState
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState(), reverseScrolling = true)
                    .testTag(ComposerTestTags.RootItem)
            ) {
                ComposerTopBar(
                    onCloseComposerClick = {
                        dismissKeyboard(context, view, keyboardController)
                        onCloseComposer()
                    }
                )
                ComposerForm(
                    emailValidator = viewModel::validateEmailAddress,
                    recipientsOpen = recipientsOpen,
                    initialFocus = focusedField,
                    fields = state.fields,
                    actions = buildActions(viewModel, { recipientsOpen = it }, { focusedField = it })
                )
            }

            ProtonSnackbarHost(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .testTag(CommonTestTags.SnackbarHost),
                hostState = snackbarHostState
            )
        }
    }

    ConsumableTextEffect(effect = state.premiumFeatureMessage) { message ->
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.NORM, message = message)
    }

    ConsumableTextEffect(effect = state.error) { error ->
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.ERROR, message = error)
    }

    ConsumableLaunchedEffect(effect = state.changeSenderBottomSheetVisibility) { show ->
        if (show) {
            changeSenderBottomSheetState.show()
        } else {
            changeSenderBottomSheetState.hide()
        }
    }

    BackHandler(true) { onCloseComposer() }
}

private fun buildActions(
    viewModel: ComposerViewModel,
    onToggleRecipients: (Boolean) -> Unit,
    onFocusChanged: (FocusedFieldType) -> Unit
): ComposerFormActions = ComposerFormActions(
    onToggleRecipients = onToggleRecipients,
    onFocusChanged = onFocusChanged,
    onToChanged = { viewModel.submit(ComposerAction.RecipientsToChanged(it)) },
    onCcChanged = { viewModel.submit(ComposerAction.RecipientsCcChanged(it)) },
    onBccChanged = { viewModel.submit(ComposerAction.RecipientsBccChanged(it)) },
    onSubjectChanged = { viewModel.submit(ComposerAction.SubjectChanged(Subject(it))) },
    onBodyChanged = { viewModel.submit(ComposerAction.DraftBodyChanged(DraftBody(it))) },
    onChangeSender = { viewModel.submit(ComposerAction.ChangeSenderRequested) }
)

@Composable
@AdaptivePreviews
private fun MessageDetailScreenPreview() {
    ProtonTheme3 {
        ComposerScreen(onCloseComposerClick = {})
    }
}
