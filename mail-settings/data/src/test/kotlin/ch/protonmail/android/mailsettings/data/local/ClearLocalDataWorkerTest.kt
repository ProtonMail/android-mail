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

package ch.protonmail.android.mailsettings.data.local

import java.io.IOException
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.domain.sample.AccountSample
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailmessage.data.local.AttachmentLocalDataSource
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

internal class ClearLocalDataWorkerTest {

    private val context = mockk<Context>()
    private val parameters: WorkerParameters = mockk {
        every { taskExecutor } returns mockk(relaxed = true)
    }

    private val accountManager = mockk<AccountManager>()
    private val messageLocalDataSource = mockk<MessageLocalDataSource>()
    private val conversationLocalDataSource = mockk<ConversationLocalDataSource>()
    private val attachmentsLocalDataSource = mockk<AttachmentLocalDataSource>()
    private val worker = ClearLocalDataWorker(
        context,
        parameters,
        accountManager,
        messageLocalDataSource,
        conversationLocalDataSource,
        attachmentsLocalDataSource
    )

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should do nothing when accounts list is empty`() = runTest {
        // Given
        expectBaseInputData()
        every { accountManager.getAccounts() } returns flowOf(emptyList())

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify {
            messageLocalDataSource wasNot called
            conversationLocalDataSource wasNot called
            attachmentsLocalDataSource wasNot called
        }
    }

    @Test
    fun `should not interrupt in case of failure when deleting the app cache`() = runTest {
        // Given
        expectBaseInputData()
        expectValidSingleAccount()
        expectCacheClearToFail()
        expectValidMessageDataSourceInteraction(BaseUserId)
        expectValidAttachmentDataSourceInteraction(BaseUserId)

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerifySequence {
            messageLocalDataSource.deleteAllMessages(BaseUserId)
            conversationLocalDataSource.deleteAllConversations(BaseUserId)
            attachmentsLocalDataSource.deleteAttachments(BaseUserId)
        }
    }

    @Test
    fun `should not interrupt in case of failure when deleting cached message data`() = runTest {
        // Given
        expectBaseInputData()
        expectValidSingleAccount()
        expectValidCacheClearInteraction()
        expectMessageDataSourceInteractionToFail(BaseUserId)
        expectValidAttachmentDataSourceInteraction(BaseUserId)

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerifySequence {
            messageLocalDataSource.deleteAllMessages(BaseUserId)
            attachmentsLocalDataSource.deleteAttachments(BaseUserId)
        }
        coVerify(exactly = 0) {
            conversationLocalDataSource.deleteAllConversations(BaseUserId)
        }
    }

    @Test
    fun `should not fail in case attachments data source throws`() = runTest {
        // Given
        expectBaseInputData()
        expectValidSingleAccount()
        expectValidCacheClearInteraction()
        expectValidMessageDataSourceInteraction(BaseUserId)
        expectAttachmentDataSourceInteractionToFail(BaseUserId)

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerifySequence {
            messageLocalDataSource.deleteAllMessages(BaseUserId)
            conversationLocalDataSource.deleteAllConversations(BaseUserId)
            attachmentsLocalDataSource.deleteAttachments(BaseUserId)
        }
    }

    private fun expectValidSingleAccount() {
        every { accountManager.getAccounts() } returns flowOf(listOf(AccountSample.Primary))
    }

    private fun expectValidCacheClearInteraction() {
        every { context.externalCacheDir?.deleteRecursively() } returns true
        every { context.codeCacheDir.deleteRecursively() } returns true
    }

    private fun expectValidMessageDataSourceInteraction(userId: UserId) {
        coEvery {
            messageLocalDataSource.deleteAllMessages(userId)
        } just runs
        coEvery {
            conversationLocalDataSource.deleteAllConversations(userId)
        } just runs
    }

    private fun expectValidAttachmentDataSourceInteraction(userId: UserId) {
        coEvery {
            attachmentsLocalDataSource.deleteAttachments(userId)
        } returns true
    }

    private fun expectBaseInputData() {
        every {
            parameters.inputData.getBoolean(ClearLocalDataWorker.KeyClearLocalCache, false)
        } returns true

        every {
            parameters.inputData.getBoolean(ClearLocalDataWorker.KeyClearAttachments, false)
        } returns true

        every {
            parameters.inputData.getBoolean(ClearLocalDataWorker.KeyClearMessagesData, false)
        } returns true
    }

    private fun expectCacheClearToFail() {
        every { context.externalCacheDir } returns null
        every { context.codeCacheDir.deleteRecursively() } returns false
    }

    private fun expectMessageDataSourceInteractionToFail(userId: UserId) {
        coEvery { messageLocalDataSource.deleteAllMessages(userId) } throws IOException()
    }

    private fun expectAttachmentDataSourceInteractionToFail(userId: UserId) {
        coEvery { attachmentsLocalDataSource.deleteAttachments(userId) } throws IOException()
    }

    private companion object {

        val BaseAccount = AccountSample.Primary
        val BaseUserId = BaseAccount.userId
    }
}
