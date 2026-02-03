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

import ch.protonmail.android.mailcomposer.domain.model.DraftRecipient
import ch.protonmail.android.mailpadlocks.domain.PrivacyLock
import ch.protonmail.android.mailpadlocks.presentation.model.EncryptionInfoUiModel
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
        val expectedToList: List<RecipientUiModel> = listOf(
            RecipientUiModel.Valid("a@bb.cc"),
            RecipientUiModel.Valid("@@")
        )

        val rawCcList = listOf("one@two.three", "123", "two@three.four", "three@four.five")
        val expectedCcList: List<RecipientUiModel> = listOf(
            RecipientUiModel.Valid("one@two.three"),
            RecipientUiModel.Valid("123"),
            RecipientUiModel.Valid("two@three.four"),
            RecipientUiModel.Valid("three@four.five")
        )

        val rawBccList = listOf("aaa", "test@example.com", "com@example.test")
        val expectedBccList: List<RecipientUiModel> = listOf(
            RecipientUiModel.Valid("aaa"),
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
    fun `should set recipients when updated from recipient state manager`() {
        // Given
        val recipientsStateManager = RecipientsStateManager()

        val rawToList = listOf(
            DraftRecipient.SingleRecipient(name = "", address = "a@bb.cc", privacyLock = PrivacyLock.None),
            DraftRecipient.SingleRecipient(name = "", address = "@@", privacyLock = PrivacyLock.None)
        )
        val expectedToList: List<RecipientUiModel> = listOf(
            RecipientUiModel.Validating("a@bb.cc"),
            RecipientUiModel.Validating("@@")
        )

        val rawCcList = listOf(
            DraftRecipient.SingleRecipient(name = "", address = "one@two.three", privacyLock = PrivacyLock.None),
            DraftRecipient.SingleRecipient(name = "", address = "123", privacyLock = PrivacyLock.None),
            DraftRecipient.SingleRecipient(name = "", address = "two@three.four", privacyLock = PrivacyLock.None),
            DraftRecipient.SingleRecipient(name = "", address = "three@four.five", privacyLock = PrivacyLock.None)
        )
        val expectedCcList: List<RecipientUiModel> = listOf(
            RecipientUiModel.Validating("one@two.three"),
            RecipientUiModel.Validating("123"),
            RecipientUiModel.Validating("two@three.four"),
            RecipientUiModel.Validating("three@four.five")
        )

        val rawBccList = listOf(
            DraftRecipient.SingleRecipient(name = "", address = "aaa", privacyLock = PrivacyLock.None),
            DraftRecipient.SingleRecipient(name = "", address = "test@example.com", privacyLock = PrivacyLock.None),
            DraftRecipient.SingleRecipient(name = "", address = "com@example.test", privacyLock = PrivacyLock.None)
        )
        val expectedBccList: List<RecipientUiModel> = listOf(
            RecipientUiModel.Validating("aaa"),
            RecipientUiModel.Validating("test@example.com"),
            RecipientUiModel.Validating("com@example.test")
        )

        // When
        recipientsStateManager.setFromDraftRecipients(
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
        val toRecipients = listOf(
            RecipientUiModel.Valid("valid@toaddress.com"),
            RecipientUiModel.Invalid("invalidtoaddress.com")
        )

        val ccRecipients = listOf(
            RecipientUiModel.Valid("valid@toaddress.com"),
            RecipientUiModel.Invalid("invalidcctoaddress.com")
        )

        val bccRecipients = listOf(
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

    @Test
    fun `resetValidationState should set all recipients to Validating`() {
        // Given
        val recipientsStateManager = RecipientsStateManager()
        val initialToRecipients = listOf(
            RecipientUiModel.Valid("to@example.com", EncryptionInfoUiModel.NoLock)
        )
        val initialCcRecipients = listOf(
            RecipientUiModel.Valid("cc@example.com", EncryptionInfoUiModel.NoLock)
        )
        recipientsStateManager.updateRecipients(initialToRecipients, ContactSuggestionsField.TO)
        recipientsStateManager.updateRecipients(initialCcRecipients, ContactSuggestionsField.CC)

        // When
        recipientsStateManager.resetValidationState()

        // Then
        val currentState = recipientsStateManager.recipients.value
        assertTrue(currentState.toRecipients.all { it is RecipientUiModel.Validating })
        assertTrue(currentState.ccRecipients.all { it is RecipientUiModel.Validating })
        assertEquals("to@example.com", currentState.toRecipients.first().address)
        assertEquals("cc@example.com", currentState.ccRecipients.first().address)
    }

    @Test
    fun `restoreState should restore previously saved state`() {
        // Given
        val recipientsStateManager = RecipientsStateManager()
        val initialToRecipients = listOf(
            RecipientUiModel.Valid("to@example.com", EncryptionInfoUiModel.NoLock)
        )
        recipientsStateManager.updateRecipients(initialToRecipients, ContactSuggestionsField.TO)
        val savedState = recipientsStateManager.recipients.value

        // Clear and verify state changed
        recipientsStateManager.resetValidationState()
        assertTrue(recipientsStateManager.recipients.value.toRecipients.first() is RecipientUiModel.Validating)

        // When
        recipientsStateManager.restoreState(savedState)

        // Then
        assertEquals(savedState, recipientsStateManager.recipients.value)
        assertTrue(recipientsStateManager.recipients.value.toRecipients.first() is RecipientUiModel.Valid)
    }

}
