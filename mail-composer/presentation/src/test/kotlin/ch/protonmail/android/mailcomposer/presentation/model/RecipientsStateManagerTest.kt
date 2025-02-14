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

package ch.protonmail.android.mailcomposer.presentation.model

import ch.protonmail.android.mailmessage.domain.model.Participant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class RecipientsStateManagerTest {

    @Test
    fun `initial state should be empty`() {
        // Given
        val recipientsStateManager = RecipientsStateManager()

        // When
        val actual = recipientsStateManager.recipients.value

        // Then
        assertEquals(RecipientsState.Empty, actual)
    }

    @Test
    fun `should return invalid recipients when empty()`() {
        // Given
        val recipientsStateManager = RecipientsStateManager()

        // Then
        assertFalse(recipientsStateManager.hasValidRecipients())
    }

    @Test
    fun `should return valid when not empty with valid recipients()`() {
        // Given
        val recipientsStateManager = RecipientsStateManager()
        val rawToList = listOf("a@bb.cc", "cc@bb.aa")
        val rawCcList = listOf("one@two.three", "two@three.four")
        val rawBccList = listOf("bcc@cc.to", "to@cc.bcc")

        // When
        recipientsStateManager.setFromRawRecipients(
            toRecipients = rawToList,
            ccRecipients = rawCcList,
            bccRecipients = rawBccList
        )

        // Then
        assertTrue(recipientsStateManager.hasValidRecipients())
    }

    @Test
    fun `should set recipients when updated from raw values`() {
        // Given
        val recipientsStateManager = RecipientsStateManager()

        val rawToList = listOf("a@bb.cc", "@@")
        val expectedToList = listOf(RecipientUiModel.Valid("a@bb.cc"), RecipientUiModel.Invalid("@@"))

        val rawCcList = listOf("one@two.three", "123", "two@three.four", "three@four.five")
        val expectedCcList = listOf(
            RecipientUiModel.Valid("one@two.three"),
            RecipientUiModel.Invalid("123"),
            RecipientUiModel.Valid("two@three.four"),
            RecipientUiModel.Valid("three@four.five")
        )

        val rawBccList = listOf("aaa", "test@example.com", "com@example.test")
        val expectedBccList = listOf(
            RecipientUiModel.Invalid("aaa"),
            RecipientUiModel.Valid("test@example.com"),
            RecipientUiModel.Valid("com@example.test")
        )

        // When
        recipientsStateManager.setFromRawRecipients(
            toRecipients = rawToList,
            ccRecipients = rawCcList,
            bccRecipients = rawBccList
        )

        val actualRecipients = recipientsStateManager.recipients.value

        // Then
        assertEquals(expectedToList, actualRecipients.toRecipients)
        assertEquals(expectedCcList, actualRecipients.ccRecipients)
        assertEquals(expectedBccList, actualRecipients.bccRecipients)
    }

    @Test
    fun `should set recipients when updated from participants`() {
        // Given
        val recipientsStateManager = RecipientsStateManager()

        val rawToList = listOf(generateParticipant("a@bb.cc"), generateParticipant("@@"))
        val expectedToList = listOf(RecipientUiModel.Valid("a@bb.cc"), RecipientUiModel.Invalid("@@"))

        val rawCcList = listOf(
            generateParticipant("one@two.three"),
            generateParticipant("123"),
            generateParticipant("two@three.four"),
            generateParticipant("three@four.five")
        )
        val expectedCcList = listOf(
            RecipientUiModel.Valid("one@two.three"),
            RecipientUiModel.Invalid("123"),
            RecipientUiModel.Valid("two@three.four"),
            RecipientUiModel.Valid("three@four.five")
        )

        val rawBccList = listOf(
            generateParticipant("aaa"),
            generateParticipant("test@example.com"),
            generateParticipant("com@example.test")
        )
        val expectedBccList = listOf(
            RecipientUiModel.Invalid("aaa"),
            RecipientUiModel.Valid("test@example.com"),
            RecipientUiModel.Valid("com@example.test")
        )

        // When
        recipientsStateManager.setFromParticipants(
            toRecipients = rawToList,
            ccRecipients = rawCcList,
            bccRecipients = rawBccList
        )

        val actualRecipients = recipientsStateManager.recipients.value

        // Then
        assertEquals(expectedToList, actualRecipients.toRecipients)
        assertEquals(expectedCcList, actualRecipients.ccRecipients)
        assertEquals(expectedBccList, actualRecipients.bccRecipients)
    }

    @Test
    fun `should update the recipients depending on the recipient type`() {
        // Given
        val recipientsStateManager = RecipientsStateManager()
        val toRecipients = listOf<RecipientUiModel>(
            RecipientUiModel.Valid("valid@toaddress.com"),
            RecipientUiModel.Invalid("invalidtoaddress.com")
        )

        val ccRecipients = listOf<RecipientUiModel>(
            RecipientUiModel.Valid("valid@toaddress.com"),
            RecipientUiModel.Invalid("invalidcctoaddress.com")
        )

        val bccRecipients = listOf<RecipientUiModel>(
            RecipientUiModel.Valid("valid@bccaddress.com"),
            RecipientUiModel.Invalid("invalidbccaddress.com")
        )

        // When
        recipientsStateManager.updateRecipients(toRecipients, ContactSuggestionsField.TO)
        recipientsStateManager.updateRecipients(ccRecipients, ContactSuggestionsField.CC)
        recipientsStateManager.updateRecipients(bccRecipients, ContactSuggestionsField.BCC)

        val actualRecipients = recipientsStateManager.recipients.value

        // Then
        assertEquals(toRecipients, actualRecipients.toRecipients)
        assertEquals(ccRecipients, actualRecipients.ccRecipients)
        assertEquals(bccRecipients, actualRecipients.bccRecipients)
    }

    private fun generateParticipant(address: String) = Participant(name = address.reversed(), address = address)
}
