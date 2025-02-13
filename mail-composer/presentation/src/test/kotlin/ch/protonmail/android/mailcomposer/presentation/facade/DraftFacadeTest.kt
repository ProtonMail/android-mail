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

package ch.protonmail.android.mailcomposer.presentation.facade

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftFields
import ch.protonmail.android.mailcomposer.domain.model.MessageWithDecryptedBody
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.usecase.DraftUploader
import ch.protonmail.android.mailcomposer.domain.usecase.GetDecryptedDraftFields
import ch.protonmail.android.mailcomposer.domain.usecase.GetLocalMessageDecrypted
import ch.protonmail.android.mailcomposer.domain.usecase.ProvideNewDraftId
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithAllFields
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithParentAttachments
import ch.protonmail.android.mailcomposer.presentation.usecase.InjectAddressSignature
import ch.protonmail.android.mailcomposer.presentation.usecase.ParentMessageToDraftFields
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Rule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull

internal class DraftFacadeTest {

    private val provideNewDraftId = mockk<ProvideNewDraftId>(relaxed = true)
    private val getDecryptedDraftFields = mockk<GetDecryptedDraftFields>(relaxed = true)
    private val getLocalMessageDecrypted = mockk<GetLocalMessageDecrypted>(relaxed = true)
    private val parentMessageToDraftFields = mockk<ParentMessageToDraftFields>(relaxed = true)
    private val storeDraftWithAllFields = mockk<StoreDraftWithAllFields>(relaxed = true)
    private val storeDraftWithParentAttachments = mockk<StoreDraftWithParentAttachments>(relaxed = true)
    private val injectAddressSignature = mockk<InjectAddressSignature>(relaxed = true)
    private val draftUploader = mockk<DraftUploader>(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    lateinit var draftFacade: DraftFacade

    @BeforeTest
    fun setup() {
        draftFacade = DraftFacade(
            provideNewDraftId,
            getDecryptedDraftFields,
            getLocalMessageDecrypted,
            parentMessageToDraftFields,
            storeDraftWithAllFields,
            storeDraftWithParentAttachments,
            injectAddressSignature,
            draftUploader,
            testDispatcher
        )
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should proxy provideNewDraftId accordingly`() {
        // When
        draftFacade.provideNewDraftId()

        // Then
        verify(exactly = 1) { provideNewDraftId.invoke() }
    }

    @Test
    fun `should proxy getDecryptedDraftFields accordingly`() = runTest {
        // Given
        val expectedUserId = UserId("user-id")
        val expectedMessageId = MessageId("message-id")

        // When
        draftFacade.getDecryptedDraftFields(expectedUserId, expectedMessageId)

        // Then
        coVerify(exactly = 1) { getDecryptedDraftFields.invoke(expectedUserId, expectedMessageId) }
    }

    @Test
    fun `should proxy storeDraft accordingly`() = runTest {
        // Given
        val expectedUserId = UserId("user-id")
        val expectedDraftMessageId = MessageId("message-id")
        val expectedFields = mockk<DraftFields>()
        val draftAction = DraftAction.Reply(parentId = MessageId("message-id-parent"))

        // When
        draftFacade.storeDraft(expectedUserId, expectedDraftMessageId, expectedFields, draftAction)

        // Then
        coVerify(exactly = 1) {
            storeDraftWithAllFields(expectedUserId, expectedDraftMessageId, expectedFields, draftAction)
        }
    }

    @Test
    fun `should proxy storeDraftWithParentAttachments accordingly`() = runTest {
        // Given
        val expectedUserId = UserId("user-id")
        val expectedMessageId = MessageId("message-id")
        val expectedParentMessage = mockk<MessageWithDecryptedBody>()
        val expectedSenderEmail = SenderEmail("sender@email.com")
        val expectedDraftAction = DraftAction.Compose

        // When
        draftFacade.storeDraftWithParentAttachments(
            expectedUserId, expectedMessageId, expectedParentMessage, expectedSenderEmail, expectedDraftAction
        )

        // Then
        coVerify(exactly = 1) {
            storeDraftWithParentAttachments(
                expectedUserId, expectedMessageId, expectedParentMessage, expectedSenderEmail, expectedDraftAction
            )
        }
    }

    @Test
    fun `should proxy start continuous upload properly`() = runTest {
        // Given
        val expectedUserId = UserId("user-id")
        val expectedMessageId = MessageId("message-id")
        val expectedAction = DraftAction.Compose
        val expectedScope = mockk<CoroutineScope>()

        // When
        draftFacade.startContinuousUpload(
            expectedUserId,
            expectedMessageId,
            expectedAction,
            expectedScope
        )

        // Then
        coVerify(exactly = 1) {
            draftUploader.startContinuousUpload(
                expectedUserId,
                expectedMessageId,
                expectedAction,
                expectedScope
            )
        }
    }

    @Test
    fun `should proxy stop continuous upload properly`() = runTest {

        // When
        draftFacade.stopContinuousUpload()

        // Then
        coVerify(exactly = 1) {
            draftUploader.stopContinuousUpload()
        }
    }

    @Test
    fun `should proxy force upload properly`() = runTest {
        val userId = UserId("user-id")
        val messageId = MessageId("message-id")

        // When
        draftFacade.forceUpload(userId, messageId)

        // Then
        coVerify(exactly = 1) {
            draftUploader.upload(userId, messageId)
        }
    }

    @Test
    fun `should proxy injectAddressSignature accordingly`() = runTest {
        // Given
        val expectedUserId = UserId("user-id")
        val expectedDraftBody = DraftBody("body")
        val expectedSenderEmail = SenderEmail("sender@email.com")
        val expectedPreviousSenderEmail = null

        // When
        draftFacade.injectAddressSignature(
            expectedUserId,
            expectedDraftBody,
            expectedSenderEmail,
            expectedPreviousSenderEmail
        )

        // Then
        coVerify(exactly = 1) {
            injectAddressSignature(expectedUserId, expectedDraftBody, expectedSenderEmail, expectedPreviousSenderEmail)
        }
    }

    @Test
    fun `should return null if it can't get the local message from the parent id`() = runTest {
        // Given
        val expectedUserId = UserId("user-id")
        val expectedMessageId = MessageId("message-id")
        val expectedAction = DraftAction.Compose
        val expectedMessageDecrypted = DataError.Local.NoDataCached.left()
        coEvery { getLocalMessageDecrypted.invoke(expectedUserId, expectedMessageId) } returns expectedMessageDecrypted

        // When
        val result = draftFacade.parentMessageToDraftFields(expectedUserId, expectedMessageId, expectedAction)

        // Then
        coVerify(exactly = 1) { getLocalMessageDecrypted.invoke(expectedUserId, expectedMessageId) }
        assertNull(result)
    }

    @Test
    fun `should return null if it can't get the fields from the parent message`() = runTest {
        // Given
        val expectedUserId = UserId("user-id")
        val expectedMessageId = MessageId("message-id")
        val expectedAction = DraftAction.Compose
        val expectedMessageDecrypted = mockk<MessageWithDecryptedBody>().right()
        val expectedDraftFields = DataError.Local.Unknown.left()
        coEvery { getLocalMessageDecrypted.invoke(expectedUserId, expectedMessageId) } returns expectedMessageDecrypted
        coEvery {
            parentMessageToDraftFields.invoke(
                expectedUserId,
                expectedMessageDecrypted.getOrNull()!!,
                expectedAction
            )
        } returns expectedDraftFields

        // When
        val result = draftFacade.parentMessageToDraftFields(expectedUserId, expectedMessageId, expectedAction)

        // Then
        coVerify(exactly = 1) { getLocalMessageDecrypted.invoke(expectedUserId, expectedMessageId) }
        coVerify(exactly = 1) {
            parentMessageToDraftFields.invoke(
                expectedUserId,
                expectedMessageDecrypted.getOrNull()!!,
                expectedAction
            )
        }
        assertNull(result)
    }
}
