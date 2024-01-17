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

package ch.protonmail.android.mailcontact.data.local

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.contact.domain.repository.ContactLocalDataSource
import kotlin.test.Test

class ContactDetailLocalDataSourceImplTest {

    private val contactLocalDataSource = mockk<ContactLocalDataSource>(relaxUnitFun = true)

    private val contactDetailLocalDataSourceImpl by lazy { ContactDetailLocalDataSourceImpl(contactLocalDataSource) }

    @Test
    fun `deleteContact should call contactLocalDataSource deleteContacts`() = runTest {
        // Given
        val contactId = ContactId("contact_id")

        // When
        contactDetailLocalDataSourceImpl.deleteContact(contactId)

        // Then
        coVerify { contactLocalDataSource.deleteContacts(contactId) }
    }
}
