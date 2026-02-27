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

import android.content.Intent
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonAlertDialog
import ch.protonmail.android.design.compose.component.ProtonAlertDialogButton
import ch.protonmail.android.design.compose.component.ProtonAlertDialogText
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.component.ProtonModalBottomSheetLayout
import ch.protonmail.android.design.compose.component.ProtonSnackbarHostState
import ch.protonmail.android.design.compose.component.ProtonSnackbarType
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcommon.presentation.ui.TimePickerBottomSheetContent
import ch.protonmail.android.mailcommon.presentation.ui.TimePickerUiModel
import ch.protonmail.android.mailcommon.presentation.ui.replaceText
import ch.protonmail.android.mailcomposer.domain.model.DraftMimeType
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.RecipientsStateManager
import ch.protonmail.android.mailcomposer.presentation.model.editor.ComposeScreenMeasures
import ch.protonmail.android.mailcomposer.presentation.model.isExpirationTimeSet
import ch.protonmail.android.mailcomposer.presentation.model.operations.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen.Actions.Companion.sendMaxDaysInFuture
import ch.protonmail.android.mailcomposer.presentation.ui.form.ComposerForm
import ch.protonmail.android.mailcomposer.presentation.ui.util.EdgeGuardNestedScrollConnection
import ch.protonmail.android.mailcomposer.presentation.viewmodel.ComposerViewModel
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.uicomponents.dismissKeyboard
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UseComposableActions")
@Composable
fun ComposerScreen(actions: ComposerScreen.Actions) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val composerInstanceId = rememberSaveable { java.util.UUID.randomUUID().toString() }
    val recipientsStateManager = remember { RecipientsStateManager() }
    val viewModel = hiltViewModel<ComposerViewModel, ComposerViewModel.Factory>(
        key = "composerViewModel_$composerInstanceId"
    ) { factory ->
        factory.create(composerInstanceId, recipientsStateManager)
    }
    val composerStates by viewModel.composerStates.collectAsStateWithLifecycle()
    val mainState = composerStates.main
    val attachmentsState = composerStates.attachments
    val accessoriesState = composerStates.accessories
    val effectsState = composerStates.effects

    val snackbarHostState = remember { ProtonSnackbarHostState() }
    val bottomSheetType = rememberSaveable(stateSaver = BottomSheetType.Saver) {
        mutableStateOf(BottomSheetType.ChangeSender)
    }
    val bottomSheetState = rememberModalBottomSheetState()
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val attachmentSizeDialogState = remember { mutableStateOf(AttachmentsFileSizeExceededDialogState.NoError) }
    val sendingErrorDialogState = remember { mutableStateOf<String?>(null) }
    val senderChangedNoticeDialogState = remember { mutableStateOf<String?>(null) }
    val sendWithoutSubjectDialogState = remember { mutableStateOf(false) }
    val sendExpiringMessageDialogState = remember {
        mutableStateOf(SendExpiringMessageDialogState(false, TextUiModel("")))
    }
    val discardDraftDialogState = rememberSaveable { mutableStateOf(false) }
    val showExpirationTimeDialog = rememberSaveable { mutableStateOf(false) }

    val featureMissingSnackbarMessage = stringResource(id = R.string.feature_coming_soon)
    val scope = rememberCoroutineScope()
    fun showFeatureMissingSnackbar() = scope.launch {
        snackbarHostState.showSnackbar(
            message = featureMissingSnackbarMessage,
            type = ProtonSnackbarType.NORM
        )
    }

    val displayBody by viewModel.displayBody.collectAsStateWithLifecycle()
    val bodyTextFieldState = viewModel.bodyTextField

    fun dismissBottomSheet(continuation: () -> Unit = {}) {
        scope.launch { bottomSheetState.hide() }
            .invokeOnCompletion {
                if (!bottomSheetState.isVisible) {
                    showBottomSheet = false
                }
                continuation()
            }
    }

    val bottomBarActions = ComposerBottomBar.Actions(
        onAddAttachmentsClick = {
            bottomSheetType.value = BottomSheetType.AttachmentSources
            viewModel.submit(ComposerAction.AddAttachmentsRequested)
        },
        onSetMessagePasswordClick = {
            actions.onSetMessagePasswordClick()
        },
        onSetExpirationTimeClick = {
            viewModel.submit(ComposerAction.OpenExpirationSettings)
        },
        onDiscardDraftClicked = { viewModel.submit(ComposerAction.DiscardDraftRequested) }
    )

    val filesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            uris.forEach { uri ->
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.submit(ComposerAction.AddFileAttachments(uris))
        }
    )

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            viewModel.submit(ComposerAction.AddAttachments(uris))
        }
    )

    ConsumableLaunchedEffect(effect = effectsState.openFilesPicker) {
        filesPicker.launch(arrayOf("*/*"))
    }

    ConsumableLaunchedEffect(effect = effectsState.openPhotosPicker) {
        mediaPicker.launch(PickVisualMediaRequest())
    }

    CameraPicturePicker(
        effectsState.openCamera,
        onCaptured = { uri ->
            viewModel.submit(ComposerAction.AddAttachments(listOf(uri)))
        },
        onError = { localisedError ->
            scope.launch {
                snackbarHostState.showSnackbar(type = ProtonSnackbarType.ERROR, message = localisedError)
            }
        }
    )

    ProtonModalBottomSheetLayout(
        showBottomSheet = showBottomSheet,
        onDismissed = { showBottomSheet = false },
        dismissOnBack = true,
        sheetContent = {
            when (val sheetType = bottomSheetType.value) {
                BottomSheetType.ChangeSender -> ChangeSenderBottomSheetContent(
                    mainState.senderAddresses,
                    mainState.sender,
                    { sender -> viewModel.submit(ComposerAction.SetSenderAddress(sender)) }
                )

                is BottomSheetType.InlineImageActions -> InlineImageActionsBottomSheetContent(
                    contentId = sheetType.contentId,
                    onTransformToAttachment = {
                        viewModel.submit(ComposerAction.ConvertInlineToAttachment(it))
                    },
                    onRemove = { viewModel.submit(ComposerAction.RemoveInlineAttachment(it)) }
                )

                is BottomSheetType.AttachmentSources -> AttachmentSourceBottomSheetContent(
                    onCamera = { viewModel.submit(ComposerAction.OpenCameraPicker) },
                    onFiles = { viewModel.submit(ComposerAction.OpenFilePicker) },
                    onPhotos = { viewModel.submit(ComposerAction.OpenPhotosPicker) }
                )

                is BottomSheetType.ScheduleSendOptions -> {
                    val actions = ScheduleSendBottomSheetContent.Actions(
                        onScheduleSendConfirmed = {
                            viewModel.submit(ComposerAction.OnScheduleSend(it))
                        },
                        onPickCustomTimeRequested = {
                            bottomSheetType.value = BottomSheetType.ScheduleSendCustomTimePicker
                        },
                        onNavigateToUpsell = { type ->
                            dismissBottomSheet {
                                actions.onNavigateToUpsell(type, UpsellingEntryPoint.Feature.ScheduleSend)
                            }
                        }
                    )
                    return@ProtonModalBottomSheetLayout ScheduleSendBottomSheetContent(
                        optionsUiModel = composerStates.accessories.scheduleSendOptions,
                        actions = actions
                    )
                }

                is BottomSheetType.ScheduleSendCustomTimePicker -> TimePickerBottomSheetContent(
                    uiModel = TimePickerUiModel(
                        pickerTitle = R.string.composer_schedule_send_content_description,
                        sendButton = R.string.send_button_title,
                        maximumDaysFromNow = sendMaxDaysInFuture
                    ),
                    onClose = { dismissBottomSheet() },
                    onTimeConfirmed = {
                        viewModel.submit(ComposerAction.OnScheduleSend(it))
                    }
                )
            }
        },
        sheetState = bottomSheetState
    ) {

        Scaffold(
            modifier = Modifier.testTag(ComposerTestTags.RootItem),
            topBar = {
                ComposerTopBar(
                    onCloseComposerClick = {
                        viewModel.submit(ComposerAction.CloseComposer)
                    },
                    onSendMessageComposerClick = {
                        viewModel.submit(ComposerAction.SendMessage)
                    },
                    onScheduleSendClick = {
                        bottomSheetType.value = BottomSheetType.ScheduleSendOptions
                        viewModel.submit(ComposerAction.OnScheduleSendRequested)
                    },
                    isSendMessageEnabled = mainState.isSubmittable
                )
            },
            bottomBar = {
                ComposerBottomBar(
                    isMessagePasswordSet = accessoriesState.isMessagePasswordSet,
                    isMessageExpirationTimeSet = accessoriesState.expirationTime.isExpirationTimeSet(),
                    actions = bottomBarActions,
                    enabled = mainState.loadingType == ComposerState.LoadingType.None
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

                val coroutineScope = rememberCoroutineScope()
                val scrollState = rememberScrollState()
                var columnBounds by remember { mutableStateOf(Rect.Zero) }
                var visibleHeaderHeightPx by remember { mutableFloatStateOf(0f) }
                var visibleWebViewHeightPx by remember { mutableFloatStateOf(0f) }
                var headerHeightPx by remember { mutableFloatStateOf(0f) }
                var viewportCoordinateAlignmentEnabled by remember { mutableStateOf(true) }

                val scrollManager = remember {
                    EditorScrollManager(
                        scope = coroutineScope,
                        onUpdateScroll = { coroutineScope.launch { scrollState.scrollTo(it.roundToInt()) } },
                        onToggleViewportAlignment = { enabled ->
                            viewportCoordinateAlignmentEnabled = enabled
                        }
                    )
                }

                LaunchedEffect(Unit) {
                    combine(
                        snapshotFlow { visibleWebViewHeightPx },
                        snapshotFlow { visibleHeaderHeightPx },
                        snapshotFlow { headerHeightPx },
                        snapshotFlow { scrollState.value }

                    ) { visWebHeight, visHeaderHeight, header, scroll ->
                        ComposeScreenMeasures(
                            visibleWebViewHeightPx = visWebHeight,
                            visibleHeaderHeightPx = visHeaderHeight,
                            headerHeightPx = header,
                            scrollValuePx = scroll.toFloat()
                        )
                    }
                        .distinctUntilChanged()
                        .collect { screenMeasures ->
                            scrollManager.onScreenMeasuresChanged(screenMeasures)
                        }
                }

                var formHeightPx by remember { mutableFloatStateOf(0f) }


                if (mainState.loadingType == ComposerState.LoadingType.Save) {
                    LoadingIndicator(preventBackNavigation = true)
                }


                val edgeGuard = remember(scrollState) {
                    EdgeGuardNestedScrollConnection(scrollState)
                }

                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .then(
                            if (mainState.draftType == DraftMimeType.PlainText) {
                                // Edge-guard scroll only for plain-text drafts to prevent reverse-fling jumps
                                Modifier.nestedScroll(edgeGuard)
                            } else {
                                Modifier
                            }
                        )
                        .verticalScroll(scrollState)
                        .onGloballyPositioned { coordinates ->
                            columnBounds = coordinates.boundsInWindow()
                            formHeightPx = columnBounds.height
                        }
                ) {
                    // Not showing the form till we're done loading ensure it does receive the
                    // right "initial values" from state when displayed
                    ComposerForm(
                        composerInstanceId = composerInstanceId,
                        modifier = Modifier.testTag(ComposerTestTags.ComposerForm),
                        changeFocusToField = effectsState.changeFocusToField,
                        initialFocusField = mainState.initialFocusedField,
                        actions = ComposerForm.Actions(
                            onBodyChanged = { newBody ->
                                viewModel.bodyTextField.replaceText(newBody)
                            },
                            onChangeSender = {
                                bottomSheetType.value = BottomSheetType.ChangeSender
                                viewModel.submit(ComposerAction.ChangeSender)
                            },
                            onEditorViewMeasuresChanged = { webViewParams ->
                                scrollManager.onWebViewMeasuresChanged(
                                    webViewParams
                                )
                            },
                            onHeaderPositioned = { headerBoundsInWindow, measuredHeight ->
                                val visibleBounds = headerBoundsInWindow.intersect(columnBounds)
                                visibleHeaderHeightPx = visibleBounds.height.coerceAtLeast(0f)
                                headerHeightPx = measuredHeight
                            },
                            onEditorViewPositioned = { boundsInWindow ->
                                val visibleBounds = boundsInWindow.intersect(columnBounds)
                                visibleWebViewHeightPx = visibleBounds.height.coerceAtLeast(0f)
                            },
                            loadImage = { viewModel.loadImage(it) },
                            sanitizePastedText = { mimeType, text ->
                                viewModel.sanitizePastedText(mimeType, text)
                            },
                            onAttachmentRemoveRequested = { viewModel.submit(ComposerAction.RemoveAttachment(it)) },
                            onInlineImageRemoved = {
                                viewModel.submit(ComposerAction.InlineAttachmentDeletedFromBody(it))
                            },
                            onInlineImageClicked = { contentId ->
                                bottomSheetType.value = BottomSheetType.InlineImageActions(contentId)
                                viewModel.submit(ComposerAction.InlineImageActionsRequested)
                            },
                            onInlineImageAdded = { viewModel.submit(ComposerAction.AddAttachments(listOf(it))) },
                            onInlineImagePasted = {
                                showFeatureMissingSnackbar()
                            }
                        ),
                        senderEmail = mainState.sender.email,
                        draftType = mainState.draftType,
                        recipientsStateManager = recipientsStateManager,
                        subjectTextField = viewModel.subjectTextField,
                        bodyInitialValue = displayBody,
                        bodyTextFieldState = bodyTextFieldState,
                        attachments = attachmentsState.uiModel,
                        focusTextBody = effectsState.focusTextBody,
                        formHeightPx = formHeightPx,
                        injectInlineAttachments = effectsState.injectInlineAttachments,
                        stripInlineAttachment = effectsState.stripInlineAttachment,
                        refreshBody = effectsState.refreshBody,
                        viewportCoordinateAlignmentEnabled = viewportCoordinateAlignmentEnabled
                    )
                }
            }
        }
    }

    if (sendWithoutSubjectDialogState.value) {

        SendingWithEmptySubjectDialog(
            onConfirmClicked = {
                viewModel.submit(ComposerAction.ConfirmSendWithNoSubject)
                sendWithoutSubjectDialogState.value = false
            },
            onDismissClicked = {
                viewModel.submit(ComposerAction.CancelSendWithNoSubject)
                sendWithoutSubjectDialogState.value = false
            }
        )
    }

    if (sendExpiringMessageDialogState.value.isVisible) {
        SendExpiringMessageDialog(
            text = sendExpiringMessageDialogState.value.text,
            onConfirmClicked = {
                viewModel.submit(ComposerAction.ConfirmSendExpirationSetToExternal)
                sendExpiringMessageDialogState.value = sendExpiringMessageDialogState.value.copy(isVisible = false)
            },
            onDismissClicked = {
                sendExpiringMessageDialogState.value = sendExpiringMessageDialogState.value.copy(isVisible = false)
            },
            onAddPasswordClicked = {
                actions.onSetMessagePasswordClick()
                sendExpiringMessageDialogState.value = sendExpiringMessageDialogState.value.copy(isVisible = false)
            }
        )
    }

    if (discardDraftDialogState.value) {
        ProtonAlertDialog(
            titleResId = R.string.discard_draft_dialog_title,
            text = {
                Text(text = stringResource(id = R.string.discard_draft_dialog_text))
            },
            dismissButton = {
                ProtonAlertDialogButton(
                    titleResId = R.string.discard_draft_dialog_dismiss_button
                ) { discardDraftDialogState.value = false }
            },
            confirmButton = {
                ProtonAlertDialogButton(
                    titleResId = R.string.discard_draft_dialog_confirm_button
                ) {
                    viewModel.submit(ComposerAction.DiscardDraftConfirmed)
                    discardDraftDialogState.value = false
                }
            },
            onDismissRequest = { discardDraftDialogState.value = false }
        )
    }

    if (attachmentSizeDialogState.value.isVisible) {
        ProtonAlertDialog(
            onDismissRequest = {
                viewModel.submit(
                    ComposerAction.AcknowledgeAttachmentErrors(attachmentSizeDialogState.value.attachmentsWithError)
                )
                attachmentSizeDialogState.value = AttachmentsFileSizeExceededDialogState.NoError
            },
            confirmButton = {
                ProtonAlertDialogButton(R.string.composer_attachment_size_exceeded_dialog_confirm_button) {
                    viewModel.submit(
                        ComposerAction.AcknowledgeAttachmentErrors(attachmentSizeDialogState.value.attachmentsWithError)
                    )
                    attachmentSizeDialogState.value = AttachmentsFileSizeExceededDialogState.NoError
                }
            },
            title = stringResource(id = R.string.composer_attachment_size_exceeded_dialog_title),
            text = {
                ProtonAlertDialogText(
                    stringResource(
                        id = R.string.composer_attachment_size_exceeded_dialog_message,
                        Formatter.formatShortFileSize(
                            LocalContext.current,
                            ComposerScreen.MAX_ATTACHMENTS_SIZE
                        )
                    )
                )
            }
        )
    }

    if (showExpirationTimeDialog.value) {
        SetMessageExpirationDialog(
            expirationTime = accessoriesState.expirationTime,
            onDismiss = { showExpirationTimeDialog.value = false },
            onTimePicked = {
                showExpirationTimeDialog.value = false
                viewModel.submit(ComposerAction.SetMessageExpiration(it))
            }
        )
    }

    senderChangedNoticeDialogState.value?.run {
        ProtonAlertDialog(
            onDismissRequest = { senderChangedNoticeDialogState.value = null },
            confirmButton = {
                ProtonAlertDialogButton(R.string.composer_sender_changed_dialog_confirm_button) {
                    senderChangedNoticeDialogState.value = null
                }
            },
            title = stringResource(id = R.string.composer_sender_changed_dialog_title),
            text = { ProtonAlertDialogText(this) }
        )
    }

    sendingErrorDialogState.value?.run {
        SendingErrorDialog(
            errorMessage = this,
            onDismissClicked = {
                sendingErrorDialogState.value = null
//                viewModel.clearSendingError()
            }
        )
    }

    ConsumableTextEffect(effect = effectsState.premiumFeatureMessage) { message ->
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.NORM, message = message)
    }

    ConsumableTextEffect(effect = effectsState.error) { error ->
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.ERROR, message = error)
    }

    ConsumableTextEffect(effect = effectsState.exitError) {
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.ERROR, message = it)
        actions.onCloseComposerClick()
    }

    ConsumableTextEffect(effect = effectsState.warning) { warning ->
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.WARNING, message = warning)
    }

    val errorAttachmentEncryption = stringResource(id = R.string.composer_attachment_encryption_failed_message)
    ConsumableLaunchedEffect(effect = effectsState.attachmentsEncryptionFailed) {
        snackbarHostState.showSnackbar(type = ProtonSnackbarType.ERROR, message = errorAttachmentEncryption)
    }

    ConsumableLaunchedEffect(effect = effectsState.changeBottomSheetVisibility) { show ->
        if (show) {
            // Set flag before updating state to allow animation to be visible
            showBottomSheet = true
            dismissKeyboard(context, view, keyboardController)
            // Delay between hiding keyboard and showing sheet to avoid glitch and freeze on some devices
            delay(ComposerScreen.DELAY_SHOWING_BOTTOMSHEET)
            bottomSheetState.show()
        } else {
            dismissBottomSheet()
        }
    }

    ConsumableLaunchedEffect(effect = effectsState.closeComposer) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
    }

    ConsumableLaunchedEffect(effect = effectsState.closeComposerWithDraftSaved) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
        actions.showDraftSavedSnackbar(it)
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

    ConsumableLaunchedEffect(effect = effectsState.closeComposerWithScheduleSending) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
        actions.showMessageSchedulingSnackbar()
    }

    ConsumableLaunchedEffect(effect = effectsState.closeComposerWithScheduleSendingOffline) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
        actions.showMessageSchedulingOfflineSnackbar()
    }

    ConsumableTextEffect(effect = effectsState.sendingErrorEffect) {
        sendingErrorDialogState.value = it
    }

    ConsumableTextEffect(effect = effectsState.senderChangedNotice) {
        senderChangedNoticeDialogState.value = it
    }

    ConsumableLaunchedEffect(effect = effectsState.attachmentsFileSizeExceeded) {
        attachmentSizeDialogState.value = AttachmentsFileSizeExceededDialogState(
            isVisible = true,
            attachmentsWithError = it
        )
    }

    ConsumableLaunchedEffect(effect = effectsState.confirmSendingWithoutSubject) {
        sendWithoutSubjectDialogState.value = true
    }

    ConsumableLaunchedEffect(effect = effectsState.confirmSendExpiringMessage) {
        sendExpiringMessageDialogState.value = SendExpiringMessageDialogState(
            isVisible = true,
            text = it
        )
    }

    ConsumableLaunchedEffect(effect = effectsState.confirmDiscardDraft) {
        discardDraftDialogState.value = true
    }

    ConsumableLaunchedEffect(effect = effectsState.closeComposerWithDraftDiscarded) {
        dismissKeyboard(context, view, keyboardController)
        actions.onCloseComposerClick()
        actions.showDraftDiscardedSnackbar()
    }

    ConsumableLaunchedEffect(effect = effectsState.pickMessageExpiration) {
        showExpirationTimeDialog.value = true
    }

    BackHandler(true) {
        viewModel.submit(ComposerAction.CloseComposer)
    }

}

