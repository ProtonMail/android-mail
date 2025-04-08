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
package ch.protonmail.android.maildetail.presentation.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.domain.model.BasicContactInfo
import ch.protonmail.android.mailcommon.presentation.AdaptivePreviews
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.compose.dpToPx
import ch.protonmail.android.mailcommon.presentation.compose.pxToDp
import ch.protonmail.android.mailcommon.presentation.extension.copyTextToClipboard
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcommon.presentation.ui.BottomActionBar
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialog
import ch.protonmail.android.mailcommon.presentation.ui.spotlight.SpotlightTooltip
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.model.TrashedMessagesBannerState
import ch.protonmail.android.maildetail.presentation.previewdata.ConversationDetailsPreviewProvider
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen.scrollOffsetDp
import ch.protonmail.android.maildetail.presentation.ui.MessageBody.DoOnDisplayedEffect
import ch.protonmail.android.maildetail.presentation.ui.dialog.ReportPhishingDialog
import ch.protonmail.android.maildetail.presentation.viewmodel.ConversationDetailViewModel
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetVisibilityEffect
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.ContactActionsBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.DetailMoreActionsBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.LabelAsBottomSheetContent
import ch.protonmail.android.mailmessage.presentation.ui.bottomsheet.MoveToBottomSheetContent
import ch.protonmail.android.uicomponents.bottomsheet.bottomSheetHeightConstrainedContent
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.contact.domain.entity.ContactId
import timber.log.Timber

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConversationDetailScreen(
    modifier: Modifier = Modifier,
    actions: ConversationDetail.Actions,
    viewModel: ConversationDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isSystemBackButtonClickEnabled = remember { mutableStateOf(true) }
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    state.bottomSheetState?.let {
        // Avoids a "jumping" of the bottom sheet
        if (it.isShowEffectWithoutContent()) return@let

        ConsumableLaunchedEffect(effect = it.bottomSheetVisibilityEffect) { bottomSheetEffect ->
            when (bottomSheetEffect) {
                BottomSheetVisibilityEffect.Hide -> scope.launch { bottomSheetState.hide() }
                BottomSheetVisibilityEffect.Show -> scope.launch { bottomSheetState.show() }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (bottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
                viewModel.submit(ConversationDetailViewAction.DismissBottomSheet)
            }
        }
    }

    BackHandler(bottomSheetState.isVisible) {
        viewModel.submit(ConversationDetailViewAction.DismissBottomSheet)
    }

    BackHandler(!bottomSheetState.isVisible && isSystemBackButtonClickEnabled.value) {
        actions.recordMailboxScreenView()
        isSystemBackButtonClickEnabled.value = false
        scope.launch {
            awaitFrame()
            onBackPressedDispatcher?.onBackPressed()
        }
    }

    DeleteDialog(
        state = state.deleteDialogState,
        confirm = { viewModel.submit(ConversationDetailViewAction.DeleteConfirmed) },
        dismiss = { viewModel.submit(ConversationDetailViewAction.DeleteDialogDismissed) }
    )

    ReportPhishingDialog(
        state = state.reportPhishingDialogState,
        onConfirm = { viewModel.submit(ConversationDetailViewAction.ReportPhishingConfirmed(it)) },
        onDismiss = { viewModel.submit(ConversationDetailViewAction.ReportPhishingDismissed) }
    )

    SpotlightTooltip(
        dialogState = state.spotlightTooltip,
        ctaClick = actions.navigateToCustomizeToolbar,
        dismiss = { viewModel.submit(ConversationDetailViewAction.SpotlightDismissed) },
        displayed = { viewModel.submit(ConversationDetailViewAction.SpotlightDisplayed) }
    )

    ProtonModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = bottomSheetHeightConstrainedContent {
            when (val bottomSheetContentState = state.bottomSheetState?.contentState) {
                is MoveToBottomSheetState -> MoveToBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = MoveToBottomSheetContent.Actions(
                        onAddFolderClick = actions.onAddFolder,
                        onFolderSelected = {
                            viewModel.submit(ConversationDetailViewAction.MoveToDestinationSelected(it))
                        },
                        onDoneClick = { mailLabelText, entryPoint ->
                            viewModel.submit(
                                ConversationDetailViewAction.MoveToDestinationConfirmed(
                                    mailLabelText,
                                    entryPoint
                                )
                            )
                        },
                        onDismiss = { viewModel.submit(ConversationDetailViewAction.DismissBottomSheet) }
                    )
                )

                is LabelAsBottomSheetState -> LabelAsBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = LabelAsBottomSheetContent.Actions(
                        onAddLabelClick = actions.onAddLabel,
                        onLabelAsSelected = { viewModel.submit(ConversationDetailViewAction.LabelAsToggleAction(it)) },
                        onDoneClick = { archiveSelected, entryPoint ->
                            viewModel.submit(
                                ConversationDetailViewAction.LabelAsConfirmed(archiveSelected, entryPoint)
                            )
                        }
                    )
                )

                is DetailMoreActionsBottomSheetState -> DetailMoreActionsBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = DetailMoreActionsBottomSheetContent.Actions(
                        onReply = actions.onReply,
                        onReplyAll = actions.onReplyAll,
                        onForward = actions.onForward,
                        onMarkUnread = { viewModel.submit(ConversationDetailViewAction.MarkMessageUnread(it)) },
                        onLabel = {
                            viewModel.submit(ConversationDetailViewAction.RequestMessageLabelAsBottomSheet(it))
                        },
                        onViewInLightMode = {
                            viewModel.submit(
                                ConversationDetailViewAction.SwitchViewMode(it, ViewModePreference.LightMode)
                            )
                        },
                        onViewInDarkMode = {
                            viewModel.submit(
                                ConversationDetailViewAction.SwitchViewMode(it, ViewModePreference.DarkMode)
                            )
                        },
                        onMoveToTrash = { viewModel.submit(ConversationDetailViewAction.MoveMessage.MoveToTrash(it)) },
                        onMoveToArchive = {
                            viewModel.submit(ConversationDetailViewAction.MoveMessage.MoveToArchive(it))
                        },
                        onMoveToSpam = { viewModel.submit(ConversationDetailViewAction.MoveMessage.MoveToSpam(it)) },
                        onMove = { viewModel.submit(ConversationDetailViewAction.RequestMessageMoveToBottomSheet(it)) },
                        onPrint = { viewModel.submit(ConversationDetailViewAction.PrintRequested(it)) },
                        onReportPhishing = { viewModel.submit(ConversationDetailViewAction.ReportPhishing(it)) },
                        onMoveConversation = {
                            viewModel.submit(ConversationDetailViewAction.RequestMoveToBottomSheet)
                        },
                        onLabelConversation = {
                            viewModel.submit(ConversationDetailViewAction.RequestConversationLabelAsBottomSheet)
                        },
                        onMarkUnreadConversation = {
                            viewModel.submit(ConversationDetailViewAction.MarkUnread)
                        },
                        onMoveToSpamConversation = {
                            viewModel.submit(ConversationDetailViewAction.MoveToSpam)
                        },
                        onMoveToArchiveConversation = {
                            viewModel.submit(ConversationDetailViewAction.Archive)
                        },
                        onMoveToTrashConversation = {
                            viewModel.submit(ConversationDetailViewAction.Trash)
                        },
                        onPrintLastMessage = {
                            viewModel.submit(ConversationDetailViewAction.PrintLastMessage(context))
                        },
                        onReplyConversation = {
                            viewModel.submit(ConversationDetailViewAction.ReplyToLastMessage(replyToAll = false))
                        },
                        onReplyAllConversation = {
                            viewModel.submit(ConversationDetailViewAction.ReplyToLastMessage(replyToAll = true))
                        },
                        onForwardConversation = {
                            viewModel.submit(ConversationDetailViewAction.ForwardLastMessage)
                        },
                        onDeleteMessage = {
                            viewModel.submit(ConversationDetailViewAction.DeleteRequested)
                        },
                        onDelete = {
                            viewModel.submit(ConversationDetailViewAction.DeleteRequested)
                        },
                        onOpenCustomizeToolbar = actions.navigateToCustomizeToolbar
                    )
                )

                is ContactActionsBottomSheetState -> ContactActionsBottomSheetContent(
                    state = bottomSheetContentState,
                    actions = ContactActionsBottomSheetContent.Actions(
                        onCopyAddressClicked = {
                            val message = context.getString(R.string.contact_actions_copy_address_performed)
                            context.copyTextToClipboard(
                                label = message,
                                text = it.address
                            )
                            viewModel.submit(ConversationDetailViewAction.DismissBottomSheet)
                            actions.showSnackbar(message)
                        },
                        onCopyNameClicked = {
                            val message = context.getString(R.string.contact_actions_copy_name_performed)
                            context.copyTextToClipboard(
                                label = message,
                                text = it.name
                            )
                            viewModel.submit(ConversationDetailViewAction.DismissBottomSheet)
                            actions.showSnackbar(message)
                        },
                        onAddContactClicked = { actions.onAddContact(BasicContactInfo(it.name, it.address)) },
                        onNewMessageClicked = { actions.onComposeNewMessage(it.address) },
                        onViewContactDetailsClicked = { actions.onViewContactDetails(it) }
                    )
                )

                else -> Unit
            }
        }
    ) {
        ConversationDetailScreen(
            modifier = modifier,
            state = state,
            actions = ConversationDetailScreen.Actions(
                onExit = actions.onExit,
                onStarClick = { viewModel.submit(ConversationDetailViewAction.Star) },
                onTrashClick = { viewModel.submit(ConversationDetailViewAction.Trash) },
                onDeleteClick = { viewModel.submit(ConversationDetailViewAction.DeleteRequested) },
                onUnStarClick = { viewModel.submit(ConversationDetailViewAction.UnStar) },
                onUnreadClick = { viewModel.submit(ConversationDetailViewAction.MarkUnread) },
                onMoveToClick = { viewModel.submit(ConversationDetailViewAction.RequestMoveToBottomSheet) },
                onLabelAsClick = {
                    viewModel.submit(ConversationDetailViewAction.RequestConversationLabelAsBottomSheet)
                },
                onExpandMessage = { viewModel.submit(ConversationDetailViewAction.ExpandMessage(it)) },
                onCollapseMessage = { viewModel.submit(ConversationDetailViewAction.CollapseMessage(it)) },
                onMessageBodyLinkClicked = { messageId, uri ->
                    viewModel.submit(ConversationDetailViewAction.MessageBodyLinkClicked(messageId, uri))
                },
                onOpenMessageBodyLink = actions.openMessageBodyLink,
                onRequestScrollTo = { viewModel.submit(ConversationDetailViewAction.RequestScrollTo(it)) },
                onShowAllAttachmentsForMessage = {
                    viewModel.submit(ConversationDetailViewAction.ShowAllAttachmentsForMessage(it))
                },
                onAttachmentClicked = { messageId, attachmentId ->
                    viewModel.submit(
                        ConversationDetailViewAction.OnAttachmentClicked(messageId, attachmentId)
                    )
                },
                openAttachment = actions.openAttachment,
                handleProtonCalendarRequest = actions.handleProtonCalendarRequest,
                showFeatureMissingSnackbar = actions.showFeatureMissingSnackbar,
                loadEmbeddedImage = { messageId, contentId -> viewModel.loadEmbeddedImage(messageId, contentId) },
                onReplyLastMessage = {
                    viewModel.submit(ConversationDetailViewAction.ReplyToLastMessage(replyToAll = false))
                },
                onReplyAllLastMessage = {
                    viewModel.submit(ConversationDetailViewAction.ReplyToLastMessage(replyToAll = true))
                },
                onForwardLastMessage = {
                    viewModel.submit(ConversationDetailViewAction.ForwardLastMessage)
                },
                onPrintLastMessage = {
                    viewModel.submit(ConversationDetailViewAction.PrintLastMessage(context))
                },
                onReply = actions.onReply,
                onReplyAll = actions.onReplyAll,
                onForward = actions.onForward,
                onEffectConsumed = { messageId, effect ->
                    viewModel.submit(ConversationDetailViewAction.EffectConsumed(messageId, effect))
                },
                onReadClick = {
                    Timber.i("Read click not handled for conversation detail")
                },
                onArchiveClick = {
                    viewModel.submit(ConversationDetailViewAction.Archive)
                },
                onReportPhishingClick = {
                    viewModel.submit(ConversationDetailViewAction.ReportPhishingLastMessage)
                },
                onMoveToSpam = {
                    viewModel.submit(ConversationDetailViewAction.MoveToSpam)
                },
                onScrollRequestCompleted = { viewModel.submit(ConversationDetailViewAction.ScrollRequestCompleted) },
                onDoNotAskLinkConfirmationAgain = {
                    viewModel.submit(ConversationDetailViewAction.DoNotAskLinkConfirmationAgain)
                },
                onBodyExpandCollapseButtonClicked = {
                    viewModel.submit(ConversationDetailViewAction.ExpandOrCollapseMessageBody(it))
                },
                onMoreActionsClick = { messageId ->
                    viewModel.submit(ConversationDetailViewAction.RequestMoreActionsBottomSheet(messageId))
                },
                onLoadRemoteContent = {
                    viewModel.submit(ConversationDetailViewAction.LoadRemoteContent(MessageIdUiModel(it.id)))
                },
                onLoadEmbeddedImages = {
                    viewModel.submit(ConversationDetailViewAction.ShowEmbeddedImages(MessageIdUiModel(it.id)))
                },
                onLoadRemoteAndEmbeddedContent = {
                    viewModel.submit(ConversationDetailViewAction.LoadRemoteAndEmbeddedContent(MessageIdUiModel(it.id)))
                },
                onOpenInProtonCalendar = {
                    viewModel.submit(ConversationDetailViewAction.OpenInProtonCalendar(MessageId(it.id)))
                },
                onPrint = { messageId ->
                    viewModel.submit(ConversationDetailViewAction.Print(context, messageId))
                },
                onAvatarClicked = { participantUiModel, avatarUiModel ->
                    viewModel.submit(
                        ConversationDetailViewAction.RequestContactActionsBottomSheet(
                            participantUiModel,
                            avatarUiModel
                        )
                    )
                },
                onOpenComposer = { actions.openComposerForDraftMessage(MessageId(it.id)) },
                onParticipantClicked = { participantUiModel, avatarUiModel ->
                    viewModel.submit(
                        ConversationDetailViewAction.RequestContactActionsBottomSheet(
                            participantUiModel,
                            avatarUiModel
                        )
                    )
                },
                onTrashedMessagesBannerClick = {
                    viewModel.submit(ConversationDetailViewAction.ChangeVisibilityOfMessages)
                },
                onMoreActionsBottomBarClick = {
                    viewModel.submit(ConversationDetailViewAction.RequestConversationMoreActionsBottomSheet)
                },
                onViewEntireMessageClicked = actions.onViewEntireMessageClicked,
                navigateToCustomizeToolbar = actions.navigateToCustomizeToolbar
            ),
            scrollToMessageId = state.scrollToMessage?.id
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "ComplexMethod")
@Composable
fun ConversationDetailScreen(
    state: ConversationDetailState,
    actions: ConversationDetailScreen.Actions,
    modifier: Modifier = Modifier,
    scrollToMessageId: String?
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(snapAnimationSpec = null)
    val snackbarHostState = remember { ProtonSnackbarHostState() }
    val linkConfirmationDialogState = remember { mutableStateOf<Uri?>(null) }
    val phishingLinkConfirmationDialogState = remember { mutableStateOf<Uri?>(null) }

    ConsumableLaunchedEffect(state.exitScreenEffect) { actions.onExit(null) }
    state.exitScreenWithMessageEffect.consume()?.let { actionResult ->
        actions.onExit(actionResult)
    }
    ConsumableTextEffect(state.error) { string ->
        snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, message = string)
    }
    ConsumableTextEffect(state.message) { string ->
        snackbarHostState.showSnackbar(ProtonSnackbarType.NORM, message = string)
    }
    ConsumableLaunchedEffect(effect = state.openMessageBodyLinkEffect) { messageBodyLink ->
        val message = when (state.messagesState) {
            is ConversationDetailsMessagesState.Data -> state.messagesState.messages.find {
                it.messageId == messageBodyLink.messageId
            }

            else -> null
        }
        val requestPhishingLinkConfirmation = when (message) {
            is ConversationDetailMessageUiModel.Expanded -> message.requestPhishingLinkConfirmation
            else -> false
        }
        if (requestPhishingLinkConfirmation) {
            phishingLinkConfirmationDialogState.value = messageBodyLink.uri
        } else if (state.requestLinkConfirmation) {
            linkConfirmationDialogState.value = messageBodyLink.uri
        } else {
            actions.onOpenMessageBodyLink(messageBodyLink.uri)
        }
    }
    ConsumableLaunchedEffect(effect = state.openAttachmentEffect) {
        actions.openAttachment(it)
    }

    ConsumableLaunchedEffect(effect = state.openProtonCalendarIntent) {
        actions.handleProtonCalendarRequest(it)
    }

    ConsumableLaunchedEffect(effect = state.openReply) {
        actions.onReply(MessageId(it.id))
    }

    ConsumableLaunchedEffect(effect = state.openReplyAll) {
        actions.onReplyAll(MessageId(it.id))
    }

    ConsumableLaunchedEffect(effect = state.openForward) {
        actions.onForward(MessageId(it.id))
    }

    if (linkConfirmationDialogState.value != null) {
        ExternalLinkConfirmationDialog(
            onCancelClicked = {
                linkConfirmationDialogState.value = null
            },
            onContinueClicked = { doNotShowAgain ->
                linkConfirmationDialogState.value?.let { actions.onOpenMessageBodyLink(it) }
                linkConfirmationDialogState.value = null
                if (doNotShowAgain) {
                    actions.onDoNotAskLinkConfirmationAgain()
                }
            },
            linkUri = linkConfirmationDialogState.value
        )
    }

    if (phishingLinkConfirmationDialogState.value != null) {
        PhishingLinkConfirmationDialog(
            onCancelClicked = { phishingLinkConfirmationDialogState.value = null },
            onContinueClicked = {
                phishingLinkConfirmationDialogState.value?.let { actions.onOpenMessageBodyLink(it) }
            },
            linkUri = phishingLinkConfirmationDialogState.value
        )
    }

    if (state.conversationState is ConversationDetailMetadataState.Error) {
        val message = state.conversationState.message.string()
        LaunchedEffect(state.conversationState) {
            snackbarHostState.showSnackbar(
                type = ProtonSnackbarType.ERROR,
                message = message
            )
        }
    }

    // When SubjectHeader is first time composed, we need to get the its actual height to be able to calculate yOffset
    // for collapsing effect
    val subjectHeaderSizeCallback: (Int) -> Unit = {
        scrollBehavior.state.heightOffsetLimit = -it.toFloat()
    }

    Scaffold(
        modifier = modifier
            .testTag(ConversationDetailScreenTestTags.RootItem)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = ProtonTheme.colors.backgroundDeep,
        snackbarHost = {
            DismissableSnackbarHost(
                modifier = Modifier.testTag(CommonTestTags.SnackbarHost),
                protonSnackbarHostState = snackbarHostState
            )
        },
        topBar = {
            val uiModel = (state.conversationState as? ConversationDetailMetadataState.Data)?.conversationUiModel
            DetailScreenTopBar(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = scrollBehavior.state.heightOffset / 2f
                    },
                title = uiModel?.subject ?: DetailScreenTopBar.NoTitle,
                isStarred = uiModel?.isStarred,
                messageCount = uiModel?.messageCount,
                actions = DetailScreenTopBar.Actions(
                    onBackClick = { actions.onExit(null) },
                    onStarClick = actions.onStarClick,
                    onUnStarClick = actions.onUnStarClick
                ),
                subjectHeaderSizeCallback = subjectHeaderSizeCallback,
                topAppBarState = scrollBehavior.state
            )
        },
        bottomBar = {
            BottomActionBar(
                state = state.bottomBarState,
                viewActionCallbacks = BottomActionBar.Actions(
                    onMarkRead = actions.onReadClick,
                    onMarkUnread = actions.onUnreadClick,
                    onStar = actions.onStarClick,
                    onUnstar = actions.onUnStarClick,
                    onMove = actions.onMoveToClick,
                    onLabel = actions.onLabelAsClick,
                    onTrash = actions.onTrashClick,
                    onDelete = actions.onDeleteClick,
                    onArchive = actions.onArchiveClick,
                    onReply = actions.onReplyLastMessage,
                    onReplyAll = actions.onReplyAllLastMessage,
                    onForward = actions.onForwardLastMessage,
                    onSpam = actions.onMoveToSpam,
                    onViewInLightMode = { Timber.d("conversation onViewInLightMode clicked") },
                    onViewInDarkMode = { Timber.d("conversation onViewInDarkMode clicked") },
                    onPrint = actions.onPrintLastMessage,
                    onViewHeaders = { Timber.d("conversation onViewHeaders clicked") },
                    onViewHtml = { Timber.d("conversation onViewHtml clicked") },
                    onReportPhishing = actions.onReportPhishingClick,
                    onRemind = { Timber.d("conversation onRemind clicked") },
                    onSavePdf = { Timber.d("conversation onSavePdf clicked") },
                    onSenderEmail = { Timber.d("conversation onSenderEmail clicked") },
                    onSaveAttachments = { Timber.d("conversation onSaveAttachments clicked") },
                    onMore = actions.onMoreActionsBottomBarClick,
                    onCustomizeToolbar = actions.navigateToCustomizeToolbar
                )
            )
        }
    ) { innerPadding ->
        when (state.messagesState) {
            is ConversationDetailsMessagesState.Data -> {
                val conversationDetailItemActions = ConversationDetailItem.Actions(
                    onExpand = actions.onExpandMessage,
                    onCollapse = actions.onCollapseMessage,
                    onOpenComposer = actions.onOpenComposer,
                    onMessageBodyLinkClicked = actions.onMessageBodyLinkClicked,
                    onOpenMessageBodyLink = actions.onOpenMessageBodyLink,
                    onShowAllAttachmentsForMessage = actions.onShowAllAttachmentsForMessage,
                    onAttachmentClicked = actions.onAttachmentClicked,
                    showFeatureMissingSnackbar = actions.showFeatureMissingSnackbar,
                    loadEmbeddedImage = actions.loadEmbeddedImage,
                    onReply = actions.onReply,
                    onReplyAll = actions.onReplyAll,
                    onForward = actions.onForward,
                    onEffectConsumed = actions.onEffectConsumed,
                    onScrollRequestCompleted = actions.onScrollRequestCompleted,
                    onBodyExpandCollapseButtonClicked = actions.onBodyExpandCollapseButtonClicked,
                    onMoreActionsClick = actions.onMoreActionsClick,
                    onLoadRemoteContent = actions.onLoadRemoteContent,
                    onLoadEmbeddedImages = actions.onLoadEmbeddedImages,
                    onLoadRemoteAndEmbeddedContent = { actions.onLoadRemoteAndEmbeddedContent(it) },
                    onOpenInProtonCalendar = { actions.onOpenInProtonCalendar(it) },
                    onPrint = actions.onPrint,
                    onAvatarClicked = actions.onAvatarClicked,
                    onParticipantClicked = actions.onParticipantClicked,
                    onViewEntireMessageClicked = actions.onViewEntireMessageClicked
                )
                MessagesContent(
                    uiModels = state.messagesState.messages,
                    trashedMessagesBannerState = state.trashedMessagesBannerState,
                    padding = innerPadding,
                    scrollToMessageId = scrollToMessageId,
                    actions = conversationDetailItemActions,
                    onTrashedMessagesBannerClick = actions.onTrashedMessagesBannerClick,
                    paddingOffsetDp = scrollBehavior.state.heightOffset.pxToDp()
                )
            }

            is ConversationDetailsMessagesState.Error -> ProtonErrorMessage(
                modifier = Modifier.padding(innerPadding),
                errorMessage = state.messagesState.message.string()
            )

            is ConversationDetailsMessagesState.Loading -> ProtonCenteredProgress(
                modifier = Modifier.padding(innerPadding)
            )

            is ConversationDetailsMessagesState.Offline -> ProtonErrorMessage(
                modifier = Modifier.padding(innerPadding),
                errorMessage = stringResource(id = R.string.please_go_back_online_to_load_messages)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
@Suppress("LongParameterList", "ComplexMethod")
private fun MessagesContent(
    uiModels: ImmutableList<ConversationDetailMessageUiModel>,
    trashedMessagesBannerState: TrashedMessagesBannerState,
    padding: PaddingValues,
    scrollToMessageId: String?,
    modifier: Modifier = Modifier,
    actions: ConversationDetailItem.Actions,
    onTrashedMessagesBannerClick: () -> Unit,
    paddingOffsetDp: Dp = 0f.dp
) {
    val listState = rememberLazyListState()
    var loadedItemsChanged by remember { mutableStateOf(0) }
    val loadedItemsHeight = remember { mutableStateMapOf<String, Int>() }
    val layoutDirection = LocalLayoutDirection.current
    val contentPadding = remember(padding, paddingOffsetDp) {
        PaddingValues(
            start = padding.calculateStartPadding(layoutDirection) + ProtonDimens.SmallSpacing,
            end = padding.calculateEndPadding(layoutDirection) + ProtonDimens.SmallSpacing,
            top = (
                padding.calculateTopPadding() + ProtonDimens.SmallSpacing + paddingOffsetDp
                ).coerceAtLeast(0f.dp),
            bottom = padding.calculateBottomPadding() + ProtonDimens.SmallSpacing
        )
    }

    // Map of item heights in LazyColumn (Row index -> height)
    // We will use this map to calculate total height of first non-draft message + any draft messages below it
    val itemsHeight = remember { mutableStateMapOf<Int, Int>() }
    var initialPlaceholderHeightCalculated by remember { mutableStateOf(false) }
    var scrollCount by remember { mutableStateOf(0) }

    val visibleUiModels = uiModels.filter { it !is ConversationDetailMessageUiModel.Hidden }

    var scrollToIndex = remember(scrollToMessageId, visibleUiModels) {
        if (scrollToMessageId == null) return@remember -1
        else visibleUiModels.indexOfFirst { uiModel -> uiModel.messageId.id == scrollToMessageId }
    }

    // Sometimes we do not get all conversation items in the first call. The complete list of items can be provided
    // after a delay. In that case we need to repeat the initial automatic scroll to the most recent non-draft item
    var lastScrolledIndex by remember { mutableStateOf(-1) }
    LaunchedEffect(scrollToMessageId) {
        if (scrollToMessageId == null) {
            lastScrolledIndex = -1
        }
    }

    // Insert some offset to scrolling to make sure the message above will also be visible partially
    val scrollOffsetPx = scrollOffsetDp.dpToPx()

    LaunchedEffect(key1 = scrollToIndex, key2 = loadedItemsChanged, key3 = initialPlaceholderHeightCalculated) {
        if (scrollToIndex >= 0) {

            // We are having frequent state updates at the beginning which are causing recompositions and
            // animateScrollToItem to be cancelled or delayed. Therefore we use scrollToItem for
            // the first scroll action.
            if (loadedItemsChanged == 0) {

                listState.scrollToItem(scrollToIndex, scrollOffsetPx)

                lastScrolledIndex = scrollToIndex

                // When try to perform both scrolling and expanding at the same time, the above scrollToItem
                // suspend function is paused during WebView initialization. Therefore we notify the view model
                // after the completion of the first scrolling to start expanding the message.
                if (scrollCount == 0) {
                    scrollToMessageId?.let { actions.onExpand(MessageIdUiModel(it)) }
                }

                scrollCount++

            } else {

                // If we get a different scrollToIndex, we need to scroll to that index again
                if (scrollToIndex != lastScrolledIndex) {
                    listState.animateScrollToItem(scrollToIndex, scrollOffsetPx)
                    lastScrolledIndex = scrollToIndex
                }

                // Scrolled message expanded, so we can conclude that scrolling is completed
                actions.onScrollRequestCompleted()
                scrollToIndex = -1
            }
        }
    }

    // We will insert a placeholder after the last item to move it to the top when scrolled
    val lazyColumnHeight = remember { mutableStateOf(0) }
    var placeholderHeightPx by remember { mutableStateOf(0) }

    // Detect if user manually scrolled the list
    var userScrolled by remember { mutableStateOf(false) }
    var userTapped by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = listState.isScrollInProgress) {
        if (!userScrolled && userTapped && listState.isScrollInProgress) {
            userScrolled = true
        }
    }

    LazyColumn(
        modifier = modifier
            .testTag(ConversationDetailScreenTestTags.MessagesList)
            .pointerInteropFilter { event ->
                if (!userTapped && event.action == android.view.MotionEvent.ACTION_DOWN) {
                    userTapped = true
                }
                false // Allow the event to propagate
            }
            .onGloballyPositioned {
                lazyColumnHeight.value = it.size.height
            },
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.SmallSpacing),
        state = listState
    ) {

        when (trashedMessagesBannerState) {
            is TrashedMessagesBannerState.Shown -> {
                item {
                    TrashedMessagesBanner(
                        uiModel = trashedMessagesBannerState.trashedMessagesBannerUiModel,
                        onActionClick = onTrashedMessagesBannerClick
                    )
                }
            }

            is TrashedMessagesBannerState.Hidden -> Unit
        }

        itemsIndexed(visibleUiModels) { index, uiModel ->
            val isLastItem = index == visibleUiModels.size - 1

            ConversationDetailItem(
                uiModel = uiModel,
                actions = actions,
                modifier = when (uiModel) {
                    is ConversationDetailMessageUiModel.Collapsed,
                    is ConversationDetailMessageUiModel.Expanding -> Modifier.animateItemPlacement()

                    else -> Modifier
                }.onSizeChanged {
                    itemsHeight[index] = it.height
                },
                onMessageBodyLoadFinished = { messageId, height ->
                    loadedItemsHeight[messageId.id] = height
                    loadedItemsChanged += 1
                }
            )

            if (!userScrolled) {
                // We will insert placeholder after the last item to move it to the top when scrolled
                // Make this calculation until we get sum of all items heights before the completion of scrolling
                // We assume scroll operation is completed when the scrolled item is expanded
                if (isLastItem && scrollToIndex >= 0 && !initialPlaceholderHeightCalculated) {
                    val sumOfHeights = itemsHeight.entries.filter { it.key >= scrollToIndex }.sumOf { it.value }
                    placeholderHeightPx = lazyColumnHeight.value - sumOfHeights +
                        contentPadding.calculateTopPadding().dpToPx()

                    // We need to check if we got all items heights, in that case we need to trigger scroll to the
                    // message again by changing initialPlaceholderHeightCalculated to true. We need this scrolling
                    // only when sum of all item heights is less than the LazyColumn height (which means we have
                    // few messages in the conversation)
                    if (itemsHeight.size == visibleUiModels.size) {
                        initialPlaceholderHeightCalculated = true
                    }
                }
            } else {
                // After user scrolled, we need to reset the placeholder height to 0
                placeholderHeightPx = 0
            }

            if (isLastItem && placeholderHeightPx > 0) {
                Spacer(
                    modifier = Modifier
                        .height(placeholderHeightPx.pxToDp())
                )
            }
        }
    }
}

object ConversationDetail {

    data class Actions(
        val onExit: (notifyUserMessage: ActionResult?) -> Unit,
        val openMessageBodyLink: (uri: Uri) -> Unit,
        val openAttachment: (values: OpenAttachmentIntentValues) -> Unit,
        val handleProtonCalendarRequest: (values: OpenProtonCalendarIntentValues) -> Unit,
        val onAddLabel: () -> Unit,
        val onAddFolder: () -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit,
        val onViewContactDetails: (ContactId) -> Unit,
        val onAddContact: (basicContactInfo: BasicContactInfo) -> Unit,
        val onComposeNewMessage: (recipientAddress: String) -> Unit,
        val navigateToCustomizeToolbar: () -> Unit,
        val openComposerForDraftMessage: (messageId: MessageId) -> Unit,
        val showSnackbar: (message: String) -> Unit,
        val recordMailboxScreenView: () -> Unit,
        val onViewEntireMessageClicked: (MessageId, Boolean, Boolean, ViewModePreference) -> Unit
    )
}

object ConversationDetailScreen {

    const val ConversationIdKey = "conversation id"
    const val ScrollToMessageIdKey = "scroll to message id"
    const val FilterByLocationKey = "opened from location"

    val scrollOffsetDp: Dp = (-30).dp

    data class Actions(
        val onExit: (notifyUserMessage: ActionResult?) -> Unit,
        val onStarClick: () -> Unit,
        val onTrashClick: () -> Unit,
        val onDeleteClick: () -> Unit,
        val onUnStarClick: () -> Unit,
        val onReadClick: () -> Unit,
        val onUnreadClick: () -> Unit,
        val onMoveToClick: () -> Unit,
        val onArchiveClick: () -> Unit,
        val onLabelAsClick: () -> Unit,
        val onReportPhishingClick: () -> Unit,
        val onExpandMessage: (MessageIdUiModel) -> Unit,
        val onCollapseMessage: (MessageIdUiModel) -> Unit,
        val onMessageBodyLinkClicked: (messageId: MessageIdUiModel, uri: Uri) -> Unit,
        val onOpenMessageBodyLink: (uri: Uri) -> Unit,
        val onDoNotAskLinkConfirmationAgain: () -> Unit,
        val onRequestScrollTo: (MessageIdUiModel) -> Unit,
        val onScrollRequestCompleted: () -> Unit,
        val onShowAllAttachmentsForMessage: (MessageIdUiModel) -> Unit,
        val onAttachmentClicked: (MessageIdUiModel, AttachmentId) -> Unit,
        val openAttachment: (values: OpenAttachmentIntentValues) -> Unit,
        val handleProtonCalendarRequest: (values: OpenProtonCalendarIntentValues) -> Unit,
        val showFeatureMissingSnackbar: () -> Unit,
        val loadEmbeddedImage: (messageId: MessageId?, contentId: String) -> GetEmbeddedImageResult?,
        val onReply: (MessageId) -> Unit,
        val onReplyAll: (MessageId) -> Unit,
        val onForward: (MessageId) -> Unit,
        val onEffectConsumed: (MessageId, DoOnDisplayedEffect) -> Unit,
        val onReplyLastMessage: () -> Unit,
        val onForwardLastMessage: () -> Unit,
        val onMoveToSpam: () -> Unit,
        val onPrintLastMessage: () -> Unit,
        val onReplyAllLastMessage: () -> Unit,
        val onBodyExpandCollapseButtonClicked: (MessageIdUiModel) -> Unit,
        val onMoreActionsClick: (MessageId) -> Unit,
        val onMoreActionsBottomBarClick: () -> Unit,
        val onLoadRemoteContent: (MessageId) -> Unit,
        val onLoadEmbeddedImages: (MessageId) -> Unit,
        val onLoadRemoteAndEmbeddedContent: (MessageId) -> Unit,
        val onOpenInProtonCalendar: (MessageId) -> Unit,
        val onOpenComposer: (MessageIdUiModel) -> Unit,
        val onPrint: (MessageId) -> Unit,
        val onAvatarClicked: (ParticipantUiModel, AvatarUiModel) -> Unit,
        val onParticipantClicked: (ParticipantUiModel, AvatarUiModel) -> Unit,
        val onTrashedMessagesBannerClick: () -> Unit,
        val onViewEntireMessageClicked: (MessageId, Boolean, Boolean, ViewModePreference) -> Unit,
        val navigateToCustomizeToolbar: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onExit = {},
                onStarClick = {},
                onTrashClick = {},
                onDeleteClick = {},
                onUnStarClick = {},
                onReadClick = {},
                onUnreadClick = {},
                onMoveToClick = {},
                onLabelAsClick = {},
                onReportPhishingClick = {},
                onArchiveClick = {},
                onExpandMessage = {},
                onCollapseMessage = {},
                onMessageBodyLinkClicked = { _, _ -> },
                onOpenMessageBodyLink = {},
                onDoNotAskLinkConfirmationAgain = {},
                onRequestScrollTo = {},
                onScrollRequestCompleted = {},
                onShowAllAttachmentsForMessage = {},
                onAttachmentClicked = { _, _ -> },
                openAttachment = {},
                handleProtonCalendarRequest = {},
                showFeatureMissingSnackbar = {},
                loadEmbeddedImage = { _, _ -> null },
                onReply = {},
                onReplyAll = {},
                onEffectConsumed = { _, _ -> },
                onReplyLastMessage = {},
                onReplyAllLastMessage = {},
                onForwardLastMessage = {},
                onPrintLastMessage = {},
                onMoveToSpam = {},
                onForward = {},
                onBodyExpandCollapseButtonClicked = {},
                onMoreActionsClick = {},
                onMoreActionsBottomBarClick = {},
                onLoadRemoteContent = {},
                onLoadEmbeddedImages = {},
                onLoadRemoteAndEmbeddedContent = {},
                onOpenInProtonCalendar = {},
                onOpenComposer = {},
                onPrint = { _ -> },
                onAvatarClicked = { _, _ -> },
                onParticipantClicked = { _, _ -> },
                onTrashedMessagesBannerClick = {},
                onViewEntireMessageClicked = { _, _, _, _ -> },
                navigateToCustomizeToolbar = {}
            )
        }
    }
}

@Composable
@AdaptivePreviews
private fun ConversationDetailScreenPreview(
    @PreviewParameter(ConversationDetailsPreviewProvider::class) state: ConversationDetailState
) {
    ProtonTheme3 {
        ProtonTheme {
            ConversationDetailScreen(
                state = state,
                actions = ConversationDetailScreen.Actions.Empty,
                scrollToMessageId = null
            )
        }
    }
}

object ConversationDetailScreenTestTags {

    const val RootItem = "ConversationDetailScreenRootItem"
    const val MessagesList = "ConversationDetailMessagesList"
}
