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

package ch.protonmail.android.mailcomposer.domain.usecase

import android.net.Uri
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftState
import ch.protonmail.android.mailcomposer.domain.model.DraftSyncState
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.test.utils.FakeTransactor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class StoreAttachmentsTest {

    private val userId = UserIdSample.Primary
    private val localMessageId = MessageId("localMessageId")
    private val remoteMessageId = MessageId("remoteMessageId")
    private val localAttachmentId = AttachmentId("localAttachmentId")

    private val uri = mockk<Uri>()
    private val attachmentRepository = mockk<AttachmentRepository>(relaxUnitFun = true)
    private val draftStateRepository = mockk<DraftStateRepository>()
    private val provideNewAttachmentId = mockk<ProvideNewAttachmentId> {
        every { this@mockk.invoke() } returns localAttachmentId
    }
    private val transactor = FakeTransactor()

    private val storeAttachments = StoreAttachments(
        attachmentRepository,
        draftStateRepository,
        provideNewAttachmentId,
        transactor
    )

    @Test
    fun `should save attachment with local message id when remote message id doesn't exist`() = runTest {
        // Given
        val draftState = DraftState(userId, localMessageId, null, DraftSyncState.Local, DraftAction.Compose)
        coEvery { draftStateRepository.observe(userId, localMessageId) } returns flowOf(draftState.right())

        // When
        storeAttachments(userId, localMessageId, listOf(uri))

        // Then
        coVerify { attachmentRepository.saveAttachment(userId, localMessageId, localAttachmentId, uri) }
    }

    @Test
    fun `should save attachment with remote id when remote id exists`() = runTest {
        // Given
        val draftState = DraftState(userId, localMessageId, remoteMessageId, DraftSyncState.Local, DraftAction.Compose)
        coEvery { draftStateRepository.observe(userId, localMessageId) } returns flowOf(draftState.right())

        // When
        storeAttachments(userId, localMessageId, listOf(uri))

        // Then
        coVerify { attachmentRepository.saveAttachment(userId, remoteMessageId, localAttachmentId, uri) }
    }
}
