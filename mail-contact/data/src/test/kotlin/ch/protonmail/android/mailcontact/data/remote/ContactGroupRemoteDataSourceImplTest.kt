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

package ch.protonmail.android.mailcontact.data.remote

import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.testdata.contact.ContactEmailSample
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.Serializable
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("MaxLineLength")
class ContactGroupRemoteDataSourceImplTest {

    private val userId = UserId("user_id")
    private val labelId = LabelId("label_id")
    private val contactEmailIds = setOf(
        ContactEmailSample.contactEmail1.id
    )

    private val enqueuer: Enqueuer = mockk(relaxed = true)

    private val contactGroupRemoteDataSource = ContactGroupRemoteDataSourceImpl(enqueuer)

    @Test
    fun `addContactEmailIdsToContactGroup should enqueue correct Worker`() {
        // When
        contactGroupRemoteDataSource.addContactEmailIdsToContactGroup(userId, labelId, contactEmailIds)

        // Then
        val editMembersOfContactGroupWorkerParams = slot<Map<String, Serializable>>()

        verify {
            enqueuer.enqueue<EditMembersOfContactGroupWorker>(
                userId,
                capture(editMembersOfContactGroupWorkerParams)
            )
        }

        val capturedUserId =
            editMembersOfContactGroupWorkerParams.captured[EditMembersOfContactGroupWorker.Companion.RawUserIdKey] as String
        val capturedLabelId =
            editMembersOfContactGroupWorkerParams.captured[EditMembersOfContactGroupWorker.Companion.RawLabelIdKey] as String
        val capturedAddedLabels =
            editMembersOfContactGroupWorkerParams.captured[EditMembersOfContactGroupWorker.Companion.RawLabelContactEmailIdsKey]!! as Array<String>
        val capturedRemovedLabels =
            editMembersOfContactGroupWorkerParams.captured[EditMembersOfContactGroupWorker.Companion.RawUnlabelContactEmailIdsKey]!! as Array<String>

        assertEquals(userId.id, capturedUserId)
        assertEquals(labelId.id, capturedLabelId)
        assertEquals(capturedAddedLabels.size, 1)
        assertEquals(capturedAddedLabels.first(), contactEmailIds.first().id)
        assertTrue(capturedRemovedLabels.isEmpty())
    }

    @Test
    fun `removeContactEmailIdsFromContactGroup should enqueue correct Worker`() {
        // When
        contactGroupRemoteDataSource.removeContactEmailIdsFromContactGroup(userId, labelId, contactEmailIds)

        // Then
        val editMembersOfContactGroupWorkerParams = slot<Map<String, Serializable>>()

        verify {
            enqueuer.enqueue<EditMembersOfContactGroupWorker>(
                userId,
                capture(editMembersOfContactGroupWorkerParams)
            )
        }

        val capturedUserId =
            editMembersOfContactGroupWorkerParams.captured[EditMembersOfContactGroupWorker.Companion.RawUserIdKey] as String
        val capturedLabelId =
            editMembersOfContactGroupWorkerParams.captured[EditMembersOfContactGroupWorker.Companion.RawLabelIdKey] as String
        val capturedAddedLabels =
            editMembersOfContactGroupWorkerParams.captured[EditMembersOfContactGroupWorker.Companion.RawLabelContactEmailIdsKey]!! as Array<String>
        val capturedRemovedLabels =
            editMembersOfContactGroupWorkerParams.captured[EditMembersOfContactGroupWorker.Companion.RawUnlabelContactEmailIdsKey]!! as Array<String>

        assertEquals(userId.id, capturedUserId)
        assertEquals(labelId.id, capturedLabelId)
        assertTrue(capturedAddedLabels.isEmpty())
        assertEquals(capturedRemovedLabels.size, 1)
        assertEquals(capturedRemovedLabels.first(), contactEmailIds.first().id)
    }

}
