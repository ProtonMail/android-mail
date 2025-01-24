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

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.repository.DraftRepository
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import kotlin.test.Test

class DiscardDraftTest {

    private val findLocalDraft = mockk<FindLocalDraft>()
    private val deleteMessages = mockk<DeleteMessages>()
    private val draftRepository = mockk<DraftRepository>()
    private val draftStateRepository = mockk<DraftStateRepository>()

    private val discardDraft = DiscardDraft(findLocalDraft, deleteMessages, draftRepository, draftStateRepository)

    @Test
    fun `invoke cancels upload draft, draft state and deletes message`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val draft = MessageWithBodySample.RemoteDraft
        val draftMessageId = draft.message.messageId

        givenFindLocalDraftReturnsDraft(userId, messageId, draft)
        givenDeleteDraftStateSucceeds(userId, draftMessageId)
        givenDeleteMessagesSucceeds(userId, draftMessageId, SystemLabelId.Drafts.labelId)
        givenCancelUploadDraftSucceeds()

        // When
        discardDraft(userId, messageId)

        // Then
        coVerifySequence {
            draftRepository.cancelUploadDraft(draftMessageId)
            draftStateRepository.deleteDraftState(userId, draftMessageId)
            deleteMessages(userId, listOf(draftMessageId), SystemLabelId.Drafts.labelId)
        }
    }

    private fun givenFindLocalDraftReturnsDraft(
        userId: UserId,
        messageId: MessageId,
        draft: MessageWithBody
    ) {
        coEvery { findLocalDraft(userId, messageId) } returns draft
    }

    private fun givenCancelUploadDraftSucceeds() {
        coEvery { draftRepository.cancelUploadDraft(any()) } returns Unit
    }

    private fun givenDeleteMessagesSucceeds(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ) {
        coEvery { deleteMessages(userId, listOf(messageId), labelId) } returns Unit
    }

    private fun givenDeleteDraftStateSucceeds(userId: UserId, messageId: MessageId) {
        coEvery { draftStateRepository.deleteDraftState(userId, messageId) } returns Unit.right()
    }
}
