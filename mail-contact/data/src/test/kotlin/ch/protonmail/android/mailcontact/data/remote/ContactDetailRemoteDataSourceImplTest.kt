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
import io.mockk.every
import io.mockk.mockk
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

class ContactDetailRemoteDataSourceImplTest {

    val userId = UserId("user_id")
    val contactId = ContactId("contact_id")

    private val enqueuer: Enqueuer = mockk {
        every {
            enqueue<DeleteContactWorker>(userId, DeleteContactWorker.params(userId.id, contactId.id))
        } returns mockk()
    }

    private val contactDetailRemoteDataSource = ContactDetailRemoteDataSourceImpl(enqueuer)

    @Test
    fun `deleteContact should call enqueuer enqueue`() {

        // When
        contactDetailRemoteDataSource.deleteContact(userId, contactId)

        // Then
        every { enqueuer.enqueue<DeleteContactWorker>(userId, DeleteContactWorker.params(userId.id, contactId.id)) }
    }

}
