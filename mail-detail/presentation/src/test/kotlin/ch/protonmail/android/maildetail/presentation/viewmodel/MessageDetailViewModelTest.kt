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

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Event
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcontact.domain.usecase.FindContactByEmail
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues.OpenIcsInProtonCalendar
import ch.protonmail.android.maildetail.domain.model.OpenProtonCalendarIntentValues.OpenProtonCalendarOnPlayStore
import ch.protonmail.android.maildetail.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.maildetail.domain.usecase.GetDetailBottomSheetActions
import ch.protonmail.android.maildetail.domain.usecase.GetDownloadingAttachmentsForMessages
import ch.protonmail.android.maildetail.domain.usecase.IsProtonCalendarInstalled
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsRead
import ch.protonmail.android.maildetail.domain.usecase.MarkMessageAsUnread
import ch.protonmail.android.maildetail.domain.usecase.MoveMessage
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageAttachmentStatus
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.ReportPhishingMessage
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.mapper.MessageBannersUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageBodyUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailActionBarUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailFooterUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailHeaderUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.MessageBannersState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageDetailActionBarUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageMetadataState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.maildetail.presentation.model.ReportPhishingDialogState
import ch.protonmail.android.maildetail.presentation.reducer.MessageBannersReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageBodyReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageCustomizeToolbarSpotlightReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageDeleteDialogReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageDetailMetadataReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageDetailReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageReportPhishingDialogReducer
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.maildetail.presentation.usecase.ExtractMessageBodyWithoutQuote
import ch.protonmail.android.maildetail.presentation.usecase.GetEmbeddedImageAvoidDuplicatedExecution
import ch.protonmail.android.maildetail.presentation.usecase.LoadDataForMessageLabelAsBottomSheet
import ch.protonmail.android.maildetail.presentation.usecase.OnMessageLabelAsConfirmed
import ch.protonmail.android.maildetail.presentation.usecase.PrintMessage
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.mapper.MailLabelTextMapper
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.maillabel.presentation.toUiModel
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.GetDecryptedMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import ch.protonmail.android.mailmessage.domain.usecase.GetDecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantNameResult
import ch.protonmail.android.mailmessage.domain.usecase.StarMessages
import ch.protonmail.android.mailmessage.domain.usecase.UnStarMessages
import ch.protonmail.android.mailmessage.presentation.mapper.DetailMoreActionsBottomSheetUiMapper
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyWithType
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.BottomSheetVisibilityEffect
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ContactActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.DetailMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.ContactActionsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.DetailMoreActionsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.LabelAsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.MailboxMoreActionsBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.MoveToBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.reducer.UpsellingBottomSheetReducer
import ch.protonmail.android.mailmessage.presentation.usecase.InjectCssIntoDecryptedMessageBody
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.model.PrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveAutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.usecase.ObserveCustomizeToolbarSpotlight
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.UpdateCustomizeToolbarSpotlight
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObservePrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateLinkConfirmationSetting
import ch.protonmail.android.testdata.action.ActionUiModelTestData
import ch.protonmail.android.testdata.contact.ContactSample
import ch.protonmail.android.testdata.contact.ContactTestData
import ch.protonmail.android.testdata.maildetail.MessageBannersUiModelTestData.messageBannersUiModel
import ch.protonmail.android.testdata.maildetail.MessageDetailFooterUiModelTestData.messageDetailFooterUiModel
import ch.protonmail.android.testdata.maildetail.MessageDetailHeaderUiModelTestData.messageDetailHeaderUiModel
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelTestData.buildCustomFolder
import ch.protonmail.android.testdata.message.MessageAttachmentMetadataTestData
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import ch.protonmail.android.testdata.message.MessageDetailActionBarUiModelTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageDetailViewModelTest {

    private val messageId = MessageId("detailMessageId")
    private val decryptedMessageBody = DecryptedMessageBody(
        messageId = messageId,
        value = "Decrypted message body.",
        mimeType = MimeType.PlainText,
        attachments = emptyList(),
        userAddress = UserAddressSample.PrimaryAddress
    )
    private val decryptedHtmlMessageBody = DecryptedMessageBody(
        messageId = messageId,
        value = EmailBodyTestSamples.BodyWithoutQuotes,
        mimeType = MimeType.PlainText,
        attachments = listOf(
            MessageAttachmentSample.invoice,
            MessageAttachmentSample.document,
            MessageAttachmentSample.documentWithMultipleDots,
            MessageAttachmentSample.image
        ),
        userAddress = UserAddressSample.PrimaryAddress
    )
    private val extractMessageBodyWithoutQuote = ExtractMessageBodyWithoutQuote()
    private val actionUiModelMapper = ActionUiModelMapper()
    private val messageDetailActionBarUiModelMapper = MessageDetailActionBarUiModelMapper()
    private val defaultFolderColorSettings = FolderColorSettings()
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(userId)
    }

    private val observeContacts = mockk<ObserveContacts> {
        every { this@mockk.invoke(userId) } returns flowOf(Either.Right(ContactTestData.contacts))
    }
    private val observeMessage = mockk<ObserveMessage>()
    private val observeMessageWithLabels = mockk<ObserveMessageWithLabels> {
        every { this@mockk.invoke(userId, any()) } returns flowOf(
            MessageWithLabels(
                MessageTestData.message,
                emptyList()
            ).right()
        )
    }

    private val getDecryptedMessageBody = mockk<GetDecryptedMessageBody> {
        coEvery { this@mockk(userId, any()) } returns decryptedMessageBody.right()
    }
    private val savedStateHandle = mockk<SavedStateHandle> {
        every { this@mockk.get<String>(MessageDetailScreen.MESSAGE_ID_KEY) } returns messageId.id
    }
    private val observeDetailActions = mockk<ObserveMessageDetailActions> {
        every { this@mockk.invoke(userId, messageId) } returns flowOf(
            nonEmptyListOf(Action.Archive, Action.MarkUnread).right()
        )
    }
    private val observeMailLabels = mockk<ObserveExclusiveDestinationMailLabels> {
        every { this@mockk.invoke(userId) } returns flowOf(
            MailLabels(
                systemLabels = listOf(MailLabel.System(MailLabelId.System.Spam)),
                folders = listOf(buildCustomFolder(id = "folder1")),
                labels = listOf()
            )
        )
    }
    private val observeFolderColorSettings =
        mockk<ObserveFolderColorSettings> {
            every { this@mockk.invoke(userId) } returns flowOf(defaultFolderColorSettings)
        }
    private val observeAutoDeleteSetting = mockk<ObserveAutoDeleteSetting> {
        coEvery { this@mockk() } returns flowOf(AutoDeleteSetting.Disabled)
    }
    private val observeAttachmentWorkerStatus = mockk<ObserveMessageAttachmentStatus>()
    private val markUnread = mockk<MarkMessageAsUnread> {
        coEvery { this@mockk(userId, messageId) } returns MessageSample.Invoice.right()
    }
    private val markRead = mockk<MarkMessageAsRead> {
        coEvery { this@mockk(userId, messageId) } returns MessageSample.Invoice.right()
    }
    private val getContacts = mockk<GetContacts> {
        coEvery { this@mockk.invoke(userId) } returns ContactTestData.contacts.right()
    }
    private val starMessages = mockk<StarMessages> {
        coEvery { this@mockk.invoke(userId, listOf(messageId)) } returns listOf(MessageTestData.starredMessage).right()
    }
    private val unStarMessages = mockk<UnStarMessages> {
        coEvery { this@mockk.invoke(userId, listOf(messageId)) } returns listOf(MessageTestData.message).right()
    }
    private val messageDetailHeaderUiModelMapper = mockk<MessageDetailHeaderUiModelMapper> {
        coEvery {
            toUiModel(
                any(),
                ContactTestData.contacts,
                defaultFolderColorSettings,
                AutoDeleteSetting.Disabled
            )
        } returns messageDetailHeaderUiModel
    }
    private val messageDetailFooterUiModelMapper = mockk<MessageDetailFooterUiModelMapper> {
        every {
            toUiModel(any())
        } returns messageDetailFooterUiModel
    }
    private val messageBannersUiModelMapper = mockk<MessageBannersUiModelMapper> {
        every { createMessageBannersUiModel(any()) } returns messageBannersUiModel
    }
    private val messageBodyUiModelMapper = mockk<MessageBodyUiModelMapper> {
        coEvery {
            toUiModel(userId, decryptedMessageBody)
        } returns MessageBodyUiModelTestData.plainTextMessageBodyUiModel
        every {
            toUiModel(
                GetDecryptedMessageBodyError.Decryption(messageId, MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY)
            )
        } returns MessageBodyUiModelTestData.plainTextMessageBodyUiModel
    }
    private val moveMessage: MoveMessage = mockk {
        coEvery {
            this@mockk.invoke(
                userId = userId,
                messageId = messageId,
                labelId = SystemLabelId.Trash.labelId
            )
        } returns Unit.right()
        coEvery {
            this@mockk.invoke(
                userId = userId,
                messageId = messageId,
                labelId = SystemLabelId.Archive.labelId
            )
        } returns Unit.right()
        coEvery {
            this@mockk.invoke(
                userId = userId,
                messageId = messageId,
                labelId = SystemLabelId.Spam.labelId
            )
        } returns Unit.right()
    }
    private val resolveParticipantName = mockk<ResolveParticipantName> {
        every { this@mockk(any(), any()) } returns ResolveParticipantNameResult("Sender", isProton = false)
    }

    // Privacy settings for link confirmation dialog
    private val observePrivacySettings = mockk<ObservePrivacySettings> {
        coEvery { this@mockk.invoke(userId) } returns flowOf(
            PrivacySettings(
                autoShowRemoteContent = false,
                autoShowEmbeddedImages = false,
                preventTakingScreenshots = false,
                requestLinkConfirmation = false,
                allowBackgroundSync = false
            ).right()
        )
    }
    private val findContactByEmail: FindContactByEmail = mockk<FindContactByEmail> {
        coEvery { this@mockk.invoke(any(), any()) } returns ContactSample.Stefano
    }

    private val updateLinkConfirmationSetting = mockk<UpdateLinkConfirmationSetting>()

    private val getAttachmentIntentValues = mockk<GetAttachmentIntentValues>()
    private val getDownloadingAttachmentsForMessages = mockk<GetDownloadingAttachmentsForMessages>()
    private val getEmbeddedImageAvoidDuplicatedExecution = mockk<GetEmbeddedImageAvoidDuplicatedExecution>()
    private val deleteMessages = mockk<DeleteMessages>()
    private val reportPhishingMessage = mockk<ReportPhishingMessage>()
    private val isProtonCalendarInstalled = mockk<IsProtonCalendarInstalled>()
    private val networkManager = mockk<NetworkManager>()
    private val injectCssIntoDecryptedMessageBody = mockk<InjectCssIntoDecryptedMessageBody> {
        every {
            this@mockk.invoke(
                MessageBodyWithType(
                    MessageBodyUiModelTestData.plainTextMessageBodyUiModel.messageBody,
                    MessageBodyUiModelTestData.plainTextMessageBodyUiModel.mimeType
                ),
                ViewModePreference.LightMode
            )
        } returns MessageBodyUiModelTestData.plainTextMessageBodyUiModel.messageBody
    }
    private val printMessage = mockk<PrintMessage>()
    private val loadDataForMessageLabelAsBottomSheet = mockk<LoadDataForMessageLabelAsBottomSheet> {
        coEvery {
            this@mockk.invoke(userId, messageId)
        } returns LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
            customLabelList = listOf(
                MailLabelTestData.customLabelOne, MailLabelTestData.customLabelTwo
            ).map {
                it.toUiModel(
                    FolderColorSettings(), emptyMap(), MailLabelTestData.customLabelOne.id
                ) as MailLabelUiModel.Custom
            }.toImmutableList(),
            selectedLabels = emptyList<LabelId>().toImmutableList(),
            entryPoint = LabelAsBottomSheetEntryPoint.Message(messageId)
        )
    }
    private val onMessageLabelAsConfirmed = mockk<OnMessageLabelAsConfirmed>()

    private val observeCustomizeToolbarSpotlight = mockk<ObserveCustomizeToolbarSpotlight> {
        every { this@mockk.invoke() } returns flowOf()
    }
    private val updateCustomizeToolbarSpotlight = mockk<UpdateCustomizeToolbarSpotlight>()

    private val getBottomSheetActions = mockk<GetDetailBottomSheetActions> {
        every { this@mockk.invoke(any()) } returns listOf(Action.Archive)
    }

    private val messageDetailReducer = MessageDetailReducer(
        MessageDetailMetadataReducer(
            messageDetailActionBarUiModelMapper,
            messageDetailHeaderUiModelMapper,
            messageDetailFooterUiModelMapper
        ),
        MessageBannersReducer(messageBannersUiModelMapper),
        MessageBodyReducer(injectCssIntoDecryptedMessageBody),
        BottomBarReducer(),
        BottomSheetReducer(
            MoveToBottomSheetReducer(),
            LabelAsBottomSheetReducer(),
            MailboxMoreActionsBottomSheetReducer(),
            DetailMoreActionsBottomSheetReducer(DetailMoreActionsBottomSheetUiMapper()),
            ContactActionsBottomSheetReducer(),
            UpsellingBottomSheetReducer()
        ),
        MessageDeleteDialogReducer(),
        MessageReportPhishingDialogReducer(),
        MailLabelTextMapper(mockk()),
        MessageCustomizeToolbarSpotlightReducer()
    )

    private val viewModel by lazy {
        MessageDetailViewModel(
            observePrimaryUserId = observePrimaryUserId,
            observeMessageWithLabels = observeMessageWithLabels,
            getDecryptedMessageBody = getDecryptedMessageBody,
            messageDetailReducer = messageDetailReducer,
            actionUiModelMapper = actionUiModelMapper,
            observeContacts = observeContacts,
            observeDetailActions = observeDetailActions,
            observeDestinationMailLabels = observeMailLabels,
            observeFolderColor = observeFolderColorSettings,
            observeAutoDeleteSetting = observeAutoDeleteSetting,
            observeMessage = observeMessage,
            observeMessageAttachmentStatus = observeAttachmentWorkerStatus,
            markUnread = markUnread,
            markRead = markRead,
            getContacts = getContacts,
            starMessages = starMessages,
            unStarMessages = unStarMessages,
            savedStateHandle = savedStateHandle,
            messageBodyUiModelMapper = messageBodyUiModelMapper,
            moveMessage = moveMessage,
            deleteMessages = deleteMessages,
            getAttachmentIntentValues = getAttachmentIntentValues,
            getDownloadingAttachmentsForMessages = getDownloadingAttachmentsForMessages,
            getEmbeddedImageAvoidDuplicatedExecution = getEmbeddedImageAvoidDuplicatedExecution,
            observePrivacySettings = observePrivacySettings,
            updateLinkConfirmationSetting = updateLinkConfirmationSetting,
            resolveParticipantName = resolveParticipantName,
            reportPhishingMessage = reportPhishingMessage,
            isProtonCalendarInstalled = isProtonCalendarInstalled,
            networkManager = networkManager,
            printMessage = printMessage,
            findContactByEmail = findContactByEmail,
            loadDataForMessageLabelAsBottomSheet = loadDataForMessageLabelAsBottomSheet,
            onMessageLabelAsConfirmed = onMessageLabelAsConfirmed,
            observeCustomizeToolbarSpotlight = observeCustomizeToolbarSpotlight,
            updateCustomizeToolbarSpotlight = updateCustomizeToolbarSpotlight,
            getBottomSheetActions = getBottomSheetActions
        )
    }
    private val testDispatcher: TestDispatcher by lazy { StandardTestDispatcher() }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Uri::class)
    }

    @Test
    fun `initial state is loading`() = runTest {
        // When
        viewModel.state.test {
            // Then
            assertEquals(MessageDetailState.Loading, awaitItem())
        }
    }

    @Test
    fun `no message state is emitted when there is no primary user`() = runTest {
        // Given
        givenNoLoggedInUser()

        // When
        viewModel.state.test {
            initialStateEmitted()
        }
    }

    @Test
    fun `throws exception when message id parameter was not provided as input`() = runTest {
        // Given
        every { savedStateHandle.get<String>(MessageDetailScreen.MESSAGE_ID_KEY) } returns null

        // Then
        val thrown = assertThrows(IllegalStateException::class.java) { viewModel.state }
        // Then
        assertEquals("No Message id given", thrown.message)
    }

    @Test
    fun `message state is data when use case returns message metadata`() = runTest {
        // Given
        val subject = "message subject"
        val isStarred = true
        val cachedMessage = MessageTestData.buildMessage(
            userId = userId,
            id = messageId.id,
            subject = subject,
            labelIds = listOf(SystemLabelId.Starred.labelId.id)
        )
        val messageWithLabels = MessageWithLabels(cachedMessage, emptyList())
        every { observeMessageWithLabels.invoke(userId, messageId) } returns flowOf(messageWithLabels.right())

        // When
        viewModel.state.test {
            initialStateEmitted()
            messageBodyEmitted()
            // Then
            val expected = MessageMetadataState.Data(
                MessageDetailActionBarUiModel(
                    subject,
                    isStarred
                ),
                messageDetailHeaderUiModel,
                messageDetailFooterUiModel
            )
            assertEquals(expected, awaitItem().messageMetadataState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is data when use case returns body`() = runTest {
        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            val expected = MessageBodyState.Data(
                MessageBodyUiModelTestData.plainTextMessageBodyUiModel
            )
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is error when use case returns error`() = runTest {
        // Given
        coEvery {
            getDecryptedMessageBody(userId, messageId)
        } returns GetDecryptedMessageBodyError.Data(DataError.Local.NoDataCached).left()

        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            val expected = MessageBodyState.Error.Data(isNetworkError = false)
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is network error when use case returns network error`() = runTest {
        // Given
        coEvery {
            getDecryptedMessageBody(userId, messageId)
        } returns GetDecryptedMessageBodyError.Data(DataError.Remote.Http(NetworkError.NoNetwork)).left()

        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            val expected = MessageBodyState.Error.Data(isNetworkError = true)
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is loading when reload action was triggered`() = runTest {
        // Given
        coEvery {
            getDecryptedMessageBody(userId, messageId)
        } returns GetDecryptedMessageBodyError.Data(DataError.Local.NoDataCached).left()

        // When
        viewModel.state.test {
            initialStateEmitted()
            messageBodyLoadingErrorEmitted()

            viewModel.submit(MessageViewAction.Reload)

            // Then
            assertEquals(MessageBodyState.Loading, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message body state is decryption error when use case returns decryption error`() = runTest {
        // Given
        coEvery { getDecryptedMessageBody(userId, messageId) } returns GetDecryptedMessageBodyError.Decryption(
            messageId, MessageBodyTestData.RAW_ENCRYPTED_MESSAGE_BODY
        ).left()

        // When
        viewModel.state.test {
            initialStateEmitted()

            // Then
            val expected = MessageBodyState.Error.Decryption(
                MessageBodyUiModelTestData.plainTextMessageBodyUiModel
            )
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bottomBar state is data when use case returns actions`() = runTest {
        // Given
        val cachedMessage = MessageTestData.buildMessage(
            userId = userId,
            id = messageId.id,
            subject = "message subject",
            labelIds = listOf(SystemLabelId.Starred.labelId.id)
        )
        val messageWithLabels = MessageWithLabels(cachedMessage, emptyList())
        every { observeMessageWithLabels.invoke(userId, messageId) } returns flowOf(messageWithLabels.right())
        every { observeDetailActions.invoke(userId, messageId) } returns flowOf(
            nonEmptyListOf(Action.Archive).right()
        )

        // When
        viewModel.state.test {
            advanceUntilIdle()
            // Then
            val actionUiModels = listOf(ActionUiModelTestData.archive).toImmutableList()
            val expected = BottomBarState.Data.Shown(actionUiModels)
            assertEquals(expected, lastEmittedItem().bottomBarState)
        }
    }

    @Test
    fun `bottomBar state is failed loading actions when use case returns no actions`() = runTest {
        // Given
        val cachedMessage = MessageTestData.buildMessage(
            userId = userId,
            id = messageId.id,
            subject = "message subject",
            labelIds = listOf(SystemLabelId.Starred.labelId.id)
        )
        val messageWithLabels = MessageWithLabels(cachedMessage, emptyList())
        every { observeMessageWithLabels.invoke(userId, messageId) } returns flowOf(messageWithLabels.right())

        every { observeDetailActions.invoke(userId, messageId) } returns
            flowOf(DataError.Local.NoDataCached.left())

        // When
        viewModel.state.test {
            advanceUntilIdle()
            // Then
            val expected = BottomBarState.Error.FailedLoadingActions
            assertEquals(expected, lastEmittedItem().bottomBarState)
        }
    }

    @Test
    fun `message detail state is dismiss message screen when mark unread is successful`() = runTest {
        // Given
        coEvery { markUnread(userId, messageId) } returns MessageSample.Invoice.right()

        viewModel.state.test {
            initialStateEmitted()
            // When
            viewModel.submit(MessageViewAction.MarkUnread)
            advanceUntilIdle()
            // Then
            assertNotNull(lastEmittedItem().exitScreenEffect.consume())
        }
    }

    @Test
    fun `message detail state is error marking unread when mark unread fails`() = runTest {
        // Given
        coEvery { markUnread(userId, messageId) } returns DataError.Local.NoDataCached.left()

        viewModel.state.test {
            // When
            viewModel.submit(MessageViewAction.MarkUnread)
            advanceUntilIdle()
            // Then
            assertEquals(TextUiModel(R.string.error_mark_unread_failed), lastEmittedItem().error.consume())
        }
    }

    @Test
    fun `starred message metadata is emitted when star action is successful`() = runTest {
        viewModel.state.test {
            advanceUntilIdle()
            // When
            viewModel.submit(MessageViewAction.Star)
            advanceUntilIdle()
            // Then
            val actual = assertIs<MessageMetadataState.Data>(lastEmittedItem().messageMetadataState)
            assertTrue(actual.messageDetailActionBar.isStarred)
        }
    }

    @Test
    fun `error starring message is emitted when star action fails`() = runTest {
        // Given
        coEvery { starMessages.invoke(userId, listOf(messageId)) } returns DataError.Local.NoDataCached.left()

        viewModel.state.test {
            initialStateEmitted()
            // When
            viewModel.submit(MessageViewAction.Star)
            advanceUntilIdle()
            // Then
            assertEquals(TextUiModel(R.string.error_star_operation_failed), lastEmittedItem().error.consume())
        }
    }

    @Test
    fun `unStarred message metadata is emitted when unStar action is successful`() = runTest {
        // Given
        val messageWithLabels = MessageWithLabels(MessageTestData.starredMessage, emptyList())
        every { observeMessageWithLabels.invoke(userId, messageId) } returns flowOf(messageWithLabels.right())

        viewModel.state.test {
            advanceUntilIdle()
            // When
            viewModel.submit(MessageViewAction.UnStar)
            advanceUntilIdle()
            // Then
            val actual = assertIs<MessageMetadataState.Data>(lastEmittedItem().messageMetadataState)
            assertFalse(actual.messageDetailActionBar.isStarred)
        }
    }

    @Test
    fun `error unStarring message is emitted when unStar action fails`() = runTest {
        // Given
        coEvery { unStarMessages.invoke(userId, listOf(messageId)) } returns DataError.Local.NoDataCached.left()

        viewModel.state.test {
            initialStateEmitted()
            // When
            viewModel.submit(MessageViewAction.UnStar)
            advanceUntilIdle()
            // Then
            assertEquals(TextUiModel(R.string.error_unstar_operation_failed), lastEmittedItem().error.consume())
        }
    }

    @Test
    fun `when trash action is submitted, use case is called and success message is emitted`() = runTest {
        // Given
        val expectedMessage = ActionResult.UndoableActionResult(TextUiModel(R.string.message_moved_to_trash))

        // when
        viewModel.submit(MessageViewAction.Trash)
        advanceUntilIdle()
        viewModel.state.test {

            // then
            coVerify { moveMessage(userId, messageId, SystemLabelId.Trash.labelId) }
            assertEquals(expectedMessage, awaitItem().exitScreenWithMessageEffect.consume())
        }
    }

    @Test
    fun `when error moving to trash, error is emitted`() = runTest {
        // Given
        coEvery {
            moveMessage(
                userId,
                messageId,
                SystemLabelId.Trash.labelId
            )
        } returns DataError.Local.NoDataCached.left()

        // When
        viewModel.submit(MessageViewAction.Trash)
        advanceUntilIdle()

        // Then
        assertEquals(TextUiModel(R.string.error_move_to_trash_failed), viewModel.state.value.error.consume())
    }

    @Test
    fun `when delete requested action is submitted, then delete dialog is shown`() = runTest {
        // Given
        val expectedTitle = TextUiModel(R.string.message_delete_dialog_title)
        val expectedMessage = TextUiModel(R.string.message_delete_dialog_message)

        // When
        viewModel.submit(MessageViewAction.DeleteRequested)
        advanceUntilIdle()

        // Then
        assertEquals(
            expected = DeleteDialogState.Shown(
                title = expectedTitle,
                message = expectedMessage
            ),
            actual = viewModel.state.value.deleteDialogState
        )
    }

    @Test
    fun `when delete confirmed action is submitted, use case is called and success message is emitted`() = runTest {
        // Given
        coJustRun { deleteMessages(userId, listOf(messageId), SystemLabelId.Spam.labelId) }
        coEvery { observeMessageWithLabels(userId, messageId) } returns flowOf(
            MessageWithLabels(
                MessageTestData.message.copy(labelIds = listOf(LabelIdSample.Spam)),
                emptyList()
            ).right()
        )
        val expectedMessage = ActionResult.DefinitiveActionResult(TextUiModel(R.string.message_deleted))

        // when
        viewModel.submit(MessageViewAction.DeleteConfirmed)
        advanceUntilIdle()
        viewModel.state.test {

            // then
            coVerify { deleteMessages(userId, listOf(messageId), SystemLabelId.Spam.labelId) }
            assertEquals(expectedMessage, awaitItem().exitScreenWithMessageEffect.consume())
        }
    }

    @Test
    fun `when delete confirmed action is submitted, and observing message fails, then error is emitted`() = runTest {
        // Given
        coEvery { observeMessageWithLabels(userId, messageId) } returns flowOf(DataError.Local.NoDataCached.left())
        val expectedMessage = TextUiModel(R.string.error_delete_message_failed)

        // when
        viewModel.submit(MessageViewAction.DeleteConfirmed)
        advanceUntilIdle()
        viewModel.state.test {

            // then
            coVerify { deleteMessages wasNot Called }
            assertEquals(expectedMessage, awaitItem().error.consume())
        }
    }

    @Test
    fun `when delete confirmed action is submitted, and exclusive labels fails, then error is emitted`() = runTest {
        // Given
        coEvery { observeMessageWithLabels(userId, messageId) } returns flowOf(
            MessageWithLabels(
                MessageTestData.message.copy(labelIds = listOf(LabelIdSample.Spam)),
                emptyList()
            ).right()
        )
        coEvery { observeMailLabels(userId) } returns flowOf()
        val expectedMessage = TextUiModel(R.string.error_delete_message_failed)

        // when
        viewModel.submit(MessageViewAction.DeleteConfirmed)
        advanceUntilIdle()
        viewModel.state.test {

            // then
            coVerify { deleteMessages wasNot Called }
            assertEquals(expectedMessage, awaitItem().error.consume())
        }
    }

    @Test
    fun `when delete confirmed action is submitted, and message has wrong location, then error is emitted`() = runTest {
        // Given
        val expectedMessage = TextUiModel(R.string.error_delete_message_failed_wrong_folder)

        // when
        viewModel.submit(MessageViewAction.DeleteConfirmed)
        advanceUntilIdle()
        viewModel.state.test {

            // then
            coVerify { deleteMessages wasNot Called }
            assertEquals(expectedMessage, awaitItem().error.consume())
        }
    }

    @Test
    fun `verify order of emitted states when starring a message`() = runTest {
        // When
        viewModel.state.test {
            // Then
            initialStateEmitted()
            messageBodyEmitted()
            val dataState = MessageDetailState.Loading.copy(
                messageMetadataState = MessageMetadataState.Data(
                    MessageDetailActionBarUiModelTestData.uiModel,
                    messageDetailHeaderUiModel,
                    messageDetailFooterUiModel
                ),
                messageBannersState = MessageBannersState.Data(messageBannersUiModel),
                messageBodyState = MessageBodyState.Data(
                    MessageBodyUiModelTestData.plainTextMessageBodyUiModel
                )
            )
            assertEquals(dataState, awaitItem())
            val bottomState = dataState.copy(
                bottomBarState = BottomBarState.Data.Shown(
                    listOf(
                        ActionUiModelTestData.archive,
                        ActionUiModelTestData.markUnread
                    ).toImmutableList()
                )
            )
            assertEquals(bottomState, awaitItem())
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.Star)
            val actual = assertIs<MessageMetadataState.Data>(awaitItem().messageMetadataState)
            assertTrue(actual.messageDetailActionBar.isStarred)
        }
    }

    @Test
    fun `selecting a move to destination emits MailLabelUiModel list with selected option`() = runTest {
        viewModel.state.test {
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.RequestMoveToBottomSheet)
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.MoveToDestinationSelected(MailLabelId.System.Spam))
            advanceUntilIdle()
            val actual = assertIs<MoveToBottomSheetState.Data>(lastEmittedItem().bottomSheetState?.contentState)
            assertTrue { actual.moveToDestinations.first { it.id == MailLabelId.System.Spam }.isSelected }
        }
    }

    @Test
    fun `verify move to is called and dismiss is set when destination gets confirmed`() = runTest {
        // Given
        coEvery {
            moveMessage(
                userId,
                messageId,
                MailLabelId.System.Spam.labelId
            )
        } returns Unit.right()

        // When
        viewModel.state.test {
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.RequestMoveToBottomSheet)
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.MoveToDestinationSelected(MailLabelId.System.Spam))
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.MoveToDestinationConfirmed(MailLabelText("spam")))
            advanceUntilIdle()

            // Then
            assertNotNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
            coVerify { moveMessage.invoke(userId, messageId, MailLabelId.System.Spam.labelId) }
        }
    }

    @Test
    fun `when error moving a message, error is emitted`() = runTest {
        // Given
        coEvery { moveMessage(userId, messageId, any()) } returns DataError.Local.NoDataCached.left()

        // When
        viewModel.submit(MessageViewAction.MoveToDestinationConfirmed(MailLabelText("spam")))
        advanceUntilIdle()

        // Then
        assertEquals(TextUiModel(R.string.error_move_message_failed), viewModel.state.value.error.consume())
    }

    @Test
    fun `toggle a label emits LabelUiModelWithSelectedState list with selected option`() = runTest {
        viewModel.state.test {
            viewModel.submit(MessageViewAction.RequestLabelAsBottomSheet)
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsToggleAction(MailLabelTestData.customLabelOne.id.labelId))
            advanceUntilIdle()
            val actual = assertIs<LabelAsBottomSheetState.Data>(lastEmittedItem().bottomSheetState?.contentState)
            assertTrue {
                actual.labelUiModelsWithSelectedState
                    .first { it.labelUiModel.id.labelId == MailLabelTestData.customLabelOne.id.labelId }
                    .selectedState == LabelSelectedState.Selected
            }
        }
    }

    @Test
    fun `verify use case is called when destination gets confirmed without archive selected`() = runTest {
        // Given
        coEvery {
            onMessageLabelAsConfirmed(userId, messageId, any(), false)
        } returns with(MessageSample) {
            Invoice.labelAs(
                listOf(
                    MailLabelTestData.customLabelOne.id.labelId,
                    MailLabelTestData.customLabelTwo.id.labelId
                )
            )
        }.right()

        // When
        viewModel.state.test {
            viewModel.submit(MessageViewAction.RequestLabelAsBottomSheet)
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsToggleAction(MailLabelTestData.customLabelOne.id.labelId))
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsToggleAction(MailLabelTestData.customLabelTwo.id.labelId))
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsConfirmed(false))
            advanceUntilIdle()

            // Then
            assertNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
            coVerify { onMessageLabelAsConfirmed(userId, messageId, any(), false) }
        }
    }

    @Test
    fun `bottom sheet with data is emitted when more actions bottom sheet is requested and loading succeeds`() =
        runTest {
            // Given
            coEvery {
                observeMessage(userId = userId, messageId = messageId)
            } returns flowOf(MessageSample.Invoice.right())

            // When
            viewModel.state.test {
                viewModel.submit(MessageViewAction.RequestMoreActionsBottomSheet(messageId))
                advanceUntilIdle()

                // Then
                assertIs<DetailMoreActionsBottomSheetState.Data>(lastEmittedItem().bottomSheetState?.contentState)
            }
        }

    @Test
    fun `verify no bottom sheet data is emitted when more actions bottom sheet is requested and loading fails`() =
        runTest {
            // Given
            coEvery {
                observeMessage(userId = userId, messageId = messageId)
            } returns flowOf(DataError.Local.NoDataCached.left())

            // When
            viewModel.state.test {
                viewModel.submit(MessageViewAction.RequestMoreActionsBottomSheet(messageId))
                advanceUntilIdle()

                // Then
                assertNull(lastEmittedItem().bottomSheetState?.contentState)
            }
        }

    @Test
    fun `verify use case is called when destination gets confirmed with archive selected`() = runTest {
        // Given
        coEvery {
            onMessageLabelAsConfirmed(userId, messageId, any(), true)
        } returns with(MessageSample) {
            Invoice.labelAs(
                listOf(
                    MailLabelTestData.customLabelOne.id.labelId,
                    MailLabelTestData.customLabelTwo.id.labelId
                )
            )
        }.right()

        // When
        viewModel.state.test {
            viewModel.submit(MessageViewAction.RequestLabelAsBottomSheet)
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsToggleAction(MailLabelTestData.customLabelOne.id.labelId))
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsToggleAction(MailLabelTestData.customLabelTwo.id.labelId))
            advanceUntilIdle()
            viewModel.submit(MessageViewAction.LabelAsConfirmed(true))
            advanceUntilIdle()

            // Then
            assertNotNull(lastEmittedItem().exitScreenWithMessageEffect.consume())
            coVerify {
                onMessageLabelAsConfirmed(userId, messageId, any(), true)
            }
        }
    }

    @Test
    fun `when error relabeling a message, error is emitted`() = runTest {
        // Given
        coEvery {
            onMessageLabelAsConfirmed(userId, messageId, any(), false)
        } returns DataError.Local.NoDataCached.left()

        // When
        viewModel.submit(MessageViewAction.RequestLabelAsBottomSheet)
        advanceUntilIdle()
        viewModel.submit(MessageViewAction.LabelAsConfirmed(false))
        advanceUntilIdle()

        // Then
        assertEquals(TextUiModel(R.string.error_relabel_message_failed), viewModel.state.value.error.consume())
    }

    @Test
    fun `should mark the message as read on message decryption`() = runTest {
        viewModel.state.test {
            // When
            initialStateEmitted()
            messageBodyEmitted()

            // Then
            coVerify { markRead.invoke(userId, messageId) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should not mark the message as read if there is an error reading the message`() = runTest {
        viewModel.state.test {
            // Given
            coEvery {
                getDecryptedMessageBody(userId, messageId)
            } returns GetDecryptedMessageBodyError.Data(DataError.Local.NoDataCached).left()

            // When
            initialStateEmitted()
            messageBodyLoadingErrorEmitted()

            // Then
            coVerify(exactly = 0) { markRead.invoke(any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state should contain uri when message body link is clicked`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val expected = Effect.of(uri)

        // When
        viewModel.state.test {
            initialStateEmitted()
            messageBodyEmitted()

            viewModel.submit(MessageViewAction.MessageBodyLinkClicked(uri))
            advanceUntilIdle()

            // Then
            assertEquals(expected, lastEmittedItem().openMessageBodyLinkEffect)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Initial requestLinkConfirmation should be emit as true`() = runTest {
        // Given
        val privacySettings = PrivacySettings(
            autoShowRemoteContent = false,
            autoShowEmbeddedImages = false,
            preventTakingScreenshots = false,
            requestLinkConfirmation = true,
            allowBackgroundSync = true
        )
        // given
        coEvery { observePrivacySettings(any()) } returns flowOf(privacySettings.right())

        // When
        viewModel.state.test {
            advanceUntilIdle()

            // Then
            assertEquals(true, lastEmittedItem().requestLinkConfirmation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Should disable requestLinkConfirmation flag when user checks do not ask again`() = runTest {
        // Given
        val initialPrivacySettings = PrivacySettings(
            autoShowRemoteContent = false,
            autoShowEmbeddedImages = false,
            preventTakingScreenshots = false,
            requestLinkConfirmation = true,
            allowBackgroundSync = true
        )
        coEvery { updateLinkConfirmationSetting(any()) } returns Unit.right()
        coEvery { observePrivacySettings(any()) } returns flowOf(initialPrivacySettings.right())

        // When
        viewModel.state.test {
            advanceUntilIdle()

            // Then
            assertEquals(true, lastEmittedItem().requestLinkConfirmation)
            cancelAndIgnoreRemainingEvents()

            // when
            viewModel.submit(MessageViewAction.DoNotAskLinkConfirmationAgain)
            advanceUntilIdle()

            // then
            coVerify { updateLinkConfirmationSetting(false) }
        }
    }

    @Test
    fun `all attachments are shown when all attachments should be shown`() = runTest {
        // Given
        val expectedMessageBody = DecryptedMessageBody(
            messageId = messageId,
            value = "Plain message body",
            mimeType = MimeType.PlainText,
            attachments = listOf(
                MessageAttachmentSample.invoice,
                MessageAttachmentSample.document,
                MessageAttachmentSample.documentWithMultipleDots,
                MessageAttachmentSample.image
            ),
            userAddress = UserAddressSample.PrimaryAddress
        )
        coEvery { getDecryptedMessageBody(userId, any()) } returns expectedMessageBody.right()
        coEvery {
            messageBodyUiModelMapper.toUiModel(userId, expectedMessageBody)
        } returns MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
        coEvery {
            observeAttachmentWorkerStatus.invoke(userId, messageId, any())
        } returns emptyFlow()


        viewModel.state.test {
            initialStateEmitted()
            messageBodyWithAttachmentEmitted()

            // When
            viewModel.submit(MessageViewAction.ShowAllAttachments)

            // Then
            val actualDataState = awaitItem().messageBodyState as MessageBodyState.Data
            assertEquals(4, actualDataState.messageBodyUiModel.attachments?.attachments?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `attachment metadata of all attachments is observed when messageBody with attachment loaded successfully`() =
        runTest {
            // Given
            val expectedMessageBody = DecryptedMessageBody(
                messageId = messageId,
                value = "Plain message body",
                mimeType = MimeType.PlainText,
                attachments = listOf(
                    MessageAttachmentSample.invoice,
                    MessageAttachmentSample.document,
                    MessageAttachmentSample.documentWithMultipleDots,
                    MessageAttachmentSample.image
                ),
                userAddress = UserAddressSample.PrimaryAddress
            )
            coEvery { getDecryptedMessageBody(userId, any()) } returns expectedMessageBody.right()
            coEvery {
                messageBodyUiModelMapper.toUiModel(userId, expectedMessageBody)
            } returns MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
            coEvery {
                observeAttachmentWorkerStatus.invoke(userId, messageId, any())
            } returns emptyFlow()


            viewModel.state.test {
                initialStateEmitted()
                // When
                messageBodyWithAttachmentEmitted()
                advanceUntilIdle()

                // Then
                coVerifyOrder {
                    observeAttachmentWorkerStatus(userId, messageId, AttachmentId("invoice"))
                    observeAttachmentWorkerStatus(userId, messageId, AttachmentId("document"))
                    observeAttachmentWorkerStatus(
                        userId,
                        messageId,
                        AttachmentId("complicated.document.name")
                    )
                    observeAttachmentWorkerStatus(userId, messageId, AttachmentId("image"))
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `verify get attachment is called when attachment is clicked`() = runTest {
        // Given
        val expectedIntentValues = OpenAttachmentIntentValues(mimeType = "application/pdf", uri = mockk())
        val expectedMessageBody = DecryptedMessageBody(
            messageId = messageId,
            value = "Plain message body",
            mimeType = MimeType.PlainText,
            attachments = listOf(
                MessageAttachmentSample.invoice,
                MessageAttachmentSample.document,
                MessageAttachmentSample.documentWithMultipleDots,
                MessageAttachmentSample.image
            ),
            userAddress = UserAddressSample.PrimaryAddress
        )
        coEvery { getDecryptedMessageBody(userId, any()) } returns expectedMessageBody.right()
        coEvery {
            messageBodyUiModelMapper.toUiModel(userId, expectedMessageBody)
        } returns MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
        coEvery {
            observeAttachmentWorkerStatus(userId, messageId, any())
        } returns flowOf()
        coEvery {
            getAttachmentIntentValues(userId, messageId, AttachmentId("invoice"))
        } returns expectedIntentValues.right()
        coEvery { getDownloadingAttachmentsForMessages(userId, listOf(messageId)) } returns listOf()


        // When
        viewModel.state.test {
            initialStateEmitted()
            messageBodyWithAttachmentEmitted()

            viewModel.submit(MessageViewAction.OnAttachmentClicked(AttachmentId("invoice")))
            advanceUntilIdle()

            val actualState = lastEmittedItem()

            // Then
            coVerify {
                getAttachmentIntentValues(
                    userId,
                    messageId,
                    AttachmentId("invoice")
                )
            }
            assertEquals(Effect.of(expectedIntentValues), actualState.openAttachmentEffect)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `verify error is shown when getting attachment failed`() = runTest {
        // Given
        val expectedMessageBody = DecryptedMessageBody(
            messageId = messageId,
            value = "Plain message body",
            mimeType = MimeType.PlainText,
            attachments = listOf(
                MessageAttachmentSample.invoice,
                MessageAttachmentSample.document,
                MessageAttachmentSample.documentWithMultipleDots,
                MessageAttachmentSample.image
            ),
            userAddress = UserAddressSample.PrimaryAddress
        )
        coEvery { getDecryptedMessageBody(userId, any()) } returns expectedMessageBody.right()
        coEvery {
            messageBodyUiModelMapper.toUiModel(userId, expectedMessageBody)
        } returns MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
        coEvery {
            observeAttachmentWorkerStatus(userId, messageId, any())
        } returns flowOf()
        coEvery { getAttachmentIntentValues(any(), any(), any()) } returns DataError.Local.NoDataCached.left()
        coEvery { getDownloadingAttachmentsForMessages(userId, listOf(messageId)) } returns listOf()

        // When
        viewModel.state.test {
            initialStateEmitted()
            messageBodyWithAttachmentEmitted()

            viewModel.submit(MessageViewAction.OnAttachmentClicked(AttachmentId("invoice")))
            advanceUntilIdle()

            val actualState = lastEmittedItem()

            // Then
            assertEquals(Effect.of(TextUiModel(R.string.error_get_attachment_failed)), actualState.error)
        }
    }

    @Test
    fun `verify get attachment is not called and error is shown when other attachment is currently downloading`() =
        runTest {
            // Given
            val expectedIntentValues = OpenAttachmentIntentValues(mimeType = "application/pdf", uri = mockk())
            val expectedMessageBody = DecryptedMessageBody(
                messageId = messageId,
                value = "Plain message body",
                mimeType = MimeType.PlainText,
                attachments = listOf(
                    MessageAttachmentSample.invoice,
                    MessageAttachmentSample.document,
                    MessageAttachmentSample.documentWithMultipleDots,
                    MessageAttachmentSample.image
                ),
                userAddress = UserAddressSample.PrimaryAddress
            )
            coEvery { getDecryptedMessageBody(userId, any()) } returns expectedMessageBody.right()
            coEvery {
                messageBodyUiModelMapper.toUiModel(userId, expectedMessageBody)
            } returns MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
            coEvery {
                observeAttachmentWorkerStatus(userId, messageId, any())
            } returns flowOf(MessageAttachmentMetadataTestData.buildMessageAttachmentMetadata())
            coEvery {
                getAttachmentIntentValues(userId, messageId, AttachmentId("invoice"))
            } returns expectedIntentValues.right()
            coEvery {
                getDownloadingAttachmentsForMessages(userId, listOf(messageId))
            } returns listOf(MessageAttachmentMetadataTestData.buildMessageAttachmentMetadata())

            viewModel.state.test {
                initialStateEmitted()
                messageBodyWithAttachmentEmitted()
                skipItems(6) // skip emitted attachment worker status

                // When
                viewModel.submit(MessageViewAction.OnAttachmentClicked(AttachmentId("invoice")))
                val actualState = awaitItem()

                // Then
                coVerify { getAttachmentIntentValues wasNot Called }
                assertEquals(Effect.of(TextUiModel(R.string.error_attachment_download_in_progress)), actualState.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `returns get embedded image result when getting was successful`() = runTest {
        // Given
        val contentId = "contentId"
        val byteArray = "I'm a byte array".toByteArray()
        val expectedResult = GetEmbeddedImageResult(byteArray, "image/png")
        coEvery {
            getEmbeddedImageAvoidDuplicatedExecution(
                userId,
                messageId,
                contentId,
                any()
            )
        } returns expectedResult

        // When
        val actual = withContext(Dispatchers.IO) { viewModel.loadEmbeddedImage(messageId, contentId) }

        // Then
        assertEquals(expectedResult, actual)
    }

    @Test
    fun `returns null when get embedded image returned an error`() = runTest {
        // Given
        val contentId = "contentId"
        coEvery {
            getEmbeddedImageAvoidDuplicatedExecution(
                userId,
                messageId,
                contentId,
                any()
            )
        } returns null

        // When
        val actual = withContext(Dispatchers.IO) { viewModel.loadEmbeddedImage(messageId, contentId) }

        // Then
        assertNull(actual)
    }

    @Test
    fun `initial expand collapse mode will be not applicable given body contains no quotes`() = runTest {
        // Given
        val expectedDecryptedHtmlMessageBody =
            decryptedHtmlMessageBody.copy(value = EmailBodyTestSamples.BodyWithoutQuotes)
        val hasQuote = extractMessageBodyWithoutQuote(expectedDecryptedHtmlMessageBody.value).hasQuote
        val expectedMessageBodyUiModel = MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel.copy(
            shouldShowExpandCollapseButton = hasQuote
        )
        coEvery {
            getDecryptedMessageBody(userId, any())
        } returns expectedDecryptedHtmlMessageBody.right()
        coEvery {
            messageBodyUiModelMapper.toUiModel(userId, any())
        } returns expectedMessageBodyUiModel
        coEvery {
            observeAttachmentWorkerStatus(userId, any(), any())
        } returns flowOf()
        coEvery {
            getAttachmentIntentValues(userId, messageId, any())
        } returns mockk()
        coEvery { getDownloadingAttachmentsForMessages(userId, any()) } returns listOf()

        // When
        viewModel.state.test {
            initialStateEmitted()
            skipItems(2)

            // Then
            val expected = MessageBodyState.Data(
                expectedMessageBodyUiModel, MessageBodyExpandCollapseMode.NotApplicable
            )
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial expand collapse mode will be collapsed given body contains proton mail quote`() = runTest {
        // Given
        val expectedDecryptedHtmlMessageBody =
            decryptedHtmlMessageBody.copy(value = EmailBodyTestSamples.BodyWithProtonMailQuote)
        val hasQuote = extractMessageBodyWithoutQuote(expectedDecryptedHtmlMessageBody.value).hasQuote
        val expectedMessageBodyUiModel = MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel.copy(
            shouldShowExpandCollapseButton = hasQuote
        )
        coEvery {
            getDecryptedMessageBody(userId, any())
        } returns expectedDecryptedHtmlMessageBody.right()
        coEvery {
            messageBodyUiModelMapper.toUiModel(userId, any())
        } returns expectedMessageBodyUiModel
        coEvery {
            observeAttachmentWorkerStatus(userId, any(), any())
        } returns flowOf()
        coEvery {
            getAttachmentIntentValues(userId, messageId, any())
        } returns mockk()
        coEvery { getDownloadingAttachmentsForMessages(userId, any()) } returns listOf()

        // When
        viewModel.state.test {
            initialStateEmitted()
            skipItems(2)

            // Then
            val expected = MessageBodyState.Data(
                expectedMessageBodyUiModel, MessageBodyExpandCollapseMode.Collapsed
            )
            assertEquals(expected, awaitItem().messageBodyState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when user clicks expand collapse button then mode will toggle`() = runTest {
        // Given
        val expectedDecryptedHtmlMessageBody =
            decryptedHtmlMessageBody.copy(value = EmailBodyTestSamples.BodyWithProtonMailQuote)
        val hasQuote = extractMessageBodyWithoutQuote(expectedDecryptedHtmlMessageBody.value).hasQuote
        val expectedMessageBodyUiModel = MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel.copy(
            shouldShowExpandCollapseButton = hasQuote
        )
        coEvery {
            getDecryptedMessageBody(userId, any())
        } returns expectedDecryptedHtmlMessageBody.right()
        coEvery {
            messageBodyUiModelMapper.toUiModel(userId, any())
        } returns expectedMessageBodyUiModel
        coEvery {
            observeAttachmentWorkerStatus(userId, any(), any())
        } returns flowOf()
        coEvery {
            getAttachmentIntentValues(userId, messageId, any())
        } returns mockk()
        coEvery { getDownloadingAttachmentsForMessages(userId, any()) } returns listOf()

        viewModel.state.test {
            initialStateEmitted()
            skipItems(3)

            // When
            viewModel.submit(MessageViewAction.ExpandOrCollapseMessageBody)

            // Then
            val expectExpanded = MessageBodyState.Data(
                expectedMessageBodyUiModel, MessageBodyExpandCollapseMode.Expanded
            )
            assertEquals(expectExpanded, awaitItem().messageBodyState)

            // When
            viewModel.submit(MessageViewAction.ExpandOrCollapseMessageBody)

            // Then
            val expectCollapsed = MessageBodyState.Data(
                expectedMessageBodyUiModel, MessageBodyExpandCollapseMode.Collapsed
            )
            assertEquals(expectCollapsed, awaitItem().messageBodyState)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when user clicks report phishing and network state is connected than confirm dialog is shown`() = runTest {
        // Given
        val expected = ReportPhishingDialogState.Shown.ShowConfirmation(MessageIdSample.Invoice)
        coEvery { networkManager.networkStatus } returns NetworkStatus.Metered


        viewModel.state.test {
            initialStateEmitted()
            skipItems(3)

            // When
            viewModel.submit(MessageViewAction.ReportPhishing(MessageIdSample.Invoice))

            assertEquals(expected, awaitItem().reportPhishingDialogState)
        }
    }

    @Test
    fun `when user clicks report phishing and network state is disconnected than offline hint is shown`() = runTest {
        // Given
        val expected = ReportPhishingDialogState.Shown.ShowOfflineHint
        coEvery { networkManager.networkStatus } returns NetworkStatus.Disconnected


        viewModel.state.test {
            initialStateEmitted()
            skipItems(3)

            // When
            viewModel.submit(MessageViewAction.ReportPhishing(MessageIdSample.Invoice))

            // Then
            assertEquals(expected, awaitItem().reportPhishingDialogState)
        }
    }

    @Test
    fun `when report phishing confirmed, use case is called and view is closed`() = runTest {
        // Given
        coEvery { networkManager.networkStatus } returns NetworkStatus.Metered
        coEvery { reportPhishingMessage(userId, messageId) } returns Unit.right()

        viewModel.state.test {
            initialStateEmitted()
            skipItems(3)

            // When
            viewModel.submit(MessageViewAction.ReportPhishingConfirmed)
            advanceUntilIdle()

            // Then
            assertEquals(Effect.of(Unit), awaitItem().exitScreenEffect)
            coVerify(exactly = 1) { reportPhishingMessage(userId, messageId) }
        }
    }

    @Test
    fun `when user clicks open proton calendar and calendar is install, then open in proton calendar is called`() =
        runTest {
            // Given
            val expectedUri = mockk<Uri>()
            val expectedMessageBody = decryptedHtmlMessageBody.copy(
                attachments = listOf(MessageAttachmentSample.calendar),
                userAddress = UserAddressSample.PrimaryAddress
            )

            coEvery { getDecryptedMessageBody(userId, any()) } returns expectedMessageBody.right()
            coEvery {
                messageBodyUiModelMapper.toUiModel(userId, expectedMessageBody)
            } returns MessageBodyUiModelTestData.messageBodyWithIcsAttachmentsUiModel
                .copy(userAddress = UserAddressSample.PrimaryAddress)
            coEvery { isProtonCalendarInstalled() } returns true
            coEvery { getAttachmentIntentValues(userId, messageId, AttachmentId("calendar")) } returns
                OpenAttachmentIntentValues("text/calendar", expectedUri).right()
            coEvery { observeAttachmentWorkerStatus(userId, messageId, any()) } returns flowOf()

            viewModel.state.test {
                skipItems(4) // skip initial states

                // When
                viewModel.submit(MessageViewAction.OpenInProtonCalendar(messageId))

                // Then
                val actual = awaitItem()
                val calendarIntentValues = actual.openProtonCalendarIntent.consume() as? OpenIcsInProtonCalendar
                assertNotNull(calendarIntentValues)
                assertEquals(expectedUri, calendarIntentValues.uriToIcsAttachment)
                assertEquals(UserAddressSample.PrimaryAddress.email, calendarIntentValues.recipient)
            }
        }

    @Test
    fun `when user clicks open proton calendar and calendar is not installed, then open play store is called`() =
        runTest {
            // Given
            val expectedUri = mockk<Uri>()
            val expectedMessageBody = decryptedHtmlMessageBody.copy(
                attachments = listOf(MessageAttachmentSample.calendar),
                userAddress = UserAddressSample.PrimaryAddress
            )

            coEvery { getDecryptedMessageBody(userId, any()) } returns expectedMessageBody.right()
            coEvery {
                messageBodyUiModelMapper.toUiModel(userId, expectedMessageBody)
            } returns MessageBodyUiModelTestData.messageBodyWithIcsAttachmentsUiModel
                .copy(userAddress = UserAddressSample.PrimaryAddress)
            coEvery { isProtonCalendarInstalled() } returns false
            coEvery { getAttachmentIntentValues(userId, messageId, AttachmentId("calendar")) } returns
                OpenAttachmentIntentValues("text/calendar", expectedUri).right()
            coEvery { observeAttachmentWorkerStatus(userId, messageId, any()) } returns flowOf()

            viewModel.state.test {
                skipItems(4) // skip initial states

                // When
                viewModel.submit(MessageViewAction.OpenInProtonCalendar(messageId))

                // Then
                val actual = awaitItem()
                val calendarIntentValues = actual.openProtonCalendarIntent.consume() as? OpenProtonCalendarOnPlayStore
                assertNotNull(calendarIntentValues)
            }
        }

    @Test
    fun `emit correct state when switching the view mode is requested`() = runTest {
        // Given
        val expected = ViewModePreference.LightMode

        viewModel.state.test {
            skipItems(4)

            // When
            viewModel.submit(MessageViewAction.SwitchViewMode(ViewModePreference.LightMode))

            val state = awaitItem().messageBodyState as MessageBodyState.Data
            assertEquals(expected, state.messageBodyUiModel.viewModePreference)
        }
    }

    @Test
    fun `emit correct state when printing the message was requested`() = runTest {
        viewModel.state.test {
            skipItems(4)

            // When
            viewModel.submit(MessageViewAction.PrintRequested)

            // Then
            val item = awaitItem()
            assertIs<Effect<BottomSheetVisibilityEffect.Hide>>(item.bottomSheetState?.bottomSheetVisibilityEffect)
            val expectedPrintEffect = Effect.of(Unit)
            val messageBodyState = item.messageBodyState as MessageBodyState.Data
            assertEquals(expectedPrintEffect, messageBodyState.messageBodyUiModel.printEffect)
        }
    }

    @Test
    fun `call the print message use case in order to print a message`() = runTest {
        // Given
        val context = mockk<Context>()
        every { printMessage(any(), any(), any(), any(), any(), any()) } just runs

        viewModel.state.test {
            skipItems(4)

            // When
            viewModel.submit(MessageViewAction.Print(context))

            // Then
            val messageMetadataState = viewModel.state.value.messageMetadataState as MessageMetadataState.Data
            val messageBodyState = viewModel.state.value.messageBodyState as MessageBodyState.Data
            verify {
                printMessage(
                    context,
                    messageMetadataState.messageDetailActionBar.subject,
                    messageMetadataState.messageDetailHeader,
                    messageBodyState.messageBodyUiModel,
                    messageBodyState.expandCollapseMode,
                    viewModel::loadEmbeddedImage
                )
            }
        }
    }

    @Test
    fun `bottom sheet with data is emitted when contact actions bottom sheet is requested`() = runTest {
        // Given
        val participant = Participant(
            "test@proton.me",
            "Test User"
        )
        val contact = ContactSample.Stefano
        coEvery {
            observeMessage(userId = userId, messageId = messageId)
        } returns flowOf(MessageSample.Invoice.right())

        coEvery { findContactByEmail.invoke(userId, participant.address) } returns contact

        val participantUiModel =
            ParticipantUiModel(
                participant.name,
                participant.address,
                R.drawable.ic_proton_lock,
                shouldShowOfficialBadge = false
            )
        val avatar = AvatarUiModel.ParticipantInitial("TU")

        // When
        viewModel.state.test {
            viewModel.submit(MessageViewAction.RequestContactActionsBottomSheet(participantUiModel, avatar))
            skipItems(4)

            // Then
            val messageDetailState = awaitItem()
            assertIs<ContactActionsBottomSheetState.Data>(messageDetailState.bottomSheetState?.contentState)
            val stateData =
                messageDetailState.bottomSheetState?.contentState as ContactActionsBottomSheetState.Data
            assertEquals(contact.id, stateData.contactId)
            assertEquals(avatar, stateData.avatarUiModel)
            assertEquals(participant, stateData.participant)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when archive action is submitted, use case is called and success message is emitted`() = runTest {
        // Given
        val expectedMessage = ActionResult.UndoableActionResult(TextUiModel(R.string.message_moved_to_archive))

        // when
        viewModel.submit(MessageViewAction.Archive)
        advanceUntilIdle()
        viewModel.state.test {

            // then
            coVerify { moveMessage(userId, messageId, SystemLabelId.Archive.labelId) }
            assertEquals(expectedMessage, awaitItem().exitScreenWithMessageEffect.consume())
        }
    }

    @Test
    fun `when error moving to archive, error is emitted`() = runTest {
        // Given
        coEvery {
            moveMessage(
                userId,
                messageId,
                SystemLabelId.Archive.labelId
            )
        } returns DataError.Local.NoDataCached.left()

        // When
        viewModel.submit(MessageViewAction.Archive)
        advanceUntilIdle()

        // Then
        assertEquals(TextUiModel(R.string.error_move_to_archive_failed), viewModel.state.value.error.consume())
    }

    @Test
    fun `when spam action is submitted, use case is called and success message is emitted`() = runTest {
        // Given
        val expectedMessage = ActionResult.UndoableActionResult(TextUiModel(R.string.message_moved_to_spam))

        // when
        viewModel.submit(MessageViewAction.Spam)
        advanceUntilIdle()
        viewModel.state.test {

            // then
            coVerify { moveMessage(userId, messageId, SystemLabelId.Spam.labelId) }
            assertEquals(expectedMessage, awaitItem().exitScreenWithMessageEffect.consume())
        }
    }

    @Test
    fun `when error moving to spam, error is emitted`() = runTest {
        // Given
        coEvery {
            moveMessage(
                userId,
                messageId,
                SystemLabelId.Spam.labelId
            )
        } returns DataError.Local.NoDataCached.left()

        // When + Then
        viewModel.state.test {
            viewModel.submit(MessageViewAction.Spam)
            advanceUntilIdle()

            val item = lastEmittedItem()
            assertEquals(TextUiModel(R.string.error_move_to_spam_failed), item.error.consume())
        }
    }

    private suspend fun ReceiveTurbine<MessageDetailState>.initialStateEmitted() {
        assertEquals(MessageDetailState.Loading, awaitItem())
    }

    private suspend fun ReceiveTurbine<MessageDetailState>.messageBodyEmitted() {
        assertEquals(
            MessageDetailState.Loading.copy(
                messageBodyState = MessageBodyState.Data(
                    MessageBodyUiModelTestData.plainTextMessageBodyUiModel
                )
            ),
            awaitItem()
        )
    }

    private suspend fun ReceiveTurbine<MessageDetailState>.messageBodyWithAttachmentEmitted() {
        assertEquals(
            MessageDetailState.Loading.copy(
                messageBodyState = MessageBodyState.Data(
                    MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
                )
            ),
            awaitItem()
        )
    }

    private suspend fun ReceiveTurbine<MessageDetailState>.messageBodyLoadingErrorEmitted() {
        assertEquals(
            MessageDetailState.Loading.copy(
                messageBodyState = MessageBodyState.Error.Data(isNetworkError = false)
            ),
            awaitItem()
        )
    }

    private fun givenNoLoggedInUser() {
        every { observePrimaryUserId.invoke() } returns MutableStateFlow(null)
    }

    private suspend fun ReceiveTurbine<MessageDetailState>.lastEmittedItem(): MessageDetailState {
        val events = cancelAndConsumeRemainingEvents()
        return (events.last() as Event.Item).value
    }
}
