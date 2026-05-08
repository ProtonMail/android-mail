/*
 * Copyright (c) 2025 Proton Technologies AG
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

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.IsDarkModeEnabled
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maildetail.presentation.mapper.MessageBodyUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.EntireMessageBodyAction
import ch.protonmail.android.maildetail.presentation.model.EntireMessageBodyState
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.maildetail.presentation.ui.EntireMessageBodyScreen
import ch.protonmail.android.maildetail.presentation.usecase.LoadImageAvoidDuplicatedExecution
import ch.protonmail.android.maildetail.presentation.viewmodel.EntireMessageBodyViewModelTest.TestData.InputData
import ch.protonmail.android.maildetail.presentation.viewmodel.EntireMessageBodyViewModelTest.TestData.MESSAGE_ID
import ch.protonmail.android.maildetail.presentation.viewmodel.EntireMessageBodyViewModelTest.TestData.SUBJECT
import ch.protonmail.android.mailmessage.domain.model.GetMessageBodyError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetMessageBodyWithClickableLinks
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsettings.domain.model.PrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObservePrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.privacy.UpdateLinkConfirmationSetting
import ch.protonmail.android.testdata.message.DecryptedMessageBodyTestData
import ch.protonmail.android.testdata.message.MessageBodyUiModelTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.proton.core.util.kotlin.serialize
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EntireMessageBodyViewModelTest {

    private val isDarkModeEnabled = mockk<IsDarkModeEnabled> {
        every { this@mockk() } returns true
    }
    private val observePrimaryUserId = mockk<ObservePrimaryUserId> {
        every { this@mockk.invoke() } returns flowOf(UserIdTestData.userId)
    }
    private val getMessageBodyWithClickableLinks = mockk<GetMessageBodyWithClickableLinks>()
    private val messageBodyUiModelMapper = mockk<MessageBodyUiModelMapper>()
    private val loadImageAvoidDuplicatedExecution = mockk<LoadImageAvoidDuplicatedExecution>()
    private val observeMessage = mockk<ObserveMessage> {
        coEvery { this@mockk.invoke(UserIdTestData.userId, MessageId(MESSAGE_ID)) } returns flowOf(
            MessageTestData.buildMessage(
                id = MESSAGE_ID, subject = SUBJECT
            ).right()
        )
    }
    private val observePrivacySettings = mockk<ObservePrivacySettings> {
        coEvery { this@mockk.invoke(UserIdTestData.userId) } returns flowOf(
            PrivacySettings(
                autoShowRemoteContent = false,
                autoShowEmbeddedImages = true,
                preventTakingScreenshots = false,
                requestLinkConfirmation = true,
                allowBackgroundSync = false
            ).right()
        )
    }
    private val savedStateHandle = mockk<SavedStateHandle> {
        every { this@mockk.get<String>(EntireMessageBodyScreen.MESSAGE_ID_KEY) } returns MESSAGE_ID
        every { this@mockk.get<String>(EntireMessageBodyScreen.INPUT_PARAMS_KEY) } returns InputData
    }
    private val updateLinkConfirmationSetting = mockk<UpdateLinkConfirmationSetting> {
        coEvery { this@mockk.invoke(false) } returns Unit.right()
    }

    private val entireMessageBodyViewModel by lazy {
        EntireMessageBodyViewModel(
            observePrimaryUserId = observePrimaryUserId,
            isDarkModeEnabled = isDarkModeEnabled,
            getMessageBodyWithClickableLinks = getMessageBodyWithClickableLinks,
            messageBodyUiModelMapper = messageBodyUiModelMapper,
            loadImageAvoidDuplicatedExecution = loadImageAvoidDuplicatedExecution,
            observeMessage = observeMessage,
            observePrivacySettings = observePrivacySettings,
            savedStateHandle = savedStateHandle,
            updateLinkConfirmationSetting = updateLinkConfirmationSetting
        )
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `should emit correct state after successfully getting the message body in the initialization`() = runTest {
        // Given
        val decryptedMessageBody = DecryptedMessageBodyTestData.messageBodyWithAttachment
        val messageBodyUiModel = MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
        coEvery {
            getMessageBodyWithClickableLinks(UserIdTestData.userId, MessageId(MESSAGE_ID), any())
        } returns decryptedMessageBody.right()
        coEvery {
            messageBodyUiModelMapper.toUiModel(
                decryptedMessageBody = decryptedMessageBody,
                attachmentListExpandCollapseMode = null,
                existingMessageBodyUiModel = null,
                forceDisableHeightRestriction = true
            )
        } returns messageBodyUiModel

        // When
        entireMessageBodyViewModel.state.test {
            val item = awaitItem()

            // Then
            val expected = EntireMessageBodyState(
                messageBodyState = MessageBodyState.Data(
                    messageBodyUiModel = messageBodyUiModel.copy(
                        shouldShowEmbeddedImagesBanner = false,
                        viewModePreference = ViewModePreference.ThemeDefault
                    ),
                    expandCollapseMode = MessageBodyExpandCollapseMode.NotApplicable
                ),
                subject = SUBJECT,
                requestLinkConfirmation = true,
                requestPhishingLinkConfirmation = false,
                openMessageBodyLinkEffect = Effect.empty()
            )
            assertEquals(expected, item)
        }
    }

    @Test
    fun `should emit correct state after a decrypt error getting the message body in the initialization`() = runTest {
        // Given
        val encryptedMessageBody = "encryptedMessageBody"
        val getDecryptedMessageBodyError = GetMessageBodyError.Decryption(
            MessageId(MESSAGE_ID), encryptedMessageBody
        )
        val messageBodyUiModel = MessageBodyUiModelTestData.plainTextMessageBodyUiModel
        coEvery {
            getMessageBodyWithClickableLinks(UserIdTestData.userId, MessageId(MESSAGE_ID), any())
        } returns getDecryptedMessageBodyError.left()
        coEvery {
            messageBodyUiModelMapper.toUiModel(getDecryptedMessageBodyError)
        } returns messageBodyUiModel

        // When
        entireMessageBodyViewModel.state.test {
            val item = awaitItem()

            // Then
            val expected = EntireMessageBodyState(
                messageBodyState = MessageBodyState.Error.Decryption(
                    encryptedMessageBody = messageBodyUiModel.copy(
                        shouldShowEmbeddedImagesBanner = false,
                        viewModePreference = ViewModePreference.ThemeDefault
                    )
                ),
                subject = SUBJECT,
                requestLinkConfirmation = true,
                requestPhishingLinkConfirmation = false,
                openMessageBodyLinkEffect = Effect.empty()
            )
            assertEquals(expected, item)
        }
    }

    @Test
    fun `should emit correct state after a loading error getting the message body in the initialization`() = runTest {
        // Given
        coEvery {
            getMessageBodyWithClickableLinks(UserIdTestData.userId, MessageId(MESSAGE_ID), any())
        } returns GetMessageBodyError.Data(DataError.Local.Unknown).left()

        // When
        entireMessageBodyViewModel.state.test {
            val item = awaitItem()

            // Then
            val expected = EntireMessageBodyState(
                messageBodyState = MessageBodyState.Error.Data(
                    isNetworkError = false
                ),
                subject = SUBJECT,
                requestLinkConfirmation = true,
                requestPhishingLinkConfirmation = false,
                openMessageBodyLinkEffect = Effect.empty()
            )
            assertEquals(expected, item)
        }
    }

    @Test
    fun `should handle message body link clicked when action is submitted`() = runTest {
        // Given
        val decryptedMessageBody = DecryptedMessageBodyTestData.messageBodyWithAttachment
        val messageBodyUiModel = MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
        val uri = mockk<Uri>()
        coEvery {
            getMessageBodyWithClickableLinks(UserIdTestData.userId, MessageId(MESSAGE_ID), any())
        } returns decryptedMessageBody.right()
        coEvery {
            messageBodyUiModelMapper.toUiModel(
                decryptedMessageBody = decryptedMessageBody,
                attachmentListExpandCollapseMode = null,
                existingMessageBodyUiModel = null,
                forceDisableHeightRestriction = true
            )
        } returns messageBodyUiModel

        // When
        entireMessageBodyViewModel.state.test {
            skipItems(1)

            entireMessageBodyViewModel.submit(EntireMessageBodyAction.MessageBodyLinkClicked(uri))

            // Then
            assertEquals(uri, awaitItem().openMessageBodyLinkEffect.consume())
        }
    }

    @Test
    fun `should handle do not ask link confirmation again when action is submitted`() = runTest {
        // Given
        val decryptedMessageBody = DecryptedMessageBodyTestData.messageBodyWithAttachment
        val messageBodyUiModel = MessageBodyUiModelTestData.messageBodyWithAttachmentsUiModel
        coEvery {
            getMessageBodyWithClickableLinks(UserIdTestData.userId, MessageId(MESSAGE_ID), any())
        } returns decryptedMessageBody.right()
        coEvery {
            messageBodyUiModelMapper.toUiModel(
                decryptedMessageBody = decryptedMessageBody,
                attachmentListExpandCollapseMode = null,
                existingMessageBodyUiModel = null,
                forceDisableHeightRestriction = true
            )
        } returns messageBodyUiModel

        // When
        entireMessageBodyViewModel.state.test {
            skipItems(1)

            entireMessageBodyViewModel.submit(EntireMessageBodyAction.DoNotAskLinkConfirmationAgain)

            // Then
            coVerify { updateLinkConfirmationSetting(false) }
        }
    }

    object TestData {

        const val MESSAGE_ID = "message_id"

        const val SUBJECT = "subject"

        val InputData = EntireMessageBodyScreen.InputParams(
            shouldShowEmbeddedImages = true,
            shouldShowRemoteContent = false,
            viewModePreference = ViewModePreference.DarkMode
        ).serialize()
    }

}
