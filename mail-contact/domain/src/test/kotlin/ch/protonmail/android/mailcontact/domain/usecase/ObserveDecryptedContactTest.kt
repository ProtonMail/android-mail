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

package ch.protonmail.android.mailcontact.domain.usecase

import app.cash.turbine.test
import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.DecryptedContact
import ch.protonmail.android.testdata.contact.ContactWithCardsSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ObserveDecryptedContactTest {

    private val contactRepositoryMock = mockk<ContactRepository> {
        every {
            this@mockk.observeContactWithCards(
                UserIdTestData.Primary,
                ContactWithCardsSample.Mario.contact.id
            )
        } returns flowOf(
            DataResult.Success(ResponseSource.Local, ContactWithCardsSample.Mario)
        )
        every {
            this@mockk.observeContactWithCards(
                UserIdTestData.Primary,
                ContactWithCardsSample.Stefano.contact.id
            )
        } returns flowOf(
            DataResult.Success(ResponseSource.Local, ContactWithCardsSample.Stefano)
        )
    }

    private val labelRepository = mockk<LabelRepository> {
        every {
            this@mockk.observeLabels(
                UserIdTestData.Primary,
                LabelType.ContactGroup
            )
        } returns flowOf(
            DataResult.Success(ResponseSource.Local, listOf(LabelSample.GroupCoworkers, LabelSample.GroupFriends))
        )
    }

    private val decryptContactMock = mockk<DecryptContact> {
        coEvery {
            this@mockk.invoke(UserIdTestData.Primary, ContactWithCardsSample.Mario)
        } returns DecryptedContact(ContactWithCardsSample.Mario.contact.id)
        coEvery {
            this@mockk.invoke(UserIdTestData.Primary, ContactWithCardsSample.Stefano)
        } returns DecryptedContact(ContactWithCardsSample.Stefano.contact.id)
    }

    private val observeDecryptedContact = ObserveDecryptedContact(
        contactRepositoryMock,
        decryptContactMock,
        labelRepository
    )

    @Test
    fun `returns Contact with injected ContactGroups if there were any`() = runTest {
        // Given
        val expectedContactGroups = listOf(
            ContactGroup(
                LabelSample.GroupCoworkers.name,
                LabelSample.GroupCoworkers.color
            )
        )

        // When
        observeDecryptedContact(UserIdTestData.Primary, ContactWithCardsSample.Mario.contact.id).test {
            // Then
            val actual = assertIs<Either.Right<DecryptedContact>>(awaitItem())
            assertEquals(expectedContactGroups, actual.value.contactGroups)
            awaitComplete()
        }
    }

    @Test
    fun `returns Contact with no injected ContactGroups if there were none`() = runTest {
        // Given
        val expectedContactGroups = emptyList<ContactGroup>()

        // When
        observeDecryptedContact(UserIdTestData.Primary, ContactWithCardsSample.Stefano.contact.id).test {
            // Then
            val actual = assertIs<Either.Right<DecryptedContact>>(awaitItem())
            assertEquals(expectedContactGroups, actual.value.contactGroups)
            awaitComplete()
        }
    }
}