object ComposerScreen {

    const val DELAY_SHOWING_BOTTOMSHEET = 100L
    const val MAX_ATTACHMENTS_SIZE = 25 * 1000 * 1000L

    const val DraftMessageIdKey = "draft_message_id"
    const val SerializedDraftActionKey = "serialized_draft_action_key"
    const val HasSavedDraftKey = "draft_action_for_saved_draft_key"

    data class Actions(
        val onCloseComposerClick: () -> Unit,
        val onSetMessagePasswordClick: () -> Unit,
        val showDraftSavedSnackbar: (MessageId) -> Unit,
        val showMessageSendingSnackbar: () -> Unit,
        val showMessageSendingOfflineSnackbar: () -> Unit,
        val showMessageSchedulingSnackbar: () -> Unit,
        val showMessageSchedulingOfflineSnackbar: () -> Unit,
        val showDraftDiscardedSnackbar: () -> Unit,
        val onNavigateToUpsell: (type: UpsellingVisibility, entryPoint: UpsellingEntryPoint.Feature) -> Unit
    ) {

        companion object {

            const val sendMaxDaysInFuture = 89
            val Empty = Actions(

                onCloseComposerClick = {},
                onSetMessagePasswordClick = {},
                showDraftSavedSnackbar = {},
                showMessageSendingSnackbar = {},
                showMessageSendingOfflineSnackbar = {},
                showMessageSchedulingSnackbar = {},
                showMessageSchedulingOfflineSnackbar = {},
                showDraftDiscardedSnackbar = {},
                onNavigateToUpsell = { _, _ -> }
            )
        }
    }
}

