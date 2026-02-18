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

package ch.protonmail.android.mailcomposer.presentation.mapper

import ch.protonmail.android.mailcomposer.domain.model.DraftRecipient
import ch.protonmail.android.mailcomposer.domain.model.DraftRecipientValidity
import ch.protonmail.android.mailcomposer.domain.model.RecipientValidityError
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailpadlocks.domain.PrivacyLock
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RecipientUiModelMapperTest {

    private val validAddress1 = "noreply1@proton.me"
    private val validAddress2 = "noreply2@proton.me"
    private val invalidAddress = "proton+me"

    @Test
    fun `should map from raw values (valid)`() {
        // Given
        val expectedRecipientUiModel = listOf(
            RecipientUiModel.Valid(validAddress1),
            RecipientUiModel.Valid(validAddress2)
        )

        // When
        val actual = RecipientUiModelMapper.mapFromRawValue(listOf(validAddress1, validAddress2))

        // Then
        assertEquals(expectedRecipientUiModel, actual)
    }

    @Test
    fun `should map from raw values (invalid raw value to valid recipient)`() {
        // Given
        val expectedRecipientUiModel = listOf(
            RecipientUiModel.Valid(invalidAddress),
            RecipientUiModel.Valid(validAddress2)
        )

        // When
        val actual = RecipientUiModelMapper.mapFromRawValue(listOf(invalidAddress, validAddress2))

        // Then
        assertEquals(expectedRecipientUiModel, actual)
    }

    @Test
    fun `should map from participants (valid to validating)`() {
        // Given
        val expectedRecipientUiModel = listOf(
            RecipientUiModel.Validating(validAddress1),
            RecipientUiModel.Validating(validAddress2)
        )

        // When
        val actual = RecipientUiModelMapper.mapFromDraftRecipients(
            listOf(
                DraftRecipient.SingleRecipient(validAddress1.reversed(), validAddress1, privacyLock = PrivacyLock.None),
                DraftRecipient.SingleRecipient(validAddress2.reversed(), validAddress2, privacyLock = PrivacyLock.None)
            )
        )

        // Then
        assertEquals(
            expectedRecipientUiModel, actual
        )
    }

    @Test
    fun `should map from participants (invalid)`() {
        // Given
        val expectedRecipientUiModel = listOf(
            RecipientUiModel.Invalid(invalidAddress),
            RecipientUiModel.Validating(validAddress2)
        )

        // When
        val actual = RecipientUiModelMapper.mapFromDraftRecipients(
            listOf(
                DraftRecipient.SingleRecipient(
                    invalidAddress.reversed(),
                    invalidAddress,
                    DraftRecipientValidity.Invalid(RecipientValidityError.Format),
                    privacyLock = PrivacyLock.None
                ),
                DraftRecipient.SingleRecipient(validAddress2.reversed(), validAddress2, privacyLock = PrivacyLock.None)
            )
        )

        // Then
        assertEquals(expectedRecipientUiModel, actual)
    }

    @Test
    fun `should map from participants (mixed)`() {
        // Given
        val expectedRecipientUiModel = listOf(
            RecipientUiModel.Validating(validAddress1),
            RecipientUiModel.Invalid(invalidAddress),
            RecipientUiModel.Validating(validAddress2)
        )

        // When
        val actual = RecipientUiModelMapper.mapFromDraftRecipients(
            listOf(
                DraftRecipient.SingleRecipient(validAddress1.reversed(), validAddress1, privacyLock = PrivacyLock.None),
                DraftRecipient.SingleRecipient(
                    invalidAddress.reversed(),
                    invalidAddress,
                    DraftRecipientValidity.Invalid(RecipientValidityError.Format),
                    privacyLock = PrivacyLock.None
                ),
                DraftRecipient.SingleRecipient(validAddress2.reversed(), validAddress2, privacyLock = PrivacyLock.None)
            )
        )

        // Then
        assertEquals(expectedRecipientUiModel, actual)
    }

    @Test
    fun `should map from GroupRecipient to RecipientUiModel Group`() {
        // Given
        val groupName = "Team Proton"
        val member1 = DraftRecipient.SingleRecipient("Alice", validAddress1, privacyLock = PrivacyLock.None)
        val member2 = DraftRecipient.SingleRecipient("Bob", validAddress2, privacyLock = PrivacyLock.None)
        val groupRecipient = DraftRecipient.GroupRecipient(
            name = groupName,
            recipients = listOf(member1, member2)
        )
        val expectedRecipientUiModel = listOf(
            RecipientUiModel.Group(
                name = groupName,
                members = listOf(validAddress1, validAddress2),
                color = ""
            )
        )

        // When
        val actual = RecipientUiModelMapper.mapFromDraftRecipients(listOf(groupRecipient))

        // Then
        assertEquals(expectedRecipientUiModel, actual)
    }
}
