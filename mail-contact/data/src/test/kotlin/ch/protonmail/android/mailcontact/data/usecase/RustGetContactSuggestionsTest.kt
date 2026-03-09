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
package ch.protonmail.android.mailcontact.data.usecase

import ch.protonmail.android.mailcommon.data.mapper.LocalDeviceContact
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcontact.data.model.LocalDeviceContactsWithSignature
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import uniffi.mail_uniffi.AvatarInformation
import uniffi.mail_uniffi.ContactSuggestion
import uniffi.mail_uniffi.ContactSuggestionKind
import uniffi.mail_uniffi.ContactSuggestions
import uniffi.mail_uniffi.ContactSuggestionsResult
import uniffi.mail_uniffi.DeviceContactSuggestion
import uniffi.mail_uniffi.MailUserSession
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RustGetContactSuggestionsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rustSession = mockk<MailUserSession>()
    private val mailUserSession = mockk<MailUserSessionWrapper> {
        every { getRustUserSession() } returns rustSession
    }

    private val rustContactSuggestions = mockk<RustContactSuggestions>()
    private val getContactSuggestions = RustGetContactSuggestions(contactSuggestions = rustContactSuggestions)

    private val userId = UserIdSample.Primary

    private val aliceDeviceContact = LocalDeviceContact(
        key = "key-alice",
        name = "Alice Johnson",
        emails = listOf("alice@proton.me")
    )

    private val bobDeviceContact = LocalDeviceContact(
        key = "key-bob",
        name = "Bob Smith",
        emails = listOf("bob@proton.me")
    )

    private val aliceContactSuggestion = ContactSuggestion(
        key = "key-alice",
        name = "Alice Johnson",
        avatarInformation = AvatarInformation(text = "A", color = "#FF5733"),
        kind = ContactSuggestionKind.DeviceContact(DeviceContactSuggestion(email = "alice@proton.me"))
    )
    private val bobContactSuggestion = ContactSuggestion(
        key = "key-bob",
        name = "Bob Smith",
        avatarInformation = AvatarInformation(text = "B", color = "#33AFFF"),
        kind = ContactSuggestionKind.DeviceContact(DeviceContactSuggestion(email = "bob@proton.me"))
    )

    @Test
    fun `returns cached results when signatures match`() = runTest {
        // Given
        val contacts = listOf(aliceDeviceContact)
        val signature = 42L
        val device = LocalDeviceContactsWithSignature(contacts = contacts, signature = signature)
        val rawSuggestions = mockk<ContactSuggestions>(relaxed = true)
        val filteredList = listOf(aliceContactSuggestion)

        coEvery { rustContactSuggestions(contacts, rustSession) } returns ContactSuggestionsResult.Ok(rawSuggestions)
        every { rawSuggestions.filtered("ali") } returns filteredList

        // When
        val first = getContactSuggestions(userId, mailUserSession, device, "ali")
        val second = getContactSuggestions(userId, mailUserSession, device, "ali")

        // Then
        coVerify(exactly = 1) { rustContactSuggestions(contacts, rustSession) }
        assertEquals(filteredList, first.getOrNull())
        assertEquals(filteredList, second.getOrNull())
    }

    @Test
    fun `fetches fresh results when signature of device contacts changes`() = runTest {
        // Given
        val contactsV1 = listOf(aliceDeviceContact)
        val contactsV2 = listOf(bobDeviceContact)
        val deviceV1 = LocalDeviceContactsWithSignature(contacts = contactsV1, signature = 1L)
        val deviceV2 = LocalDeviceContactsWithSignature(contacts = contactsV2, signature = 2L)

        val suggestionsV1 = mockk<ContactSuggestions>(relaxed = true)
        val suggestionsV2 = mockk<ContactSuggestions>(relaxed = true)

        coEvery { rustContactSuggestions(contactsV1, rustSession) } returns ContactSuggestionsResult.Ok(suggestionsV1)
        coEvery { rustContactSuggestions(contactsV2, rustSession) } returns ContactSuggestionsResult.Ok(suggestionsV2)
        every { suggestionsV1.filtered(any()) } returns emptyList()
        every { suggestionsV2.filtered(any()) } returns emptyList()

        // When
        val first = getContactSuggestions(userId, mailUserSession, deviceV1, "a")
        val second = getContactSuggestions(userId, mailUserSession, deviceV2, "a")

        // Then
        coVerify(exactly = 1) { rustContactSuggestions(contactsV1, rustSession) }
        coVerify(exactly = 1) { rustContactSuggestions(contactsV2, rustSession) }
        assertTrue(first.isRight())
        assertTrue(second.isRight())
    }

    @Test
    fun `preload populates cache, subsequent invoke serves from cache`() = runTest {
        // Given
        val contacts = listOf(aliceDeviceContact)
        val signature = 9L
        val device = LocalDeviceContactsWithSignature(contacts = contacts, signature = signature)
        val suggestions = mockk<ContactSuggestions>(relaxed = true)

        coEvery { rustContactSuggestions(contacts, rustSession) } returns ContactSuggestionsResult.Ok(suggestions)
        every { suggestions.filtered("a") } returns listOf(aliceContactSuggestion)

        // When
        val preloadResult = getContactSuggestions.preload(userId, mailUserSession, device)

        // Then
        assertTrue(preloadResult.isRight())
        coVerify(exactly = 1) { rustContactSuggestions(contacts, rustSession) }

        // When
        val result = getContactSuggestions.invoke(userId, mailUserSession, device, "a")

        // Then
        coVerify(exactly = 1) { rustContactSuggestions(contacts, rustSession) }
        assertTrue(result.isRight())
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(aliceContactSuggestion, result.getOrNull()?.first())
    }
}
