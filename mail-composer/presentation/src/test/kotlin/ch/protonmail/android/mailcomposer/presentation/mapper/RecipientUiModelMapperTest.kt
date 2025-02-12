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

import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel
import ch.protonmail.android.mailmessage.domain.model.Participant
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
    fun `should map from raw values (invalid)`() {
        // Given
        val expectedRecipientUiModel = listOf(
            RecipientUiModel.Invalid(invalidAddress),
            RecipientUiModel.Valid(validAddress2)
        )

        // When
        val actual = RecipientUiModelMapper.mapFromRawValue(listOf(invalidAddress, validAddress2))

        // Then
        assertEquals(expectedRecipientUiModel, actual)
    }

    @Test
    fun `should map from raw values (mixed)`() {
        // Given
        val expectedRecipientUiModel = listOf(
            RecipientUiModel.Valid(validAddress1),
            RecipientUiModel.Invalid(invalidAddress),
            RecipientUiModel.Valid(validAddress2)
        )

        // When
        val actual = RecipientUiModelMapper.mapFromRawValue(
            listOf(
                validAddress1,
                invalidAddress,
                validAddress2
            )
        )

        // Then
        assertEquals(expectedRecipientUiModel, actual)
    }

    @Test
    fun `should map from participants (valid)`() {
        // Given
        val expectedRecipientUiModel = listOf(
            RecipientUiModel.Valid(validAddress1),
            RecipientUiModel.Valid(validAddress2)
        )

        // When
        val actual = RecipientUiModelMapper.mapFromParticipants(
            listOf(
                Participant(validAddress1, validAddress1.reversed()),
                Participant(validAddress2, validAddress2.reversed())
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
            RecipientUiModel.Valid(validAddress2)
        )

        // When
        val actual = RecipientUiModelMapper.mapFromParticipants(
            listOf(
                Participant(invalidAddress, invalidAddress.reversed()),
                Participant(validAddress2, validAddress2.reversed())
            )
        )

        // Then
        assertEquals(expectedRecipientUiModel, actual)
    }

    @Test
    fun `should map from participants (mixed)`() {
        // Given
        val expectedRecipientUiModel = listOf(
            RecipientUiModel.Valid(validAddress1),
            RecipientUiModel.Invalid(invalidAddress),
            RecipientUiModel.Valid(validAddress2)
        )

        // When
        val actual = RecipientUiModelMapper.mapFromParticipants(
            listOf(
                Participant(validAddress1, validAddress1.reversed()),
                Participant(invalidAddress, invalidAddress.reversed()),
                Participant(validAddress2, validAddress2.reversed())
            )
        )

        // Then
        assertEquals(expectedRecipientUiModel, actual)
    }
}
