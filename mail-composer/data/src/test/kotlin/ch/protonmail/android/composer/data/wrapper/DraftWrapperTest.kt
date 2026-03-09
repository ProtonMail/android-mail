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

package ch.protonmail.android.composer.data.wrapper

import ch.protonmail.android.mailcomposer.domain.model.BodyFields
import ch.protonmail.android.mailcomposer.domain.model.ComposerValues.EDITOR_ID
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.DraftHead
import io.mockk.every
import io.mockk.mockk
import uniffi.mail_uniffi.ComposerContent
import uniffi.mail_uniffi.Draft
import uniffi.mail_uniffi.DraftComposerContentResult
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DraftWrapperTest {

    @Test
    fun `should return the correct body fields (error)`() {
        // Given
        val rustDraft = mockk<Draft>()
        val fallbackBody = "fallback"
        val expected = BodyFields(DraftHead.Empty, DraftBody(fallbackBody))
        every { rustDraft.composerContent(any(), EDITOR_ID) } returns DraftComposerContentResult.Error(mockk())
        every { rustDraft.body() } returns fallbackBody

        // When
        val actual = DraftWrapper(rustDraft).bodyFields()

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should return the correct body fields (ok)`() {
        // Given
        val rustDraft = mockk<Draft>()
        val head = DraftHead("head")
        val body = DraftBody("body")
        val expected = BodyFields(head, body)

        every {
            rustDraft.composerContent(
                any(),
                EDITOR_ID
            )
        } returns DraftComposerContentResult.Ok(
            v1 = ComposerContent(head.value, body.value)
        )

        // When
        val actual = DraftWrapper(rustDraft).bodyFields()

        // Then
        assertEquals(expected, actual)
    }
}
