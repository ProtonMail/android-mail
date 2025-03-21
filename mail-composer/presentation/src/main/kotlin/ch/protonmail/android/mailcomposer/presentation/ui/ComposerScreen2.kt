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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.StoreAttachments
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerBottomSheetType
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsStateManager
import ch.protonmail.android.mailcomposer.presentation.model.SendExpiringMessageDialogState
import ch.protonmail.android.mailcomposer.presentation.model.operations.ComposerAction2
import ch.protonmail.android.mailcomposer.presentation.ui.form.ComposerForm2
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel2
import ch.protonmail.android.mailmessage.presentation.ui.AttachmentFooter
import ch.protonmail.android.uicomponents.bottomsheet.bottomSheetHeightConstrainedContent
import ch.protonmail.android.uicomponents.dismissKeyboard
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import timber.log.Timber
import kotlin.time.Duration

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ComposerScreen2(actions: ComposerScreen.Actions) {
    val recipientsStateManager = remember { RecipientsStateManager() }

    val viewModel = hiltViewModel<ComposerViewModel2, ComposerViewModel2.Factory> { factory ->
        factory.create(recipientsStateManager)
    }

    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val composerStates by viewModel.composerStates.collectAsStateWithLifecycle()
    val mainState = composerStates.main
    val attachmentsState = composerStates.attachments
    val accessoriesState = composerStates.accessories
    val effectsState = composerStates.effects

    var bottomSheetType = rememberSaveable { mutableStateOf(ComposerBottomSheetType.ChangeSender) }
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    var attachmentSizeDialogVisible by remember { mutableStateOf(false) }
    var sendingErrorDialogState by remember { mutableStateOf<String?>(null) }
    var senderChangedNoticeDialogState by remember { mutableStateOf<String?>(null) }
    var sendWithoutSubjectDialogState by remember { mutableStateOf(false) }
    var sendExpiringMessageDialogState by remember {
        mutableStateOf(SendExpiringMessageDialogState(false, emptyList()))
    }

    val formActions = ComposerForm2.Actions(
        onChangeSender = {
            bottomSheetType.value = ComposerBottomSheetType.ChangeSender
            viewModel.submitAction(ComposerAction2.ChangeSender)
        },
        onRespondInline = { viewModel.submitAction(ComposerAction2.RespondInline) }
    )

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) viewModel.submitAction(ComposerAction2.StoreAttachments(uris))
        }
    )

    val snackbarHostState = remember { ProtonSnackbarHostState() }

    if (mainState.loadingType == ComposerState.LoadingType.Save) {
        LoadingIndicator(preventBackNavigation = true)
    }

    BackHandler(true) {
        viewModel.submitAction(ComposerAction2.CloseComposer)
    }

    ProtonModalBottomSheetLayout(
        sheetContent = bottomSheetHeightConstrainedContent {
            when (bottomSheetType.value) {
                ComposerBottomSheetType.ChangeSender -> ChangeSenderBottomSheetContent(
                    mainState.senderAddresses,
                    { sender -> viewModel.submitAction(ComposerAction2.SetSenderAddress(sender)) }
                )

                ComposerBottomSheetType.SetExpirationTime -> SetExpirationTimeBottomSheetContent(
                    expirationTime = accessoriesState.messageExpiresIn,
                    onDoneClick = { viewModel.submitAction(ComposerAction2.SetMessageExpiration(it)) }
                )
            }
        },
        sheetState = bottomSheetState
    ) {
        Scaffold(
            modifier = Modifier.testTag(ComposerTestTags.RootItem),
            topBar = {
                ComposerTopBar(
                    attachmentsCount = attachmentsState.uiModel.attachments.size,
                    onAddAttachmentsClick = {
                        viewModel.submitAction(ComposerAction2.OpenFilePicker)
                    },
                    onCloseComposerClick = {
                        viewModel.submitAction(ComposerAction2.CloseComposer)
                    },
                    onSendMessageComposerClick = {
                        viewModel.submitAction(ComposerAction2.SendMessage)
                    },
                    isSendMessageButtonEnabled = mainState.isSubmittable,
                    enableSecondaryButtonsInteraction = true
                )
            },
            bottomBar = {
                ComposerBottomBar(
                    draftId = mainState.draftId,
                    senderEmail = SenderEmail(mainState.senderUiModel.email),
                    isMessagePasswordSet = accessoriesState.isMessagePasswordSet,
                    isMessageExpirationTimeSet = accessoriesState.messageExpiresIn != Duration.ZERO,
                    onSetMessagePasswordClick = actions.onSetMessagePasswordClick,
                    onSetExpirationTimeClick = {
                        bottomSheetType.value = ComposerBottomSheetType.SetExpirationTime
                        viewModel.submitAction(ComposerAction2.OpenExpirationSettings)
                    },
                    enableInteractions = true
                )
            },
            snackbarHost = {
                DismissableSnackbarHost(
                    modifier = Modifier.testTag(CommonTestTags.SnackbarHost),
                    protonSnackbarHostState = snackbarHostState
                )
            }
        ) { paddingValues ->
            if (mainState.loadingType == ComposerState.LoadingType.Initial) {
                @Suppress("MagicNumber")
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(.5f)
                ) { ProtonCenteredProgress() }
            } else {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                ) {
                    ComposerForm2(
                        changeFocusToField = effectsState.changeFocusToField,
                        senderEmail = mainState.senderUiModel.email,
                        recipientsStateManager = recipientsStateManager,
                        subjectTextField = viewModel.subjectTextField,
                        bodyTextField = viewModel.bodyFieldText,
                        quotedHtmlContent = mainState.quotedHtmlContent?.styled,
                        shouldRestrictWebViewHeight = mainState.shouldRestrictWebViewHeight,
                        focusTextBody = effectsState.focusTextBody,
                        actions = formActions,
                        modifier = Modifier.testTag(ComposerTestTags.ComposerForm)
                    )

                    if (attachmentsState.uiModel.attachments.isNotEmpty()) {
                        AttachmentFooter(
                            messageBodyAttachmentsUiModel = attachmentsState.uiModel,
                            actions = AttachmentFooter.Actions(
                                onShowAllAttachments = { Timber.d("On show all attachments clicked") },
                                onAttachmentClicked = { Timber.d("On attachment clicked: $it") },
                                onAttachmentDeleteClicked = {
                                    viewModel.submitAction(ComposerAction2.RemoveAttachment(it))
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    ConsumableLaunchedEffect(effect = effectsState.openImagePicker) {
        imagePicker.launch("*/*")
    }

    ConsumableLaunchedEffect(effect = effectsState.closeComposer) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
    }

    ConsumableLaunchedEffect(effect = effectsState.closeComposerWithDraftSaved) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
        actions.showDraftSavedSnackbar(mainState.draftId)
    }

    ConsumableLaunchedEffect(effect = effectsState.closeComposerWithMessageSending) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
        actions.showMessageSendingSnackbar()
    }

    ConsumableLaunchedEffect(effect = effectsState.closeComposerWithMessageSendingOffline) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
        actions.showMessageSendingOfflineSnackbar()
    }

    ConsumableTextEffect(effect = effectsState.attachmentsReEncryptionFailed) {
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.ERROR, message = it)
    }

    ConsumableTextEffect(effect = effectsState.error) {
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.ERROR, message = it)
    }

    ConsumableTextEffect(effect = effectsState.exitError) {
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.ERROR, message = it)
        actions.onCloseComposerClick()
    }

    ConsumableTextEffect(effect = effectsState.warning) {
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.WARNING, message = it)
    }

    ConsumableTextEffect(effect = effectsState.premiumFeatureMessage) {
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.NORM, message = it)
    }

    ConsumableLaunchedEffect(effect = effectsState.changeBottomSheetVisibility) { show ->
        if (show) {
            dismissKeyboard(context, view, keyboardController)
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    ConsumableLaunchedEffect(effect = effectsState.confirmSendingWithoutSubject) {
        sendWithoutSubjectDialogState = true
    }

    ConsumableLaunchedEffect(effect = effectsState.confirmSendExpiringMessage) {
        sendExpiringMessageDialogState = SendExpiringMessageDialogState(
            isVisible = true, externalParticipants = it
        )
    }

    ConsumableLaunchedEffect(effect = effectsState.attachmentsFileSizeExceeded) {
        attachmentSizeDialogVisible = true
    }

    ConsumableTextEffect(effect = effectsState.sendingErrorEffect) {
        sendingErrorDialogState = it
    }

    ConsumableTextEffect(effect = effectsState.senderChangedNotice) {
        senderChangedNoticeDialogState = it
    }

    if (sendWithoutSubjectDialogState) {
        SendingWithEmptySubjectDialog(
            onConfirmClicked = {
                viewModel.submitAction(ComposerAction2.ConfirmSendWithNoSubject)
                sendWithoutSubjectDialogState = false
            },
            onDismissClicked = {
                viewModel.submitAction(ComposerAction2.CancelSendWithNoSubject)
                sendWithoutSubjectDialogState = false
            }
        )
    }

    if (sendExpiringMessageDialogState.isVisible) {
        SendExpiringMessageDialog(
            externalRecipients = sendExpiringMessageDialogState.externalParticipants,
            onConfirmClicked = {
                viewModel.submitAction(ComposerAction2.ConfirmSendExpirationSetToExternal)
                sendExpiringMessageDialogState = sendExpiringMessageDialogState.copy(isVisible = false)
            },
            onDismissClicked = {
                viewModel.submitAction(ComposerAction2.CancelSendExpirationSetToExternal)
                sendExpiringMessageDialogState = sendExpiringMessageDialogState.copy(isVisible = false)
            }
        )
    }

    if (attachmentSizeDialogVisible) {
        ProtonAlertDialog(
            onDismissRequest = { attachmentSizeDialogVisible = false },
            confirmButton = {
                ProtonAlertDialogButton(R.string.composer_attachment_size_exceeded_dialog_confirm_button) {
                    attachmentSizeDialogVisible = false
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

    senderChangedNoticeDialogState?.run {
        ProtonAlertDialog(
            onDismissRequest = { senderChangedNoticeDialogState = null },
            confirmButton = {
                ProtonAlertDialogButton(R.string.composer_sender_changed_dialog_confirm_button) {
                    senderChangedNoticeDialogState = null
                }
            },
            title = stringResource(id = R.string.composer_sender_changed_dialog_title),
            text = { ProtonAlertDialogText(this) }
        )
    }

    sendingErrorDialogState?.run {
        SendingErrorDialog(
            errorMessage = this,
            onDismissClicked = {
                sendingErrorDialogState = null
                viewModel.submitAction(ComposerAction2.ClearSendingError)
            }
        )
    }
}
