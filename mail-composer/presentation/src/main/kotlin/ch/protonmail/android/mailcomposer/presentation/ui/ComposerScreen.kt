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

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.dismissKeyboard
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.Subject
import ch.protonmail.android.mailcomposer.domain.usecase.StoreAttachments
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel
import ch.protonmail.android.mailmessage.presentation.ui.AttachmentFooter
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonTheme3
import timber.log.Timber

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Suppress("UseComposableActions")
@Composable
fun ComposerScreen(actions: ComposerScreen.Actions, viewModel: ComposerViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val state by viewModel.state.collectAsState()
    var recipientsOpen by rememberSaveable { mutableStateOf(false) }
    var focusedField by rememberSaveable { mutableStateOf(FocusedFieldType.TO) }
    val snackbarHostState = remember { ProtonSnackbarHostState() }
    val bottomSheetType = rememberSaveable { mutableStateOf(BottomSheetType.AddAttachments) }
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val attachmentSizeDialogState = remember { mutableStateOf(false) }
    val sendingErrorDialogState = remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            viewModel.submit(ComposerAction.AttachmentsAdded(uris))
        }
    )

    ProtonModalBottomSheetLayout(
        sheetContent = {
            when (bottomSheetType.value) {
                BottomSheetType.AddAttachments -> AddAttachmentsBottomSheetContent(
                    onImportFromSelected = {
                        viewModel.submit(ComposerAction.OnBottomSheetOptionSelected)
                        imagePicker.launch("*/*")
                    }
                )

                BottomSheetType.ChangeSender -> ChangeSenderBottomSheetContent(
                    state.senderAddresses,
                    { sender -> viewModel.submit(ComposerAction.SenderChanged(sender)) }
                )
            }
        },
        sheetState = bottomSheetState
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState(), reverseScrolling = true)
                    .testTag(ComposerTestTags.RootItem)
            ) {
                ComposerTopBar(
                    isAddAttachmentsButtonVisible = state.isAddAttachmentsButtonVisible,
                    onAddAttachmentsClick = {
                        bottomSheetType.value = BottomSheetType.AddAttachments
                        viewModel.submit(ComposerAction.OnAddAttachments)
                    },
                    onCloseComposerClick = {
                        viewModel.submit(ComposerAction.OnCloseComposer)
                    },
                    onSendMessageComposerClick = {
                        viewModel.submit(ComposerAction.OnSendMessage)
                    },
                    isSendMessageButtonEnabled = state.isSubmittable
                )
                if (!state.isLoading) {
                    // Not showing the form till we're done loading ensure it does receive the
                    // right "initial values" from state when displayed
                    ComposerForm(
                        emailValidator = viewModel::validateEmailAddress,
                        recipientsOpen = recipientsOpen,
                        initialFocus = focusedField,
                        fields = state.fields,
                        replaceDraftBody = state.replaceDraftBody,
                        shouldForceBodyTextFocus = state.focusTextBody,
                        actions = buildActions(
                            viewModel,
                            { recipientsOpen = it },
                            { focusedField = it },
                            { bottomSheetType.value = it }
                        )
                    )
                    if (state.attachments.attachments.isNotEmpty()) {
                        AttachmentFooter(
                            messageBodyAttachmentsUiModel = state.attachments,
                            actions = AttachmentFooter.Actions(
                                onShowAllAttachments = { Timber.d("On show all attachments clicked") },
                                onAttachmentClicked = { Timber.d("On attachment clicked: $it") },
                                onAttachmentDeleteClicked = { viewModel.submit(ComposerAction.RemoveAttachment(it)) }
                            )
                        )
                    }
                }
            }
            ProtonSnackbarHost(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .testTag(CommonTestTags.SnackbarHost),
                hostState = snackbarHostState
            )

            if (attachmentSizeDialogState.value) {
                ProtonAlertDialog(
                    onDismissRequest = { attachmentSizeDialogState.value = false },
                    confirmButton = {
                        ProtonAlertDialogButton(R.string.composer_attachment_size_exceeded_dialog_confirm_button) {
                            attachmentSizeDialogState.value = false
                        }
                    },
                    title = stringResource(id = R.string.composer_attachment_size_exceeded_dialog_title),
                    text = {
                        ProtonAlertDialogText(
                            stringResource(
                                id = R.string.composer_attachment_size_exceeded_dialog_message,
                                Formatter.formatShortFileSize(
                                    LocalContext.current,
                                    StoreAttachments.MAX_ATTACHMENTS_SIZE
                                )
                            )
                        )
                    }
                )
            }

            if (sendingErrorDialogState.value != null) {
                SendingErrorDialog(
                    errorMessage = sendingErrorDialogState.value.toString(),
                    onDismissClicked = {
                        sendingErrorDialogState.value = null
                        viewModel.clearSendingError()
                    }
                )
            }

            if (state.isLoading) {
                @Suppress("MagicNumber")
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(.5f)
                ) { ProtonCenteredProgress() }
            }
        }
    }

    ConsumableTextEffect(effect = state.premiumFeatureMessage) { message ->
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.NORM, message = message)
    }

    ConsumableTextEffect(effect = state.error) { error ->
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.ERROR, message = error)
    }

    ConsumableTextEffect(effect = state.warning) { warning ->
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.WARNING, message = warning)
    }

    val errorAttachmentReEncryption = stringResource(id = R.string.composer_attachment_reencryption_failed_message)
    ConsumableLaunchedEffect(effect = state.attachmentsReEncryptionFailed) {
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.ERROR, message = errorAttachmentReEncryption)
    }

    ConsumableLaunchedEffect(effect = state.changeBottomSheetVisibility) { show ->
        if (show) {
            dismissKeyboard(context, view, keyboardController)
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    ConsumableLaunchedEffect(effect = state.closeComposer) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
    }

    ConsumableLaunchedEffect(effect = state.closeComposerWithDraftSaved) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
        actions.showDraftSavedSnackbar()
    }

    ConsumableLaunchedEffect(effect = state.closeComposerWithMessageSending) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
        actions.showMessageSendingSnackbar()
    }

    ConsumableLaunchedEffect(effect = state.closeComposerWithMessageSendingOffline) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
        actions.showMessageSendingOfflineSnackbar()
    }

    ConsumableLaunchedEffect(effect = state.sendingErrorEffect) {
        sendingErrorDialogState.value = viewModel.formatSendingError(it)
    }

    ConsumableLaunchedEffect(effect = state.attachmentsFileSizeExceeded) { attachmentSizeDialogState.value = true }

    BackHandler(true) {
        viewModel.submit(ComposerAction.OnCloseComposer)
    }
}

