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
import ch.protonmail.android.mailmessage.domain.model.MessageId
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import org.junit.Test

internal class ProvideNewDraftIdTest {

    private val provideNewDraftId = ProvideNewDraftId()

    @Test
    fun `should provide a new draft id`() {
        // Given
        val expectedDraftRawId = "8236daf9-db4b-45b0-855d-a9676d890b6d"
        val expectedDraftMessageId = MessageId(expectedDraftRawId)
        mockkStatic(UUID::randomUUID)
        every { UUID.randomUUID() } returns UUID.fromString(expectedDraftRawId)

        // When
        val actualDraftMessageId = provideNewDraftId()

        // Then
        assertEquals(expectedDraftMessageId, actualDraftMessageId)
        unmockkStatic(UUID::randomUUID)
    }
}