private sealed interface BottomSheetType {
    data object ChangeSender : BottomSheetType
    data object AttachmentSources : BottomSheetType
    data object ScheduleSendOptions : BottomSheetType
    data object ScheduleSendCustomTimePicker : BottomSheetType
    data class InlineImageActions(val contentId: String) : BottomSheetType

    companion object {

        private const val TYPE_KEY = "sheetTypeKey"
        private const val CONTENT_ID_KEY = "inlineImageContentId"

        val Saver = mapSaver(
            save = { state: BottomSheetType ->
                when (state) {
                    is ChangeSender -> mapOf(TYPE_KEY to ChangeSender::class.simpleName)
                    is InlineImageActions -> {
                        mapOf(
                            TYPE_KEY to InlineImageActions::class.simpleName,
                            CONTENT_ID_KEY to state.contentId
                        )
                    }

                    is AttachmentSources -> mapOf(TYPE_KEY to AttachmentSources::class.simpleName)
                    is ScheduleSendOptions -> mapOf(TYPE_KEY to ScheduleSendOptions::class.simpleName)
                    is ScheduleSendCustomTimePicker -> mapOf(TYPE_KEY to ScheduleSendCustomTimePicker::class.simpleName)
                }
            },
            restore = { map ->
                when (map[TYPE_KEY]) {
                    ChangeSender::class.simpleName -> ChangeSender
                    InlineImageActions::class.simpleName -> InlineImageActions(map[CONTENT_ID_KEY].toString())
                    AttachmentSources::class.simpleName -> AttachmentSources
                    ScheduleSendOptions::class.simpleName -> ScheduleSendOptions
                    ScheduleSendCustomTimePicker::class.simpleName -> ScheduleSendCustomTimePicker
                    else -> throw IllegalStateException("Attempting to restore invalid bottom sheet type")
                }
            }
        )
    }
}

private data class SendExpiringMessageDialogState(
    val isVisible: Boolean,
    val text: TextUiModel
)

private data class AttachmentsFileSizeExceededDialogState(
    val isVisible: Boolean,
    val attachmentsWithError: List<AttachmentId>
) {

    companion object {

        val NoError = AttachmentsFileSizeExceededDialogState(
            isVisible = false,
            attachmentsWithError = emptyList()
        )
    }
}

@Composable
@AdaptivePreviews
private fun MessageDetailScreenPreview() {
    ProtonTheme {
        ComposerScreen(ComposerScreen.Actions.Empty)
    }
}
