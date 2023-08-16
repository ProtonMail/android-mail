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

import java.util.UUID
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsDraftKnownToApiTest {

    private val isDraftKnowToApi = IsDraftKnownToApi()

    @Test
    fun `returns true when draft state has apiMessageId and messageId is not in UUID format`() {
        // Given
        val expectedDraftState = DraftStateSample.RemoteDraftState

        // When
        val actual = isDraftKnowToApi(expectedDraftState)

        // Then
        assertTrue(actual)
    }

    @Test
    fun `returns true when draft state has apiMessageId and messageId is in UUID format`() {
        // Given
        val expectedDraftState = DraftStateSample.LocalDraftThatWasSyncedOnce
        println("UUID.randomUUID() = ${UUID.randomUUID()}")

        // When
        val actual = isDraftKnowToApi(expectedDraftState)

        // Then
        assertTrue(actual)
    }


    @Test
    fun `returns true when draft state does not have apiMessageId and messageId is not in UUID format`() {
        // Given
        val expectedDraftState = DraftStateSample.RemoteWithoutApiMessageId

        // When
        val actual = isDraftKnowToApi(expectedDraftState)

        // Then
        assertTrue(actual)
    }

    @Test
    fun `returns false when draft state does not have apiMessageId and messageId is in UUID format`() {
        // Given
        val expectedDraftState = DraftStateSample.LocalDraftNeverSynced

        // When
        val actual = isDraftKnowToApi(expectedDraftState)

        // Then
        assertFalse(actual)
    }
}
