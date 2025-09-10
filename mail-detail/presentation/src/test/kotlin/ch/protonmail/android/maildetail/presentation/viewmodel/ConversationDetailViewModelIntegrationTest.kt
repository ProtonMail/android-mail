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

package ch.protonmail.android.maildetail.presentation.viewmodel

import java.util.Locale
import java.util.Random
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.text.format.Formatter
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Event
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailattachments.domain.model.AttachmentDisposition
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.AttachmentMetadata
import ch.protonmail.android.mailattachments.domain.model.AttachmentMimeType
import ch.protonmail.android.mailattachments.domain.model.MimeTypeCategory
import ch.protonmail.android.mailattachments.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailattachments.domain.sample.AttachmentMetadataSamples
import ch.protonmail.android.mailattachments.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.mailcommon.domain.annotation.MissingRustApi
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.GetAppLocale
import ch.protonmail.android.mailcommon.domain.usecase.GetCurrentEpochTimeDuration
import ch.protonmail.android.mailcommon.domain.usecase.GetLocalisedCalendar
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.mapper.AvatarInformationMapper
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.mapper.ExpirationTimeMapper
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetState
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcommon.presentation.sample.ParticipantAvatarSample
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcommon.presentation.usecase.FormatExtendedTime
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.mailcontact.domain.model.ContactMetadata
import ch.protonmail.android.mailcontact.domain.usecase.FindContactByEmail
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import ch.protonmail.android.mailconversation.domain.usecase.GetConversationAvailableActions
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.mailconversation.domain.usecase.StarConversations
import ch.protonmail.android.mailconversation.domain.usecase.UnStarConversations
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues.OpenIcsInProtonCalendar
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues.OpenProtonCalendarOnPlayStore
import ch.protonmail.android.maildetail.domain.usecase.AnswerRsvpEvent
import ch.protonmail.android.maildetail.domain.usecase.GetDownloadingAttachmentsForMessages
import ch.protonmail.android.maildetail.domain.usecase.GetRsvpEvent
import ch.protonmail.android.maildetail.domain.usecase.IsProtonCalendarInstalled
import ch.protonmail.android.maildetail.domain.usecase.MarkConversationAsRead
import ch.protonmail.android.maildetail.domain.usecase.MarkConversationAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsLegitimate
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsRead
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MessageViewStateCache
import ch.protonmail.android.maildetail.domain.usecase.MoveConversation
import ch.protonmail.android.maildetail.domain.usecase.MoveMessage
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationMessages
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationViewState
import ch.protonmail.android.maildetail.domain.usecase.ObserveDetailBottomBarActions
import ch.protonmail.android.maildetail.domain.usecase.ReportPhishingMessage
import ch.protonmail.android.maildetail.domain.usecase.UnblockSender
import ch.protonmail.android.maildetail.domain.usecase.UnsubscribeFromNewsletter
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.mapper.ActionResultMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.DetailAvatarUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageBannersUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageBodyUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailFooterUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailHeaderUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageIdUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageLocationUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ParticipantUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.rsvp.RsvpButtonsUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.rsvp.RsvpEventUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.rsvp.RsvpStatusUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDeleteState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Collapsed
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Expanded
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel.Expanding
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ExpandMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.OnAttachmentClicked
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.RequestScrollTo
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction.ShowAllAttachmentsForMessage
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.MarkAsLegitimateDialogState
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.maildetail.presentation.model.MoreActionsBottomSheetEntryPoint
import ch.protonmail.android.maildetail.presentation.model.ReportPhishingDialogState
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDeleteDialogReducer
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailMessagesReducer
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailMetadataReducer
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.reducer.ConversationReportPhishingDialogReducer
import ch.protonmail.android.maildetail.presentation.reducer.EditScheduledMessageDialogReducer
import ch.protonmail.android.maildetail.presentation.reducer.MarkAsLegitimateDialogReducer
import ch.protonmail.android.maildetail.presentation.reducer.TrashedMessagesBannerReducer
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.usecase.FormatRsvpWidgetTime
import ch.protonmail.android.maildetail.presentation.usecase.FormatScheduleSendTime
import ch.protonmail.android.maildetail.presentation.usecase.GetMessagesInSameExclusiveLocation
import ch.protonmail.android.maildetail.presentation.usecase.GetMoreActionsBottomSheetData
import ch.protonmail.android.maildetail.presentation.usecase.LoadImageAvoidDuplicatedExecution
import ch.protonmail.android.maildetail.presentation.usecase.ObservePrimaryUserAddress
import ch.protonmail.android.maildetail.presentation.usecase.print.PrintConfiguration
import ch.protonmail.android.maildetail.presentation.usecase.print.PrintMessage
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.sample.LabelSample
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToItemId
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmessage.domain.model.ConversationMessages
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.EventId
import ch.protonmail.android.mailmessage.domain.model.GetMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.domain.model.MessageBodyTransformations
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageTheme
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.RsvpAnswer
import ch.protonmail.android.mailmessage.domain.model.RsvpAttendee
import ch.protonmail.android.mailmessage.domain.model.RsvpAttendeeStatus
import ch.protonmail.android.mailmessage.domain.model.RsvpEvent
import ch.protonmail.android.mailmessage.domain.model.RsvpOccurrence
import ch.protonmail.android.mailmessage.domain.model.RsvpOrganizer
import ch.protonmail.android.mailmessage.domain.model.RsvpState
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.usecase.CancelScheduleSendMessage
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import ch.protonmail.android.mailmessage.domain.usecase.GetMessageAvailableActions
import ch.protonmail.android.mailmessage.domain.usecase.GetMessageBodyWithClickableLinks
import ch.protonmail.android.mailmessage.domain.usecase.LoadAvatarImage
import ch.protonmail.android.mailmessage.domain.usecase.ObserveAvatarImageStates
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailmessage.domain.usecase.StarMessages
import ch.protonmail.android.mailmessage.domain.usecase.UnStarMessages
import ch.protonmail.android.mailmessage.presentation.mapper.AttachmentGroupUiModelMapper
import ch.protonmail.android.mailmessage.presentation.mapper.AttachmentMetadataUiModelMapper
import ch.protonmail.android.mailmessage.presentation.mapper.AvatarImageUiModelMapper
import ch.protonmail.android.mailmessage.presentation.mapper.DetailMoreActionsBottomSheetUiMapper
import ch.protonmail.android.mailmessage.presentation.mapper.MailLabelTextMapper
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.ContactActionsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.DetailMoreActionsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.MailboxMoreActionsBottomSheetReducer
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.PrivacySettings
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsRefreshSignal
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObservePrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateLinkConfirmationSetting
import ch.protonmail.android.mailsnooze.domain.SnoozeRepository
import ch.protonmail.android.testdata.action.AvailableActionsTestData
import ch.protonmail.android.testdata.avatar.AvatarImageStatesTestData
import ch.protonmail.android.testdata.contact.ContactSample
import ch.protonmail.android.testdata.message.MessageAttachmentMetadataTestData
import ch.protonmail.android.testdata.message.MessageThemeOptionsTestData
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration

internal class ConversationDetailViewModelIntegrationTest {

    private val userId = UserIdSample.Primary
    private val conversationId = ConversationIdSample.WeatherForecast
    private val filterByLocationLabelId = SystemLabelId.Archive.labelId