private fun buildActions(
    viewModel: ComposerViewModel,
    onToggleRecipients: (Boolean) -> Unit,
    onFocusChanged: (FocusedFieldType) -> Unit,
    setBottomSheetType: (BottomSheetType) -> Unit
): ComposerFormActions = ComposerFormActions(
    onToggleRecipients = onToggleRecipients,
    onFocusChanged = onFocusChanged,
    onToChanged = { viewModel.submit(ComposerAction.RecipientsToChanged(it)) },
    onCcChanged = { viewModel.submit(ComposerAction.RecipientsCcChanged(it)) },
    onBccChanged = { viewModel.submit(ComposerAction.RecipientsBccChanged(it)) },
    onSubjectChanged = { viewModel.submit(ComposerAction.SubjectChanged(Subject(it))) },
    onBodyChanged = { viewModel.submit(ComposerAction.DraftBodyChanged(DraftBody(it))) },
    onChangeSender = {
        setBottomSheetType(BottomSheetType.ChangeSender)
        viewModel.submit(ComposerAction.ChangeSenderRequested)
    }
)

object ComposerScreen {

    const val DraftMessageIdKey = "draft_message_id"
    const val SerializedDraftActionKey = "serialized_draft_action_key"

    data class Actions(
        val onCloseComposerClick: () -> Unit,
        val showDraftSavedSnackbar: () -> Unit,
        val showMessageSendingSnackbar: () -> Unit,
        val showMessageSendingOfflineSnackbar: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onCloseComposerClick = {},
                showDraftSavedSnackbar = {},
                showMessageSendingSnackbar = {},
                showMessageSendingOfflineSnackbar = {}
            )
        }
    }
}

private enum class BottomSheetType { AddAttachments, ChangeSender }

@Composable
@AdaptivePreviews
private fun MessageDetailScreenPreview() {
    ProtonTheme3 {
        ComposerScreen(ComposerScreen.Actions.Empty)
    }
}