    // region mock observe use cases
    private val observeContacts: ObserveContacts = mockk {
        coEvery {
            this@mockk(userId = UserIdSample.Primary)
        } returns flowOf(emptyList<ContactMetadata.Contact>().right())
    }
    private val observeConversationUseCase: ObserveConversation = mockk {
        coEvery {
            this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast, filterByLocationLabelId)
        } returns flowOf(ConversationSample.WeatherForecast.right())
    }
    private val observeMessage = mockk<ObserveMessage>()
    private val observeConversationMessages: ObserveConversationMessages = mockk {
        coEvery {
            this@mockk(UserIdSample.Primary, ConversationIdSample.WeatherForecast, filterByLocationLabelId)
        } returns flowOf(
            ConversationMessages(
                messages = nonEmptyListOf(
                    MessageSample.Invoice,
                    MessageSample.UnreadInvoice
                ),
                messageIdToOpen = MessageSample.Invoice.messageId
            ).right()
        )
    }
    private val observeDetailBottomBarActions = mockk<ObserveDetailBottomBarActions> {
        coEvery {
            this@mockk(
                UserIdSample.Primary,
                filterByLocationLabelId,
                ConversationIdSample.WeatherForecast
            )
        } returns flowOf(listOf(Action.Archive, Action.MarkUnread).right())
    }
    private val observePrimaryUserId: ObservePrimaryUserId = mockk {
        every { this@mockk() } returns flowOf(UserIdSample.Primary)
    }
    private val getConversationAvailableActions = mockk<GetConversationAvailableActions>()
    private val getMessageAvailableActions = mockk<GetMessageAvailableActions>()
    private val reportPhishingMessage = mockk<ReportPhishingMessage>()
    private val loadAvatarImage = mockk<LoadAvatarImage> {
        every { this@mockk.invoke(any(), any()) } returns Unit
    }
    private val observeAvatarImageStates = mockk<ObserveAvatarImageStates> {
        every { this@mockk() } returns flowOf(AvatarImageStatesTestData.SampleData1)
    }

    private val snoozeRepository = mockk<SnoozeRepository> {
        coEvery { this@mockk.unSnoozeConversation(any(), any(), any()) } returns Unit.right()
    }

    private val unsubscribeFromNewsletter = mockk<UnsubscribeFromNewsletter>()

    // Privacy settings for link confirmation dialog
    private val observePrivacySettings = mockk<ObservePrivacySettings> {
        coEvery { this@mockk.invoke(any()) } returns flowOf(
            PrivacySettings(
                autoShowRemoteContent = false,
                autoShowEmbeddedImages = false,
                preventTakingScreenshots = false,
                requestLinkConfirmation = false,
                allowBackgroundSync = false
            ).right()
        )
    }
    private val updateLinkConfirmationSetting = mockk<UpdateLinkConfirmationSetting>()

    private val getDownloadingAttachmentsForMessages = mockk<GetDownloadingAttachmentsForMessages>()
    private val getAttachmentIntentValues = mockk<GetAttachmentIntentValues>()
    private val loadImageAvoidDuplicatedExecution = mockk<LoadImageAvoidDuplicatedExecution>()
    private val findContactByEmail: FindContactByEmail = mockk<FindContactByEmail> {
        coEvery { this@mockk.invoke(any(), any()) } returns ContactSample.Stefano
    }
    // endregion

    // region mock action use cases
    private val markConversationAsRead: MarkConversationAsRead = mockk()
    private val markConversationAsUnread: MarkConversationAsUnread = mockk()
    private val move: MoveConversation = mockk()
    private val deleteConversations: DeleteConversations = mockk()
    private val savedStateHandle: SavedStateHandle = mockk {
        every { get<String>(ConversationDetailScreen.ConversationIdKey) } returns conversationId.id
        every { get<String>(ConversationDetailScreen.ScrollToMessageIdKey) } returns "null"
        every { get<String>(ConversationDetailScreen.OpenedFromLocationKey) } returns filterByLocationLabelId.id
        every { get<String>(ConversationDetailScreen.IsSingleMessageMode) } returns "false"
    }
    private val starMessages = mockk<StarMessages>()
    private val unStarMessages = mockk<UnStarMessages>()
    private val starConversations: StarConversations = mockk()
    private val unStarConversations: UnStarConversations = mockk()
    private val getDecryptedMessageBody: GetMessageBodyWithClickableLinks = mockk {
        coEvery { this@mockk.invoke(any(), any()) } returns DecryptedMessageBody(
            MessageId("default"),
            "",
            isUnread = false,
            MimeType.Html,
            hasQuotedText = false,
            hasCalendarInvite = false,
            emptyList()
        ).right()
    }

    private val observePrimaryUserAddress = mockk<ObservePrimaryUserAddress> {
        every { this@mockk() } returns flowOf(UserAddressSample.PrimaryAddress.email)
    }

    private val markMessageAsRead: MarkMessageAsRead =
        mockk {
            coEvery { this@mockk.invoke(any(), any()) } returns Unit.right()
        }
    private val getCurrentEpochTimeDuration: GetCurrentEpochTimeDuration = mockk {
        coEvery { this@mockk.invoke() } returns Duration.parse("PT0S")
    }
    private val isProtonCalendarInstalled = mockk<IsProtonCalendarInstalled>()
    private val markMessageAsUnread = mockk<MarkMessageAsUnread>()
    private val moveMessage = mockk<MoveMessage>()
    private val deleteMessages = mockk<DeleteMessages>()
    private val avatarInformationMapper = mockk<AvatarInformationMapper> {
        every {
            this@mockk.toUiModel(any(), any(), any())
        } returns ParticipantAvatarSample.ebay
    }
    private val markMessageAsLegitimate = mockk<MarkMessageAsLegitimate>()
    private val unblockSender = mockk<UnblockSender>()

    private val messageIdUiModelMapper = MessageIdUiModelMapper()
    private val attachmentMetadataUiModelMapper = AttachmentMetadataUiModelMapper()
    private val attachmentGroupUiModelMapper = AttachmentGroupUiModelMapper(attachmentMetadataUiModelMapper)
    private val getMoreActionsBottomSheetData = GetMoreActionsBottomSheetData(
        getMessageAvailableActions,
        getConversationAvailableActions,
        observeMessage,
        observeConversationUseCase
    )

    private val getMessagesInSameExclusiveLocation = mockk<GetMessagesInSameExclusiveLocation>()

    private val cancelScheduleSendMessage = mockk<CancelScheduleSendMessage>()
    // endregion

    // region mappers
    private val actionUiModelMapper = ActionUiModelMapper()
    private val colorMapper = ColorMapper()
    private val resolveParticipantName = ResolveParticipantName()
    private val messageLocationUiModelMapper = MessageLocationUiModelMapper(colorMapper)
    private val formatShortTime: FormatShortTime =
        mockk { every { this@mockk.invoke(any()) } returns TextUiModel("10:00") }
    private val formatExtendedTime: FormatExtendedTime =
        mockk { every { this@mockk.invoke(any()) } returns TextUiModel("10:00") }
    private val context = mockk<Context>()
    private val avatarImageUiModelMapper = AvatarImageUiModelMapper()
    private val getAppLocale = mockk<GetAppLocale> {
        every { this@mockk.invoke() } returns Locale.ITALIAN
    }
    private val formatScheduleSendTime = FormatScheduleSendTime(GetLocalisedCalendar(getAppLocale), getAppLocale)
    private val rsvpEventUiModelMapper = RsvpEventUiModelMapper(
        colorMapper = colorMapper,
        formatRsvpWidgetTime = FormatRsvpWidgetTime(context, getAppLocale),
        rsvpStatusUiModelMapper = RsvpStatusUiModelMapper(),
        rsvpButtonsUiModelMapper = RsvpButtonsUiModelMapper()
    )

    private val printMessage = mockk<PrintMessage>()

    private val getRsvpEvent = mockk<GetRsvpEvent>()
    private val answerRsvpEvent = mockk<AnswerRsvpEvent>()

    private val mailLabelTextMapper = mockk<MailLabelTextMapper> {
        every { this@mockk.mapToString(MailLabelText.TextString("Spam")) } returns "Spam"
        every { this@mockk.mapToString(MailLabelText.TextRes(R.string.label_title_spam)) } returns "Spam"
        every { this@mockk.mapToString(MailLabelText.TextString("Trash")) } returns "Trash"
        every { this@mockk.mapToString(MailLabelText.TextRes(R.string.label_title_trash)) } returns "Spam"
        every { this@mockk.mapToString(MailLabelText.TextRes(R.string.label_title_archive)) } returns "Archive"
    }

    private val conversationMessageMapper = ConversationDetailMessageUiModelMapper(
        avatarUiModelMapper = DetailAvatarUiModelMapper(avatarInformationMapper),
        expirationTimeMapper = ExpirationTimeMapper(getCurrentEpochTimeDuration),
        colorMapper = colorMapper,
        formatShortTime = formatShortTime,
        messageLocationUiModelMapper = messageLocationUiModelMapper,
        messageDetailHeaderUiModelMapper = MessageDetailHeaderUiModelMapper(
            colorMapper = colorMapper,
            context = mockk(),
            detailAvatarUiModelMapper = DetailAvatarUiModelMapper(avatarInformationMapper),
            formatExtendedTime = formatExtendedTime,
            formatShortTime = formatShortTime,
            messageLocationUiModelMapper = messageLocationUiModelMapper,
            participantUiModelMapper = ParticipantUiModelMapper(resolveParticipantName),
            avatarImageUiModelMapper = avatarImageUiModelMapper
        ),
        messageDetailFooterUiModelMapper = MessageDetailFooterUiModelMapper(),
        messageBannersUiModelMapper = MessageBannersUiModelMapper(context, formatScheduleSendTime),
        messageBodyUiModelMapper = MessageBodyUiModelMapper(
            attachmentGroupUiModelMapper = attachmentGroupUiModelMapper
        ),
        participantUiModelMapper = ParticipantUiModelMapper(resolveParticipantName),
        messageIdUiModelMapper = messageIdUiModelMapper,
        avatarImageUiModelMapper = avatarImageUiModelMapper,
        rsvpEventUiModelMapper = rsvpEventUiModelMapper
    )

    private val conversationMetadataMapper = ConversationDetailMetadataUiModelMapper()
    // endregion

    private val reducer = ConversationDetailReducer(
        bottomBarReducer = BottomBarReducer(),
        metadataReducer = ConversationDetailMetadataReducer(),
        messagesReducer = ConversationDetailMessagesReducer(),
        bottomSheetReducer = BottomSheetReducer(
            mailboxMoreActionsBottomSheetReducer = MailboxMoreActionsBottomSheetReducer(),
            detailMoreActionsBottomSheetReducer = DetailMoreActionsBottomSheetReducer(
                DetailMoreActionsBottomSheetUiMapper(),
                ActionUiModelMapper()
            ),
            contactActionsBottomSheetReducer = ContactActionsBottomSheetReducer()
        ),
        deleteDialogReducer = ConversationDeleteDialogReducer(),
        reportPhishingDialogReducer = ConversationReportPhishingDialogReducer(),
        trashedMessagesBannerReducer = TrashedMessagesBannerReducer(),
        markAsLegitimateDialogReducer = MarkAsLegitimateDialogReducer(),
        editScheduledMessageDialogReducer = EditScheduledMessageDialogReducer(),
        actionResultMapper = ActionResultMapper(mailLabelTextMapper)
    )

    private val inMemoryConversationStateRepository = FakeInMemoryConversationStateRepository()
    private val messageViewStateCache = MessageViewStateCache(inMemoryConversationStateRepository)
    private val observeConversationViewState = spyk(ObserveConversationViewState(inMemoryConversationStateRepository))

    private val refreshToolbarSharedFlow = MutableSharedFlow<Unit>()
    private val toolbarRefreshSignal = mockk<ToolbarActionsRefreshSignal> {
        every { this@mockk.refreshEvents } returns refreshToolbarSharedFlow
    }
    private val testDispatcher: TestDispatcher by lazy { StandardTestDispatcher() }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Formatter::formatShortFileSize)
        every { Formatter.formatShortFileSize(any(), any()) } returns "0"

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()

        mockkStatic(Color::parseColor)
        every { Color.parseColor(any()) } returns 0
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        unmockkStatic(Formatter::formatShortFileSize)
        unmockkStatic(Uri::class)
        unmockkStatic(Color::parseColor)
    }

    @Test
    fun `Should emit updated requestLinkConfirmation flag`() = runTest {
        // given
        val privacySettings = PrivacySettings(
            autoShowRemoteContent = false,
            autoShowEmbeddedImages = false,
            preventTakingScreenshots = false,
            requestLinkConfirmation = true,
            allowBackgroundSync = false
        )
        coEvery { observePrivacySettings(userId) } returns flowOf(privacySettings.right())

        // When
        buildConversationDetailViewModel().state.test {
            skipItems(3)

            // then
            val convDetailsState = awaitItem()
            assertIs<ConversationDetailState>(convDetailsState)
            assertTrue { convDetailsState.requestLinkConfirmation }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Should disable requestLinkConfirmation flag when user checks do not ask again`() = runTest {
        // given
        val initialPrivacySettings = PrivacySettings(
            autoShowRemoteContent = false,
            autoShowEmbeddedImages = false,
            preventTakingScreenshots = false,
            requestLinkConfirmation = true,
            allowBackgroundSync = false
        )
        coEvery { observePrivacySettings(userId) } returns flowOf(initialPrivacySettings.right())
        coEvery { updateLinkConfirmationSetting(any()) } returns Unit.right()

        // When
        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            skipItems(3)

            // then
            val convDetailsState = awaitItem()
            assertIs<ConversationDetailState>(convDetailsState)
            assertTrue { convDetailsState.requestLinkConfirmation }

            // when
            viewModel.submit(ConversationDetailViewAction.DoNotAskLinkConfirmationAgain)
            advanceUntilIdle()

            // then
            awaitItem()
            coVerify { updateLinkConfirmationSetting(false) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Should expand the message in the conversation after initial scroll and handle attachments`() = runTest {
        // given
        val messages = nonEmptyListOf(
            ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(3)
        )
        val messageId = MessageId(messages.first().messageId.id)

        coEvery { getDecryptedMessageBody.invoke(userId, any()) } returns DecryptedMessageBody(
            messageId = messageId,
            value = EmailBodyTestSamples.BodyWithoutQuotes,
            mimeType = MimeType.Html,
            isUnread = false,
            hasQuotedText = false,
            hasCalendarInvite = false,
            banners = emptyList(),
            attachments = listOf(
                AttachmentMetadataSamples.Document,
                AttachmentMetadataSamples.DocumentWithReallyLongFileName,
                AttachmentMetadataSamples.Invoice,
                AttachmentMetadataSamples.Image
            )
        ).right()

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            // The initial states
            skipItems(3)

            // when
            val newState = awaitItem().messagesState as ConversationDetailsMessagesState.Data

            // then
            val collapsedMessage = newState.messages.first { it.messageId == messages.first().messageId }
            assertIs<Collapsed>(collapsedMessage)

            // When
            // Initial scroll completed and UI  notifies view model to expand message
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))
            advanceUntilIdle()

            // then
            val newExpandedState = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            val expandedMessage = newExpandedState.messages.first { it.messageId == messages.first().messageId }
            assertIs<Expanded>(expandedMessage)
            assertEquals(
                messages.first().messageBodyUiModel.attachments,
                expandedMessage.messageBodyUiModel.attachments
            )
            assertFalse(expandedMessage.messageBodyUiModel.shouldShowExpandCollapseButton)
        }
    }

    @Test
    fun `Should show expand button with initially collapsed mode when message contains quote`() = runTest {
        // given
        val messages = nonEmptyListOf(
            ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(3)
        )
        val messageId = MessageId(messages.first().messageId.id)
        val transformations =
            MessageBodyTransformations(
                showQuotedText = false,
                hideEmbeddedImages = null,
                hideRemoteContent = null,
                messageThemeOptions = null
            )
        coEvery { getDecryptedMessageBody.invoke(userId, any(), transformations) } returns DecryptedMessageBody(
            messageId = messageId,
            value = EmailBodyTestSamples.BodyWithProtonMailQuote,
            mimeType = MimeType.Html,
            isUnread = false,
            hasQuotedText = true,
            hasCalendarInvite = false,
            banners = emptyList(),
            attachments = listOf(
                AttachmentMetadataSamples.Document,
                AttachmentMetadataSamples.DocumentWithReallyLongFileName,
                AttachmentMetadataSamples.Invoice,
                AttachmentMetadataSamples.Image
            )
        ).right()

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            // The initial states
            skipItems(4)

            // When
            // Initial scroll completed and UI  notifies view model to expand message
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))

            // Then
            val newExpandedStateBodyCollapsed = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            val expandedMessageBodyCollapsed = newExpandedStateBodyCollapsed.messages.first {
                it.messageId == messages.first().messageId
            }
            assertIs<Expanded>(expandedMessageBodyCollapsed)
            assertTrue(expandedMessageBodyCollapsed.messageBodyUiModel.shouldShowExpandCollapseButton)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit mapped messages as they're emitted`() = runTest {
        // Given
        val initialMessage = MessageSample.AugWeatherForecast.copy(customLabels = listOf(LabelSample.Label2021))
        val updatedMessage = initialMessage.copy(isUnread = true)
        val updatedConversationWithLabels = ConversationMessages(
            messages = nonEmptyListOf(updatedMessage),
            messageIdToOpen = updatedMessage.messageId
        )

        val conversationWithLabelsFlow = MutableStateFlow(
            ConversationMessages(
                messages = nonEmptyListOf(initialMessage),
                messageIdToOpen = initialMessage.messageId
            ).right()
        )
        val observeConversationMessagesMock = mockk<ObserveConversationMessages> {
            coEvery {
                this@mockk(userId, initialMessage.conversationId, filterByLocationLabelId)
            } returns conversationWithLabelsFlow
        }

        fun assertCorrectMessagesEmitted(actual: ConversationDetailsMessagesState.Data, expected: Message) {
            assertEquals(1, actual.messages.size)
            with(actual.messages.first() as Collapsed) {
                assertEquals(expected.messageId.id, messageId.id)
                assertEquals(expected.isUnread, isUnread)
                assertEquals(expected.customLabels.size, labels.size)
            }
        }

        // When
        buildConversationDetailViewModel(
            observeConversationMessages = observeConversationMessagesMock
        ).state.test {
            // The initial states
            skipItems(3)

            // Then
            // The expanded message
            val actualFirstMessagesState = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            assertEquals(1, actualFirstMessagesState.messages.size)
            assertCorrectMessagesEmitted(actualFirstMessagesState, expected = initialMessage)

            // When
            // Emit updated message
            conversationWithLabelsFlow.emit(updatedConversationWithLabels.right())

            // Then
            val actualUpdatedMessagesState = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            val actualLabels = (
                actualUpdatedMessagesState.messages.first()
                    as Collapsed
                ).labels
            assertCorrectMessagesEmitted(actualUpdatedMessagesState, expected = updatedMessage)
            assertEquals(LabelSample.Label2021.name, actualLabels.first().name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit first non draft message as Collapsed`() = runTest {
        // given
        val expectedCollapsed = MessageSample.AugWeatherForecast
        val messages = ConversationMessages(
            nonEmptyListOf(
                expectedCollapsed,
                MessageSample.Invoice,
                MessageSample.EmptyDraft
            ),
            expectedCollapsed.messageId
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())

        // When
        buildConversationDetailViewModel().state.test {
            skipItems(3)

            // then
            val collapsedState = awaitItem()
            val collapsedMessage = (collapsedState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == expectedCollapsed.messageId.id }
            assertIs<Collapsed>(collapsedMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit scroll to message id only on start but not when expanding a second message`() = runTest {
        // given
        val expectedScrolledTo = MessageSample.AugWeatherForecast
        val expectedExpandedNotScrolled = MessageSample.Invoice
        val messages = ConversationMessages(
            nonEmptyListOf(
                expectedScrolledTo,
                expectedExpandedNotScrolled,
                MessageSample.EmptyDraft
            ),
            expectedScrolledTo.messageId
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())

        // When
        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            skipItems(3)
            // then
            var conversationState: ConversationDetailState = awaitItem()
            val collapsedMessage = (conversationState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == expectedScrolledTo.messageId.id }
            assertIs<Collapsed>(collapsedMessage)
            assertTrue { conversationState.scrollToMessage?.id == expectedScrolledTo.messageId.id }

            // when
            // scroll request completed, clear scroll msg id in state
            viewModel.submit(ConversationDetailViewAction.ScrollRequestCompleted)

            // then
            conversationState = awaitItem()
            assertTrue { conversationState.scrollToMessage == null }

            // when
            viewModel.submit(
                ExpandMessage(
                    messageIdUiModelMapper.toUiModel(
                        MessageSample.Invoice.messageId
                    )
                )
            )

            // then
            conversationState = awaitItem()
            val expandMessage = (conversationState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == expectedExpandedNotScrolled.messageId.id }
            assertIs<Expanded>(expandMessage)
            // If there is only one expanded message that item  is scrolled to
            // (according to View Model requestScrollToMessageId)
            assertTrue { conversationState.scrollToMessage == expandMessage.messageId }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit expanding and expanded states when expanding a message`() = runTest {
        // given
        val defaultExpanded = MessageSample.AugWeatherForecast
        val expectedExpanded = MessageSample.Invoice
        val messages = ConversationMessages(
            nonEmptyListOf(
                defaultExpanded,
                expectedExpanded
            ),
            defaultExpanded.messageId
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery { getDecryptedMessageBody.invoke(any(), any()) } coAnswers {
            // Add a delay, so we're able to receive the `Expanding` state.
            // Without it, we'd only get the final `Expanded` state.
            delay(1)
            DecryptedMessageBody(
                messageId = defaultExpanded.messageId,
                value = "",
                mimeType = MimeType.Html,
                isUnread = false,
                hasQuotedText = false,
                hasCalendarInvite = false,
                banners = emptyList()
            ).right()
        }

        val viewModel = buildConversationDetailViewModel()

        // when
        viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(expectedExpanded.messageId)))

        viewModel.state.test {
            skipItems(3)

            // then
            val expandingState = awaitItem()
            val expandingMessage = (expandingState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == expectedExpanded.messageId.id }
            assertIs<Expanding>(expandingMessage)

            val expandedState = awaitItem()
            println(expandedState)
            val expandedMessage = (expandedState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == expectedExpanded.messageId.id }
            assertIs<Expanded>(expandedMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit collapsed message when collapsing it`() = runTest {
        // given
        val defaultExpanded = MessageSample.AugWeatherForecast
        val messages = ConversationMessages(
            nonEmptyListOf(
                defaultExpanded,
                MessageSample.Invoice
            ),
            defaultExpanded.messageId
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())

        val viewModel = buildConversationDetailViewModel()

        viewModel.state.test {
            skipItems(3)

            // when
            viewModel.submit(
                ConversationDetailViewAction.CollapseMessage(
                    messageIdUiModelMapper.toUiModel(defaultExpanded.messageId)
                )
            )

            // then
            val collapsedState = awaitItem()
            val collapsedMessage = (collapsedState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == defaultExpanded.messageId.id }
            assertIs<Collapsed>(collapsedMessage)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit scroll to message id when requested`() = runTest {
        // given
        val defaultExpanded = MessageSample.AugWeatherForecast
        val messages = ConversationMessages(
            nonEmptyListOf(
                defaultExpanded,
                MessageSample.Invoice
            ),
            defaultExpanded.messageId
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())

        val viewModel = buildConversationDetailViewModel()

        viewModel.state.test {
            skipItems(3)
            // when
            viewModel.submit(RequestScrollTo(messageIdUiModelMapper.toUiModel(defaultExpanded.messageId)))

            // then
            assertEquals(defaultExpanded.messageId.id, awaitItem().scrollToMessage?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit show all attachment when view action is triggered`() = runTest {
        // given
        val expectedAttachmentCount = 5
        val defaultExpanded = MessageSample.AugWeatherForecast
        val expectedExpanded = MessageSample.Invoice
        val messages = ConversationMessages(
            nonEmptyListOf(
                defaultExpanded,
                expectedExpanded
            ),
            defaultExpanded.messageId
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery { getDecryptedMessageBody.invoke(any(), expectedExpanded.messageId) } returns
            DecryptedMessageBody(
                messageId = expectedExpanded.messageId,
                value = "",
                mimeType = MimeType.Html,
                isUnread = false,
                hasQuotedText = false,
                hasCalendarInvite = false,
                banners = emptyList(),
                attachments = (0 until expectedAttachmentCount).map {
                    aMessageAttachment(id = it.toString())
                }
            ).right()

        val viewModel = buildConversationDetailViewModel()

        viewModel.state.test {
            skipItems(4)
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(expectedExpanded.messageId)))
            advanceUntilIdle()

            // When
            viewModel.submit(
                ShowAllAttachmentsForMessage(
                    messageIdUiModelMapper.toUiModel(
                        expectedExpanded.messageId
                    )
                )
            )
            advanceUntilIdle()
            val newItem = awaitItem()

            // Then
            val messagesState = (newItem.messagesState as ConversationDetailsMessagesState.Data).messages
            val expandedInvoice =
                messagesState.first { it.messageId.id == expectedExpanded.messageId.id } as Expanded
            assertEquals(expectedAttachmentCount, expandedInvoice.messageBodyUiModel.attachments!!.attachments.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify get attachment is called when attachment is clicked`() = runTest {
        // given
        val expectedAttachmentCount = 5
        val defaultExpanded = MessageSample.AugWeatherForecast
        val expectedExpanded = MessageSample.Invoice
        val messages = ConversationMessages(
            nonEmptyListOf(
                defaultExpanded,
                expectedExpanded
            ),
            defaultExpanded.messageId
        )
        val expandedMessageId = expectedExpanded.messageId
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery { getDecryptedMessageBody.invoke(any(), expandedMessageId) } returns
            DecryptedMessageBody(
                messageId = expandedMessageId,
                value = "",
                mimeType = MimeType.Html,
                isUnread = false,
                hasQuotedText = false,
                hasCalendarInvite = false,
                banners = emptyList(),
                attachments = (0 until expectedAttachmentCount).map {
                    aMessageAttachment(id = it.toString())
                }
            ).right()
        coEvery {
            getDownloadingAttachmentsForMessages(
                userId,
                listOf(defaultExpanded.messageId, expandedMessageId)
            )
        } returns listOf()
        coEvery {
            getAttachmentIntentValues.invoke(any(), any())
        } returns DataError.Local.NoDataCached.left()

        val viewModel = buildConversationDetailViewModel()

        viewModel.state.test {
            skipItems(4)
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(expectedExpanded.messageId)))

            // When
            viewModel.submit(
                OnAttachmentClicked(
                    messageIdUiModelMapper.toUiModel(expectedExpanded.messageId),
                    AttachmentId(0.toString())
                )
            )
            awaitItem()

            // Then
            coVerify { getAttachmentIntentValues(userId, AttachmentId(0.toString())) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify get attachment is not called and error is shown when other attachment is currently downloaded`() =
        runTest {
            // given
            val expectedAttachmentCount = 5
            val defaultExpanded = MessageSample.AugWeatherForecast
            val expectedExpanded = MessageSample.Invoice
            val messages = ConversationMessages(
                nonEmptyListOf(
                    defaultExpanded,
                    expectedExpanded
                ),
                defaultExpanded.messageId
            )
            val expandedMessageId = expectedExpanded.messageId
            coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
            coEvery { getDecryptedMessageBody.invoke(any(), expandedMessageId) } returns
                DecryptedMessageBody(
                    messageId = expandedMessageId,
                    value = "",
                    mimeType = MimeType.Html,
                    isUnread = false,
                    hasQuotedText = false,
                    hasCalendarInvite = false,
                    banners = emptyList(),
                    attachments = (0 until expectedAttachmentCount).map {
                        aMessageAttachment(id = it.toString())
                    }
                ).right()
            coEvery {
                getDownloadingAttachmentsForMessages(
                    userId,
                    listOf(defaultExpanded.messageId, expandedMessageId)
                )
            } returns listOf(MessageAttachmentMetadataTestData.buildMessageAttachmentMetadata())

            val viewModel = buildConversationDetailViewModel()

            viewModel.state.test {
                skipItems(4)
                viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(expectedExpanded.messageId)))
                skipItems(1)
                // When
                viewModel.submit(
                    OnAttachmentClicked(
                        messageIdUiModelMapper.toUiModel(expectedExpanded.messageId),
                        AttachmentId(0.toString())
                    )
                )
                val actualState = awaitItem()

                // Then
                assertEquals(Effect.of(TextUiModel(R.string.error_attachment_download_in_progress)), actualState.error)
                coVerify { getAttachmentIntentValues wasNot Called }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `verify error is shown when getting attachment failed`() = runTest {
        // given
        val expectedAttachmentCount = Random().nextInt(100)
        val defaultExpanded = MessageSample.AugWeatherForecast
        val expectedExpanded = MessageSample.Invoice
        val messages = nonEmptyListOf(
            defaultExpanded,
            expectedExpanded
        )
        val expandedMessageId = expectedExpanded.messageId
        mockAttachmentDownload(
            messages = messages,
            expandedMessageId = expandedMessageId,
            expectedAttachmentCount = expectedAttachmentCount,
            defaultExpanded = defaultExpanded,
            expectedError = DataError.Local.NoDataCached
        )

        val viewModel = buildConversationDetailViewModel()

        viewModel.state.test {
            skipItems(4)
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(expectedExpanded.messageId)))
            skipItems(1)

            // When
            viewModel.submit(
                OnAttachmentClicked(
                    messageIdUiModelMapper.toUiModel(expectedExpanded.messageId),
                    AttachmentId(0.toString())
                )
            )
            val actualState = awaitItem()

            // Then
            coVerify { getAttachmentIntentValues(userId, AttachmentId(0.toString())) }
            assertEquals(Effect.of(TextUiModel(R.string.error_get_attachment_failed)), actualState.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify not enough space error is shown when getting attachment failed due to insufficient storage`() =
        runTest {
            // given
            val expectedAttachmentCount = Random().nextInt(100)
            val defaultExpanded = MessageSample.AugWeatherForecast
            val expectedExpanded = MessageSample.Invoice
            val messages = nonEmptyListOf(
                defaultExpanded,
                expectedExpanded
            )
            val expandedMessageId = expectedExpanded.messageId
            mockAttachmentDownload(
                messages = messages,
                expandedMessageId = expandedMessageId,
                expectedAttachmentCount = expectedAttachmentCount,
                defaultExpanded = defaultExpanded,
                expectedError = DataError.Local.OutOfMemory
            )

            val viewModel = buildConversationDetailViewModel()

            viewModel.state.test {
                skipItems(4)
                viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(expectedExpanded.messageId)))
                skipItems(1)

                // When
                viewModel.submit(
                    OnAttachmentClicked(
                        messageIdUiModelMapper.toUiModel(expectedExpanded.messageId),
                        AttachmentId(0.toString())
                    )
                )
                val actualState = awaitItem()

                // Then
                coVerify { getAttachmentIntentValues(userId, AttachmentId(0.toString())) }
                assertEquals(Effect.of(TextUiModel(R.string.error_get_attachment_not_enough_memory)), actualState.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `verify delete dialog is shown when delete conversation is called`() = runTest {
        // Given
        val expectedTitle = TextUiModel(R.string.conversation_delete_dialog_title)
        val expectedMessage = TextUiModel(R.string.conversation_delete_dialog_message)
        val viewModel = buildConversationDetailViewModel()

        // When
        viewModel.submit(ConversationDetailViewAction.DeleteRequested)
        advanceUntilIdle()

        // Then
        assertEquals(
            expected = ConversationDeleteState(DeleteDialogState.Shown(expectedTitle, expectedMessage)),
            actual = viewModel.state.value.conversationDeleteState
        )
    }

    @Test
    fun `verify delete dialog is hidden when dismissed is called`() = runTest {
        // Given
        val viewModel = buildConversationDetailViewModel()

        // When
        viewModel.submit(ConversationDetailViewAction.DeleteRequested)
        advanceUntilIdle()
        viewModel.submit(ConversationDetailViewAction.DeleteDialogDismissed)
        advanceUntilIdle()

        // Then
        assertEquals(
            expected = ConversationDeleteState.Hidden,
            actual = viewModel.state.value.conversationDeleteState
        )
    }

    @Test
    fun `verify delete is executed when delete confirmed is called`() = runTest {
        // Given
        val expectedMessage = ActionResult.DefinitiveActionResult(TextUiModel(R.string.conversation_deleted))
        coEvery {
            observeConversationUseCase(userId, conversationId, any())
        } returns flowOf(
            ConversationSample.WeatherForecast.right()
        )
        coEvery { deleteConversations(userId, listOf(conversationId)) } returns Unit.right()

        val viewModel = buildConversationDetailViewModel()

        // When
        viewModel.submit(ConversationDetailViewAction.DeleteConfirmed)
        advanceUntilIdle()

        // Then
        coVerify { deleteConversations(userId, listOf(conversationId)) }
        assertEquals(expectedMessage, viewModel.state.value.exitScreenActionResult.consume())
    }

    @Test
    fun `should initially scroll to the message id assigned by the navigator in search mode`() = runTest {
        // given
        val searchedItem = MessageSample.AugWeatherForecast
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.SepWeatherForecast,
                searchedItem,
                MessageSample.Invoice
            ),
            searchedItem.messageId
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery { savedStateHandle.get<String>(ConversationDetailScreen.ScrollToMessageIdKey) } returns
            searchedItem.messageId.id

        // When
        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            skipItems(3)

            // then
            val conversationState: ConversationDetailState = awaitItem()
            assertEquals(searchedItem.messageId.id, conversationState.scrollToMessage?.id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun mockAttachmentDownload(
        messages: NonEmptyList<Message>,
        expandedMessageId: MessageId,
        expectedAttachmentCount: Int,
        defaultExpanded: Message,
        expectedError: DataError.Local
    ) {
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(
            ConversationMessages(messages, expandedMessageId).right()
        )
        coEvery { getDecryptedMessageBody.invoke(any(), expandedMessageId) } returns
            DecryptedMessageBody(
                messageId = expandedMessageId,
                value = "",
                mimeType = MimeType.Html,
                isUnread = false,
                hasQuotedText = false,
                hasCalendarInvite = false,
                banners = emptyList(),
                attachments = (0 until expectedAttachmentCount).map {
                    aMessageAttachment(id = it.toString())
                }
            ).right()
        coEvery {
            getDownloadingAttachmentsForMessages(
                userId,
                listOf(defaultExpanded.messageId, expandedMessageId)
            )
        } returns listOf()
        coEvery {
            getAttachmentIntentValues(userId, AttachmentId(0.toString()))
        } returns expectedError.left()
    }

    @Test
    fun `Should emit expanding and then collapse state if the message is not decrypted`() = runTest {
        // Given
        val defaultExpanded = MessageSample.AugWeatherForecast
        val messages = ConversationMessages(
            nonEmptyListOf(
                defaultExpanded,
                MessageSample.EmptyDraft
            ),
            defaultExpanded.messageId
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery { getDecryptedMessageBody.invoke(any(), any()) } coAnswers {
            // Add a delay, so we're able to receive the `Expanding` state.
            // Without it, we'd only get the final `Expanded` state.
            delay(1)
            GetMessageBodyError.Decryption(defaultExpanded.messageId, "").left()
        }

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            skipItems(3)

            // When
            val initialCollapsedState = awaitItem()

            // Then
            var message = (initialCollapsedState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == defaultExpanded.messageId.id }
            assertIs<Collapsed>(message)

            // When
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(defaultExpanded.messageId)))
            val expandingState = awaitItem()

            // Then
            message = (expandingState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == defaultExpanded.messageId.id }
            assertIs<Expanding>(message)

            val collapsedState = awaitItem()
            message = (collapsedState.messagesState as ConversationDetailsMessagesState.Data)
                .messages
                .first { it.messageId.id == defaultExpanded.messageId.id }
            assertIs<Collapsed>(message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `returns get embedded image result when getting was successful`() = runTest {
        // Given
        val messageId = MessageId("rawMessageId")
        val contentId = "contentId"
        val byteArray = "I'm a byte array".toByteArray()
        val expectedResult = MessageBodyImage(byteArray, "image/png")
        coEvery { loadImageAvoidDuplicatedExecution(userId, messageId, contentId, any()) } returns expectedResult

        // When
        val viewModel = buildConversationDetailViewModel()
        advanceUntilIdle()

        val actual = viewModel.loadImage(messageId, contentId)

        // Then
        assertEquals(expectedResult, actual)
    }

    @Test
    fun `returns null when get embedded image returned an error`() = runTest {
        // Given
        val messageId = MessageId("rawMessageId")
        val contentId = "contentId"
        coEvery { loadImageAvoidDuplicatedExecution(userId, messageId, contentId, any()) } returns null

        // When
        val viewModel = buildConversationDetailViewModel()
        advanceUntilIdle()

        val actual = viewModel.loadImage(messageId, contentId)

        // Then
        assertNull(actual)
    }

    @Test
    fun `verify bottom sheet with data is emitted when more actions bottom sheet is requested and loading succeeds`() =
        runTest {
            // Given
            val themeOptions = MessageThemeOptionsTestData.darkNoOverride
            val messageId = MessageId("messageId")
            val labelId = SystemLabelId.Archive.labelId
            coEvery {
                observeMessage(userId = userId, messageId = messageId)
            } returns flowOf(MessageSample.AugWeatherForecast.right())
            coEvery {
                getMessageAvailableActions(userId, labelId, messageId, themeOptions)
            } returns AvailableActionsTestData.replyActionsOnly.right()

            // When
            val viewModel = buildConversationDetailViewModel()
            viewModel.state.test {
                viewModel.submit(
                    ConversationDetailViewAction.RequestMessageMoreActionsBottomSheet(
                        messageId,
                        themeOptions,
                        MoreActionsBottomSheetEntryPoint.BottomBar
                    )
                )
                advanceUntilIdle()

                // Then
                assertIs<DetailMoreActionsBottomSheetState.Data>(lastEmittedItem().bottomSheetState?.contentState)
            }
        }

    @Test
    fun `verify no bottom sheet data is emitted when more actions bottom sheet is requested and loading fails`() =
        runTest {
            // Given
            val messageId = MessageId("messageId")
            val labelId = SystemLabelId.Archive.labelId
            val themeOptions = MessageThemeOptionsTestData.darkNoOverride

            coEvery {
                observeMessage(
                    userId = userId,
                    messageId = messageId
                )
            } returns flowOf(DataError.Local.NoDataCached.left())
            coEvery {
                getMessageAvailableActions(userId, labelId, messageId, themeOptions)
            } returns AvailableActionsTestData.replyActionsOnly.right()

            // When
            val viewModel = buildConversationDetailViewModel()
            viewModel.state.test {
                viewModel.submit(
                    ConversationDetailViewAction.RequestMessageMoreActionsBottomSheet(
                        messageId,
                        themeOptions,
                        MoreActionsBottomSheetEntryPoint.BottomBar
                    )
                )
                advanceUntilIdle()

                // Then
                assertNull(lastEmittedItem().bottomSheetState?.contentState)
            }
        }

    @Test
    fun `when user clicks report phishing then confirm dialog is shown`() = runTest {
        // Given
        val expected = ReportPhishingDialogState.Shown.ShowConfirmation(MessageIdSample.Invoice)

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            // When
            viewModel.submit(ConversationDetailViewAction.ReportPhishing(MessageIdSample.Invoice))
            advanceUntilIdle()

            // Then
            assertEquals(expected, lastEmittedItem().reportPhishingDialogState)
        }
    }

    @Test
    fun `when user confirms report phishing then report use case is called`() = runTest {
        // Given
        val expectedMessageId = MessageIdSample.HtmlInvoice
        coEvery { reportPhishingMessage(userId, expectedMessageId) } returns Unit.right()

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            // When
            viewModel.submit(ConversationDetailViewAction.ReportPhishingConfirmed(expectedMessageId))
            advanceUntilIdle()

            // Then
            coVerify { reportPhishingMessage(userId, expectedMessageId) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @MissingRustApi
    fun `when a user click open proton calendar and calendar is installed, then open in proton calendar is called`() =
        runTest {
            // Given
            val expectedUri = mockk<Uri>()
            val message = MessageSample.CalendarInvite
            val messages = ConversationMessages(nonEmptyListOf(message), message.messageId)

            val messageId = message.messageId
            coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
            coEvery { getDecryptedMessageBody.invoke(userId, messageId) } returns DecryptedMessageBody(
                messageId = messageId,
                value = EmailBodyTestSamples.BodyWithoutQuotes,
                mimeType = MimeType.Html,
                isUnread = false,
                hasQuotedText = false,
                hasCalendarInvite = false,
                banners = emptyList(),
                attachments = listOf(AttachmentMetadataSamples.Calendar)
            ).right()
            coEvery { isProtonCalendarInstalled() } returns true
            coEvery {
                getAttachmentIntentValues(userId, AttachmentId(AttachmentMetadataSamples.Ids.ID_CALENDAR))
            } returns OpenAttachmentIntentValues(
                mimeType = " text/calendar",
                uri = expectedUri
            ).right()

            val viewModel = buildConversationDetailViewModel()
            viewModel.state.test {
                // The initial states
                skipItems(4)
                viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))
                awaitItem() // Expanded message

                // When
                viewModel.submit(ConversationDetailViewAction.OpenInProtonCalendar(messageId))
                val actual = awaitItem()
                val calendarIntentValues = actual.openProtonCalendarIntent.consume() as? OpenIcsInProtonCalendar
                assertNotNull(calendarIntentValues)
                assertEquals(expectedUri, calendarIntentValues.uriToIcsAttachment)
                assertEquals(message.sender.address, calendarIntentValues.sender)
                // This is not correct, it's due to a @MissingRustApi: see note in `handleOpenInProtonCalendar`
                assertEquals(message.toList.first().address, calendarIntentValues.recipient)
            }
        }

    @Test
    fun `when a user click open proton calendar and calendar is not installed, then open play store is called`() =
        runTest {
            // Given
            val message = MessageSample.CalendarInvite
            val messages = ConversationMessages(nonEmptyListOf(message), message.messageId)

            val messageId = message.messageId
            coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
            coEvery { getDecryptedMessageBody.invoke(userId, messageId) } returns DecryptedMessageBody(
                messageId = messageId,
                value = EmailBodyTestSamples.BodyWithoutQuotes,
                mimeType = MimeType.Html,
                isUnread = false,
                hasQuotedText = false,
                hasCalendarInvite = false,
                banners = emptyList(),
                attachments = listOf(AttachmentMetadataSamples.Calendar)
            ).right()
            coEvery { isProtonCalendarInstalled() } returns false

            val viewModel = buildConversationDetailViewModel()
            viewModel.state.test {
                // The initial states
                skipItems(4)
                viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))
                awaitItem() // Expanded message

                // When
                viewModel.submit(ConversationDetailViewAction.OpenInProtonCalendar(messageId))
                val actual = awaitItem()
                val calendarIntentValues = actual.openProtonCalendarIntent.consume() as? OpenProtonCalendarOnPlayStore
                assertNotNull(calendarIntentValues)
            }
        }

    @Test
    fun `emit the correct state when switching of view mode has been requested`() = runTest {
        // Given
        val expandedMessageId = MessageSample.Invoice.messageId
        val transformations = MessageBodyTransformations.MessageDetailsDefaults.copy(
            messageThemeOptions = MessageThemeOptionsTestData.darkOverrideLight
        )
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.AugWeatherForecast,
                MessageSample.Invoice,
                MessageSample.EmptyDraft
            ),
            MessageSample.AugWeatherForecast.messageId
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery {
            getDecryptedMessageBody.invoke(userId, expandedMessageId, transformations)
        } returns DecryptedMessageBody(
            messageId = expandedMessageId,
            value = EmailBodyTestSamples.BodyWithoutQuotes,
            mimeType = MimeType.Html,
            isUnread = false,
            hasQuotedText = false,
            hasCalendarInvite = false,
            banners = emptyList(),
            transformations = transformations

        ).right()
        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.submit(
            ExpandMessage(
                messageIdUiModelMapper.toUiModel(expandedMessageId)
            )
        )

        viewModel.state.test {
            skipItems(4)

            viewModel.submit(
                ConversationDetailViewAction.SwitchViewMode(
                    expandedMessageId,
                    MessageTheme.Dark,
                    MessageTheme.Light
                )
            )

            skipItems(1)

            // then
            val state = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            val expandedMessage = state.messages.find {
                it.messageId.id == expandedMessageId.id
            } as Expanded
            val actual = expandedMessage.messageBodyUiModel.viewModePreference
            assertEquals(ViewModePreference.LightMode, actual)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should close bottom sheet, collapse message and call use case when marking a message as unread`() = runTest {
        // Given
        val themeOptions = MessageThemeOptionsTestData.darkNoOverride
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.AugWeatherForecast,
                MessageSample.Invoice,
                MessageSample.EmptyDraft
            ),
            MessageSample.AugWeatherForecast.messageId
        )
        val labelId = SystemLabelId.Archive.labelId
        val messageId = MessageSample.Invoice.messageId
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery {
            observeMessage(userId, messageId)
        } returns flowOf(MessageSample.Invoice.right())
        coEvery {
            getMessageAvailableActions(userId, labelId, messageId, themeOptions)
        } returns AvailableActionsTestData.replyActionsOnly.right()
        coEvery {
            markMessageAsUnread(userId, messageId)
        } returns Unit.right()

        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.submit(
            ExpandMessage(
                messageIdUiModelMapper.toUiModel(messageId)
            )
        )

        viewModel.state.test {
            skipItems(4)

            viewModel.submit(
                ConversationDetailViewAction.RequestMessageMoreActionsBottomSheet(
                    messageId, themeOptions, MoreActionsBottomSheetEntryPoint.BottomBar
                )
            )
            skipItems(2)
            viewModel.submit(
                ConversationDetailViewAction.MarkMessageUnread(
                    messageId
                )
            )

            // then
            assertEquals(
                BottomSheetVisibilityEffect.Hide, awaitItem().bottomSheetState?.bottomSheetVisibilityEffect?.consume()
            )
            val item = awaitItem()
            val messagesState = item.messagesState as ConversationDetailsMessagesState.Data
            val message = messagesState.messages.find {
                it.messageId.id == messageId.id
            }
            assertIs<Collapsed>(message)
            coVerify { markMessageAsUnread(userId, messageId) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should exit screen when marking the only message as unread`() = runTest {
        // Given
        val themeOptions = MessageThemeOptionsTestData.darkNoOverride
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.Invoice
            ),
            MessageSample.Invoice.messageId
        )
        val labelId = SystemLabelId.Archive.labelId
        val messageId = MessageSample.Invoice.messageId
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery {
            observeMessage(userId, messageId)
        } returns flowOf(MessageSample.Invoice.right())
        coEvery {
            getMessageAvailableActions(userId, labelId, messageId, themeOptions)
        } returns AvailableActionsTestData.replyActionsOnly.right()
        coEvery {
            markMessageAsUnread(userId, messageId)
        } returns Unit.right()

        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.submit(
            ExpandMessage(
                messageIdUiModelMapper.toUiModel(messageId)
            )
        )

        viewModel.state.test {
            skipItems(4)

            viewModel.submit(
                ConversationDetailViewAction.RequestMessageMoreActionsBottomSheet(
                    messageId, themeOptions, MoreActionsBottomSheetEntryPoint.BottomBar
                )
            )
            skipItems(2)
            viewModel.submit(
                ConversationDetailViewAction.MarkMessageUnread(
                    messageId
                )
            )
            advanceUntilIdle()

            assertNotNull(lastEmittedItem().exitScreenEffect.consume())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should close bottom sheet and call use case when moving a message to trash`() = runTest {
        // Given
        val themeOptions = MessageThemeOptionsTestData.darkNoOverride
        val messageId = MessageSample.Invoice.messageId
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.AugWeatherForecast,
                MessageSample.Invoice,
                MessageSample.EmptyDraft
            ),
            MessageSample.AugWeatherForecast.messageId
        )
        val labelId = SystemLabelId.Archive.labelId
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery {
            observeMessage(userId, messageId)
        } returns flowOf(MessageSample.Invoice.right())
        coEvery { moveMessage(userId, messageId, SystemLabelId.Trash) } returns Unit.right()
        coEvery {
            getMessageAvailableActions(userId, labelId, messageId, themeOptions)
        } returns AvailableActionsTestData.replyActionsOnly.right()
        coEvery {
            getMessagesInSameExclusiveLocation(userId, conversationId, messageId, any()) // labelId here is not strict
        } returns listOf<Message>(mockk(), mockk()).right()
        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))

        viewModel.state.test {
            skipItems(4)

            viewModel.submit(
                ConversationDetailViewAction.RequestMessageMoreActionsBottomSheet(
                    messageId, themeOptions, MoreActionsBottomSheetEntryPoint.BottomBar
                )
            )
            skipItems(2)
            viewModel.submit(ConversationDetailViewAction.MoveMessage.System.Trash(messageId))

            // then
            assertEquals(
                BottomSheetVisibilityEffect.Hide, awaitItem().bottomSheetState?.bottomSheetVisibilityEffect?.consume()
            )
            coVerify { moveMessage(userId, messageId, SystemLabelId.Trash) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should close bottom sheet and call use case when moving a message to archive`() = runTest {
        // Given
        val themeOptions = MessageThemeOptionsTestData.darkNoOverride
        val messageId = MessageSample.Invoice.messageId
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.AugWeatherForecast,
                MessageSample.Invoice,
                MessageSample.EmptyDraft
            ),
            MessageSample.AugWeatherForecast.messageId
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery {
            observeMessage(userId, messageId)
        } returns flowOf(MessageSample.Invoice.right())
        coEvery { moveMessage(userId, messageId, SystemLabelId.Archive) } returns Unit.right()
        coEvery {
            getMessageAvailableActions(userId, filterByLocationLabelId, messageId, themeOptions)
        } returns AvailableActionsTestData.replyActionsOnly.right()
        coEvery {
            getMessagesInSameExclusiveLocation(userId, conversationId, messageId, any()) // labelId here is not strict
        } returns listOf<Message>(mockk(), mockk()).right()

        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))

        viewModel.state.test {
            skipItems(4)

            viewModel.submit(
                ConversationDetailViewAction.RequestMessageMoreActionsBottomSheet(
                    messageId, themeOptions, MoreActionsBottomSheetEntryPoint.BottomBar
                )
            )
            skipItems(2)
            viewModel.submit(ConversationDetailViewAction.MoveMessage.System.Archive(messageId))

            // then
            assertEquals(
                BottomSheetVisibilityEffect.Hide, awaitItem().bottomSheetState?.bottomSheetVisibilityEffect?.consume()
            )
            coVerify { moveMessage(userId, messageId, SystemLabelId.Archive) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should close bottom sheet and call use case when moving a message to spam`() = runTest {
        // Given
        val themeOptions = MessageThemeOptionsTestData.darkNoOverride
        val messageId = MessageSample.Invoice.messageId
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.AugWeatherForecast,
                MessageSample.Invoice,
                MessageSample.EmptyDraft
            ),
            MessageSample.AugWeatherForecast.messageId
        )
        val labelId = SystemLabelId.Archive.labelId
        coEvery { observeConversationMessages(userId, any(), labelId) } returns flowOf(messages.right())
        coEvery {
            observeMessage(userId, messageId)
        } returns flowOf(MessageSample.Invoice.right())
        coEvery { moveMessage(userId, messageId, SystemLabelId.Spam) } returns Unit.right()
        coEvery {
            getMessageAvailableActions(userId, labelId, messageId, themeOptions)
        } returns AvailableActionsTestData.replyActionsOnly.right()
        coEvery {
            getMessagesInSameExclusiveLocation(userId, conversationId, messageId, any()) // labelId here is not strict
        } returns listOf<Message>(mockk(), mockk()).right()
        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))

        viewModel.state.test {
            skipItems(4)

            viewModel.submit(
                ConversationDetailViewAction.RequestMessageMoreActionsBottomSheet(
                    messageId, themeOptions, MoreActionsBottomSheetEntryPoint.BottomBar
                )
            )
            skipItems(2)
            viewModel.submit(ConversationDetailViewAction.MoveMessage.System.Spam(messageId))

            // then
            assertEquals(
                BottomSheetVisibilityEffect.Hide, awaitItem().bottomSheetState?.bottomSheetVisibilityEffect?.consume()
            )
            coVerify { moveMessage(userId, messageId, SystemLabelId.Spam) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should request message move to bottom sheet`() = runTest {
        // Given
        val themeOptions = MessageThemeOptionsTestData.darkNoOverride
        val messageId = MessageSample.Invoice.messageId
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.AugWeatherForecast,
                MessageSample.Invoice,
                MessageSample.EmptyDraft
            ),
            MessageSample.AugWeatherForecast.messageId
        )
        val labelId = SystemLabelId.Archive.labelId
        val expectedBottomSheetState = BottomSheetState(
            contentState = MoveToBottomSheetState.Requested(
                userId,
                labelId,
                itemIds = listOf(MoveToItemId(messageId.id)),
                entryPoint = MoveToBottomSheetEntryPoint.Message(isLastInCurrentLocation = false, messageId = messageId)
            ),
            bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
        )
        coEvery { observeConversationMessages(userId, any(), labelId) } returns flowOf(messages.right())
        coEvery {
            observeMessage(userId, messageId)
        } returns flowOf(MessageSample.Invoice.right())
        coEvery {
            getMessageAvailableActions(userId, labelId, messageId, themeOptions)
        } returns AvailableActionsTestData.replyActionsOnly.right()
        coEvery {
            getMessagesInSameExclusiveLocation(userId, conversationId, messageId, any()) // labelId here is not strict
        } returns listOf<Message>(mockk(), mockk()).right()

        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))

        viewModel.state.test {
            skipItems(4)

            viewModel.submit(
                ConversationDetailViewAction.RequestMessageMoreActionsBottomSheet(
                    messageId, themeOptions, MoreActionsBottomSheetEntryPoint.BottomBar
                )
            )
            skipItems(2)
            viewModel.submit(ConversationDetailViewAction.RequestMessageMoveToBottomSheet(messageId))
            advanceUntilIdle()

            // then
            assertEquals(
                BottomSheetVisibilityEffect.Show, awaitItem().bottomSheetState?.bottomSheetVisibilityEffect?.consume()
            )

            assertEquals(expectedBottomSheetState, awaitItem().bottomSheetState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when performing move to trash with success - should stop initial jobs and not restart them`() = runTest {
        // Given
        val expectedEffect = ActionResult.UndoableActionResult(TextUiModel(R.string.conversation_moved_to_trash))
        val messages = nonEmptyListOf(
            ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(3)
        )
        val messageId = MessageId(messages.first().messageId.id)

        coEvery { getDecryptedMessageBody.invoke(userId, any()) } returns DecryptedMessageBody(
            messageId = messageId,
            value = EmailBodyTestSamples.BodyWithoutQuotes,
            mimeType = MimeType.Html,
            isUnread = false,
            hasQuotedText = false,
            hasCalendarInvite = false,
            banners = emptyList(),
            attachments = listOf(
                AttachmentMetadataSamples.Document,
                AttachmentMetadataSamples.DocumentWithReallyLongFileName,
                AttachmentMetadataSamples.Invoice,
                AttachmentMetadataSamples.Image
            )
        ).right()

        coEvery { move.invoke(any(), any(), any<SystemLabelId>()) } returns Unit.right()
        initGenericObserverMocks()
        val viewModel = buildConversationDetailViewModel()

        // When + Then
        viewModel.state.test {
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))
            advanceUntilIdle()

            clearMocks(
                observeConversationUseCase,
                observeConversationViewState,
                observeDetailBottomBarActions,
                observeContacts,
                observePrivacySettings,
                answers = false,
                recordedCalls = true
            )

            viewModel.submit(ConversationDetailViewAction.MoveToTrash)
            advanceUntilIdle()

            verify {
                observeConversationUseCase wasNot Called
                observeConversationViewState wasNot Called
                observeDetailBottomBarActions wasNot Called
                observeContacts wasNot Called
                observeConversationViewState wasNot Called
                observePrivacySettings wasNot Called
            }

            val lastItem = expectMostRecentItem()
            assertEquals(lastItem.exitScreenActionResult.consume(), expectedEffect)
        }
    }

    @Test
    fun `when performing move to trash with failure - should restart observation jobs`() = runTest {
        // Given
        val expectedEffect = TextUiModel(R.string.error_move_to_trash_failed)
        val messages = nonEmptyListOf(
            ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(3)
        )
        val messageId = MessageId(messages.first().messageId.id)

        coEvery { getDecryptedMessageBody.invoke(userId, any()) } returns DecryptedMessageBody(
            messageId = messageId,
            value = EmailBodyTestSamples.BodyWithoutQuotes,
            mimeType = MimeType.Html,
            isUnread = false,
            hasQuotedText = false,
            hasCalendarInvite = false,
            banners = emptyList(),
            attachments = listOf(
                AttachmentMetadataSamples.Document,
                AttachmentMetadataSamples.DocumentWithReallyLongFileName,
                AttachmentMetadataSamples.Invoice,
                AttachmentMetadataSamples.Image
            )
        ).right()

        initGenericObserverMocks()

        val viewModel = buildConversationDetailViewModel()
        coEvery { move.invoke(any(), any(), any<SystemLabelId>()) } returns DataError.Local.Unknown.left()

        // When + Then
        viewModel.state.test {
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))
            advanceUntilIdle()

            clearMocks(
                observeConversationUseCase,
                observeConversationViewState,
                observeDetailBottomBarActions,
                observeContacts,
                observePrivacySettings,
                answers = false,
                recordedCalls = true
            )

            viewModel.submit(ConversationDetailViewAction.MoveToTrash)
            advanceUntilIdle()

            coVerify {
                observeConversationUseCase(userId, conversationId, any())
                observeConversationViewState()
                observePrivacySettings(userId)
            }

            val state = expectMostRecentItem()
            assertEquals(state.exitScreenActionResult, Effect.empty())
            assertEquals(state.exitScreenEffect, Effect.empty())
            assertEquals(state.error.consume(), expectedEffect)
        }
    }

    @Test
    fun `should show confirmation dialog when marking a message as legitimate`() = runTest {
        // Given
        val messageId = MessageSample.Invoice.messageId
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.AugWeatherForecast,
                MessageSample.Invoice,
                MessageSample.EmptyDraft
            ),
            MessageSample.AugWeatherForecast.messageId
        )
        val labelId = SystemLabelId.Archive.labelId
        coEvery { observeConversationMessages(userId, any(), labelId) } returns flowOf(messages.right())
        coEvery { observeMessage(userId, messageId) } returns flowOf(MessageSample.Invoice.right())

        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))

        viewModel.state.test {
            skipItems(4)

            viewModel.submit(ConversationDetailViewAction.MarkMessageAsLegitimate(messageId, isPhishing = true))
            advanceUntilIdle()

            // Then
            assertEquals(
                MarkAsLegitimateDialogState.Shown(messageId, isPhishing = true),
                awaitItem().markAsLegitimateDialogState
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should call use case when marking a message as legitimate is confirmed`() = runTest {
        // Given
        val messageId = MessageSample.Invoice.messageId
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.AugWeatherForecast,
                MessageSample.Invoice,
                MessageSample.EmptyDraft
            ),
            MessageSample.AugWeatherForecast.messageId
        )
        val labelId = SystemLabelId.Archive.labelId
        coEvery { observeConversationMessages(userId, any(), labelId) } returns flowOf(messages.right())
        coEvery { observeMessage(userId, messageId) } returns flowOf(MessageSample.Invoice.right())
        coEvery { markMessageAsLegitimate(userId, messageId) } returns Unit.right()

        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))

        viewModel.state.test {
            skipItems(4)

            viewModel.submit(ConversationDetailViewAction.MarkMessageAsLegitimateConfirmed(messageId))
            advanceUntilIdle()

            // Then
            coVerify { markMessageAsLegitimate(userId, messageId) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should call use case when unblocking sender`() = runTest {
        // Given
        val email = "abc@pm.me"
        val messageId = MessageSample.Invoice.messageId
        val messageIdUiModel = MessageIdUiModel(messageId.id)
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.AugWeatherForecast,
                MessageSample.Invoice,
                MessageSample.EmptyDraft
            ),
            MessageSample.AugWeatherForecast.messageId
        )
        val labelId = SystemLabelId.Archive.labelId
        coEvery { observeConversationMessages(userId, any(), labelId) } returns flowOf(messages.right())
        coEvery { observeMessage(userId, messageId) } returns flowOf(MessageSample.Invoice.right())
        coEvery { unblockSender(userId, email) } returns Unit.right()

        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))

        viewModel.state.test {
            skipItems(4)

            viewModel.submit(ConversationDetailViewAction.UnblockSender(messageIdUiModel, email))
            advanceUntilIdle()

            // Then
            coVerify { unblockSender(userId, email) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should call use case and not exit screen when report phishing in a multiple message conversation`() = runTest {
        // Given
        val messageId = MessageSample.AugWeatherForecast.messageId
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.AugWeatherForecast,
                MessageSample.Invoice,
                MessageSample.EmptyDraft
            ),
            MessageSample.AugWeatherForecast.messageId
        )
        val labelId = SystemLabelId.Archive.labelId
        coEvery { observeConversationMessages(userId, any(), labelId) } returns flowOf(messages.right())
        coEvery { observeMessage(userId, messageId) } returns flowOf(MessageSample.AugWeatherForecast.right())
        coEvery { reportPhishingMessage(userId, messageId) } returns Unit.right()

        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.state.test {
            skipItems(3)
            viewModel.submit(ConversationDetailViewAction.ReportPhishingConfirmed(messageId))
            advanceUntilIdle()

            // Then
            val item = awaitItem()
            assertNull(item.exitScreenEffect.consume())
            coVerify { reportPhishingMessage(userId, messageId) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should call use case and exit screen when report phishing in a single message conversation`() = runTest {
        // Given
        val messageId = MessageSample.AugWeatherForecast.messageId
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.AugWeatherForecast
            ),
            MessageSample.AugWeatherForecast.messageId
        )
        val labelId = SystemLabelId.Archive.labelId
        coEvery { observeConversationMessages(userId, any(), labelId) } returns flowOf(messages.right())
        coEvery { observeMessage(userId, messageId) } returns flowOf(MessageSample.AugWeatherForecast.right())
        coEvery { reportPhishingMessage(userId, messageId) } returns Unit.right()

        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.state.test {
            skipItems(4)
            viewModel.submit(ConversationDetailViewAction.ReportPhishingConfirmed(messageId))
            advanceUntilIdle()

            // Then
            val item = awaitItem()
            assertEquals(Unit, item.exitScreenEffect.consume())
            coVerify { reportPhishingMessage(userId, messageId) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should call print message for the given messageId`() = runTest {
        // Given
        val messages = nonEmptyListOf(
            ConversationDetailMessageUiModelSample.invoiceExpandedWithAttachments(3)
        )
        val messageId = MessageId(messages.first().messageId.id)
        every { printMessage(any(), any(), any(), any(), any(), any()) } just runs

        coEvery { getDecryptedMessageBody.invoke(userId, any()) } returns DecryptedMessageBody(
            messageId = messageId,
            value = EmailBodyTestSamples.BodyWithoutQuotes,
            mimeType = MimeType.Html,
            isUnread = false,
            hasQuotedText = false,
            hasCalendarInvite = false,
            banners = emptyList(),
            attachments = listOf(
                AttachmentMetadataSamples.Document,
                AttachmentMetadataSamples.DocumentWithReallyLongFileName,
                AttachmentMetadataSamples.Invoice,
                AttachmentMetadataSamples.Image
            )
        ).right()

        val viewModel = buildConversationDetailViewModel()
        viewModel.state.test {
            skipItems(3)

            // When
            val newState = awaitItem().messagesState as ConversationDetailsMessagesState.Data

            // Then
            val collapsedMessage = newState.messages.first { it.messageId == messages.first().messageId }
            assertIs<Collapsed>(collapsedMessage)

            // When
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))
            advanceUntilIdle()

            // Then
            val newExpandedState = awaitItem().messagesState as ConversationDetailsMessagesState.Data
            val expandedMessage = newExpandedState.messages.first { it.messageId == messages.first().messageId }
            assertIs<Expanded>(expandedMessage)
            assertEquals(
                messages.first().messageBodyUiModel.attachments,
                expandedMessage.messageBodyUiModel.attachments
            )
            assertFalse(expandedMessage.messageBodyUiModel.shouldShowExpandCollapseButton)

            viewModel.submit(ConversationDetailViewAction.PrintMessage(context, messageId))
            val newItem = awaitItem()
            val conversationState = newItem.conversationState as ConversationDetailMetadataState.Data
            val messageState = newItem.messagesState as ConversationDetailsMessagesState.Data

            val message = messageState.messages.first() as Expanded

            verify {
                printMessage(
                    context,
                    conversationState.conversationUiModel.subject,
                    message.messageDetailHeaderUiModel,
                    message.messageBodyUiModel,
                    any(),
                    PrintConfiguration(
                        showRemoteContent = !message.messageBodyUiModel.shouldShowRemoteContentBanner,
                        showEmbeddedImages = !message.messageBodyUiModel.shouldShowEmbeddedImagesBanner
                    )
                )
            }
        }
    }

    @Test
    fun `should get rsvp event when the message contains a calendar invite`() = runTest {
        // Given
        val message = MessageSample.AugWeatherForecast
        val messages = ConversationMessages(
            nonEmptyListOf(
                message
            ),
            message.messageId
        )
        val rsvpEvent = RsvpEvent(
            eventId = EventId("id"),
            summary = "summary",
            location = "location",
            description = "description",
            recurrence = "recurrence",
            startsAt = 123L,
            endsAt = 124L,
            occurrence = RsvpOccurrence.Date,
            organizer = RsvpOrganizer("organizerName", "organizerEmail"),
            attendees = listOf(RsvpAttendee("attendeeName", "attendeeEmail", RsvpAttendeeStatus.Yes)),
            userAttendeeIdx = 0,
            calendar = null,
            state = RsvpState.CancelledReminder
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery { getDecryptedMessageBody.invoke(any(), any()) } returns DecryptedMessageBody(
            messageId = message.messageId,
            value = "",
            mimeType = MimeType.Html,
            isUnread = false,
            hasQuotedText = false,
            hasCalendarInvite = true,
            banners = emptyList()
        ).right()
        coEvery { getRsvpEvent(userId, message.messageId) } returns rsvpEvent.right()

        val viewModel = buildConversationDetailViewModel()

        // When
        viewModel.state.test {

            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(message.messageId)))
            advanceUntilIdle()

            // Then
            coVerify { getRsvpEvent(userId, message.messageId) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should get rsvp event when retry button is clicked`() = runTest {
        // Given
        val message = MessageSample.AugWeatherForecast
        val messages = ConversationMessages(
            nonEmptyListOf(
                message
            ),
            message.messageId
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery { getDecryptedMessageBody.invoke(any(), any()) } returns DecryptedMessageBody(
            messageId = message.messageId,
            value = "",
            mimeType = MimeType.Html,
            isUnread = false,
            hasQuotedText = false,
            hasCalendarInvite = true,
            banners = emptyList()
        ).right()
        coEvery { getRsvpEvent(userId, message.messageId) } returns DataError.Local.NoDataCached.left()

        val viewModel = buildConversationDetailViewModel()

        // When
        viewModel.state.test {
            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(message.messageId)))
            advanceUntilIdle()

            viewModel.submit(ConversationDetailViewAction.RetryRsvpEventLoading(message.messageId))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 2) { getRsvpEvent(userId, message.messageId) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should answer rsvp event when answer rsvp event action is submitted`() = runTest {
        // Given
        val message = MessageSample.AugWeatherForecast
        val messages = ConversationMessages(
            nonEmptyListOf(
                message
            ),
            message.messageId
        )
        val rsvpEvent = RsvpEvent(
            eventId = EventId("id"),
            summary = "summary",
            location = "location",
            description = "description",
            recurrence = "recurrence",
            startsAt = 123L,
            endsAt = 124L,
            occurrence = RsvpOccurrence.Date,
            organizer = RsvpOrganizer("organizerName", "organizerEmail"),
            attendees = listOf(RsvpAttendee("attendeeName", "attendeeEmail", RsvpAttendeeStatus.Yes)),
            userAttendeeIdx = 0,
            calendar = null,
            state = RsvpState.CancelledReminder
        )
        coEvery { observeConversationMessages(userId, any(), any()) } returns flowOf(messages.right())
        coEvery { getDecryptedMessageBody.invoke(any(), any()) } returns DecryptedMessageBody(
            messageId = message.messageId,
            value = "",
            mimeType = MimeType.Html,
            isUnread = false,
            hasQuotedText = false,
            hasCalendarInvite = true,
            banners = emptyList()
        ).right()
        coEvery { getRsvpEvent(userId, message.messageId) } returns rsvpEvent.right()
        coEvery { answerRsvpEvent(userId, message.messageId, RsvpAnswer.Yes) } returns Unit.right()

        val viewModel = buildConversationDetailViewModel()

        // When
        viewModel.state.test {

            viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(message.messageId)))
            advanceUntilIdle()

            viewModel.submit(ConversationDetailViewAction.AnswerRsvpEvent(message.messageId, RsvpAnswer.Yes))
            advanceUntilIdle()

            // Then
            coVerify { answerRsvpEvent(userId, message.messageId, RsvpAnswer.Yes) }
            coVerify(exactly = 2) { getRsvpEvent(userId, message.messageId) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should call use case when unsubscribing from newsletter`() = runTest {
        // Given
        val messageId = MessageSample.Invoice.messageId
        val messages = ConversationMessages(
            nonEmptyListOf(
                MessageSample.AugWeatherForecast,
                MessageSample.Invoice,
                MessageSample.EmptyDraft
            ),
            MessageSample.AugWeatherForecast.messageId
        )
        val labelId = SystemLabelId.Archive.labelId
        coEvery { observeConversationMessages(userId, any(), labelId) } returns flowOf(messages.right())
        coEvery { observeMessage(userId, messageId) } returns flowOf(MessageSample.Invoice.right())
        coEvery { unsubscribeFromNewsletter(userId, messageId) } returns Unit.right()

        // When
        val viewModel = buildConversationDetailViewModel()

        viewModel.submit(ExpandMessage(messageIdUiModelMapper.toUiModel(messageId)))

        viewModel.state.test {
            skipItems(4)

            viewModel.submit(ConversationDetailViewAction.UnsubscribeFromNewsletter(messageId))
            advanceUntilIdle()

            // Then
            coVerify { unsubscribeFromNewsletter(userId, messageId) }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Suppress("LongParameterList")
    private fun buildConversationDetailViewModel(
        observePrimaryUser: ObservePrimaryUserId = observePrimaryUserId,
        actionMapper: ActionUiModelMapper = actionUiModelMapper,
        messageMapper: ConversationDetailMessageUiModelMapper = conversationMessageMapper,
        metadataMapper: ConversationDetailMetadataUiModelMapper = conversationMetadataMapper,
        read: MarkConversationAsRead = markConversationAsRead,
        unread: MarkConversationAsUnread = markConversationAsUnread,
        moveConversation: MoveConversation = move,
        delete: DeleteConversations = deleteConversations,
        report: ReportPhishingMessage = reportPhishingMessage,
        observeConversation: ObserveConversation = observeConversationUseCase,
        observeConversationMessages: ObserveConversationMessages = this.observeConversationMessages,
        observeDetailActions: ObserveDetailBottomBarActions = observeDetailBottomBarActions,
        getAttachmentStatus: GetDownloadingAttachmentsForMessages = getDownloadingAttachmentsForMessages,
        detailReducer: ConversationDetailReducer = reducer,
        savedState: SavedStateHandle = savedStateHandle,
        starMsg: StarMessages = starMessages,
        unStarMsg: UnStarMessages = unStarMessages,
        star: StarConversations = starConversations,
        unStar: UnStarConversations = unStarConversations,
        decryptedMessageBody: GetMessageBodyWithClickableLinks = getDecryptedMessageBody,
        markMessageAndConversationRead: MarkMessageAsRead = markMessageAsRead,
        getIntentValues: GetAttachmentIntentValues = getAttachmentIntentValues,
        ioDispatcher: CoroutineDispatcher = testDispatcher,
        protonCalendarInstalled: IsProtonCalendarInstalled = isProtonCalendarInstalled,
        findContactByEmailAddress: FindContactByEmail = findContactByEmail,
        loadAvatarImg: LoadAvatarImage = loadAvatarImage,
        observeAvatarImgStates: ObserveAvatarImageStates = observeAvatarImageStates
    ) = ConversationDetailViewModel(
        observePrimaryUserId = observePrimaryUser,
        messageIdUiModelMapper = messageIdUiModelMapper,
        actionUiModelMapper = actionMapper,
        conversationMessageMapper = messageMapper,
        conversationMetadataMapper = metadataMapper,
        markConversationAsRead = read,
        markConversationAsUnread = unread,
        moveConversation = moveConversation,
        deleteConversations = delete,
        observeConversation = observeConversation,
        observeConversationMessages = observeConversationMessages,
        observeDetailActions = observeDetailActions,
        getDownloadingAttachmentsForMessages = getAttachmentStatus,
        reducer = detailReducer,
        starConversations = star,
        unStarConversations = unStar,
        starMessages = starMsg,
        unStarMessages = unStarMsg,
        savedStateHandle = savedState,
        getMessageBodyWithClickableLinks = decryptedMessageBody,
        markMessageAsRead = markMessageAndConversationRead,
        messageViewStateCache = messageViewStateCache,
        observeConversationViewState = observeConversationViewState,
        getAttachmentIntentValues = getIntentValues,
        loadImageAvoidDuplicatedExecution = loadImageAvoidDuplicatedExecution,
        ioDispatcher = ioDispatcher,
        observePrivacySettings = observePrivacySettings,
        updateLinkConfirmationSetting = updateLinkConfirmationSetting,
        reportPhishingMessage = report,
        isProtonCalendarInstalled = protonCalendarInstalled,
        markMessageAsUnread = markMessageAsUnread,
        findContactByEmail = findContactByEmailAddress,
        getMoreActionsBottomSheetData = getMoreActionsBottomSheetData,
        moveMessage = moveMessage,
        deleteMessages = deleteMessages,
        observePrimaryUserAddress = observePrimaryUserAddress,
        loadAvatarImage = loadAvatarImg,
        observeAvatarImageStates = observeAvatarImgStates,
        getMessagesInSameExclusiveLocation = getMessagesInSameExclusiveLocation,
        markMessageAsLegitimate = markMessageAsLegitimate,
        unblockSender = unblockSender,
        cancelScheduleSendMessage = cancelScheduleSendMessage,
        printMessage = printMessage,
        getRsvpEvent = getRsvpEvent,
        answerRsvpEvent = answerRsvpEvent,
        snoozeRepository = snoozeRepository,
        unsubscribeFromNewsletter = unsubscribeFromNewsletter,
        toolbarRefreshSignal = toolbarRefreshSignal
    )

    private fun aMessageAttachment(id: String): AttachmentMetadata = AttachmentMetadata(
        attachmentId = AttachmentId(id),
        name = "name",
        size = 0,
        mimeType = AttachmentMimeType(
            mime = "application/pdf",
            category = MimeTypeCategory.Pdf
        ),
        disposition = AttachmentDisposition.Attachment,
        includeInPreview = true
    )

    private suspend fun ReceiveTurbine<ConversationDetailState>.lastEmittedItem(): ConversationDetailState {
        val events = cancelAndConsumeRemainingEvents()
        return (events.last() as Event.Item).value
    }

    private fun initGenericObserverMocks() {
        coEvery {
            observeContacts(userId = UserIdSample.Primary)
        } returns flowOf(emptyList<ContactMetadata.Contact>().right())
        coEvery {
            observeConversationUseCase(
                UserIdSample.Primary, ConversationIdSample.WeatherForecast, any()
            )
        } returns flowOf(ConversationSample.WeatherForecast.right())
        coEvery {
            observeDetailBottomBarActions(
                UserIdSample.Primary,
                any(),
                ConversationIdSample.WeatherForecast
            )
        } returns flowOf(
            listOf(Action.Archive, Action.MarkUnread).right()
        )
        coEvery { observePrivacySettings.invoke(any()) } returns flowOf(
            PrivacySettings(
                autoShowRemoteContent = false,
                autoShowEmbeddedImages = false,
                preventTakingScreenshots = false,
                requestLinkConfirmation = false,
                allowBackgroundSync = false
            ).right()
        )
    }
}
