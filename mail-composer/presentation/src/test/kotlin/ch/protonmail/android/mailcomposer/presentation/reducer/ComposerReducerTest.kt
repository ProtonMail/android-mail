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

package ch.protonmail.android.mailcomposer.presentation.reducer

import java.util.UUID
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.RecipientsBccChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.RecipientsCcChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction.RecipientsToChanged
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState.NotSubmittable
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState.Submittable
import ch.protonmail.android.mailcomposer.presentation.model.ComposerFields
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel.Invalid
import ch.protonmail.android.mailcomposer.presentation.model.RecipientUiModel.Valid
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import org.junit.Test
import kotlin.test.assertEquals

class ComposerReducerTest {

    @Test
    fun `Should generate submittable state when adding a new valid email address in the to field`() {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        val emailAddress = "a@b.c"
        val reducer = ComposerReducer()
        val currentState = ComposerDraftState.empty(messageId)
        val operation = RecipientsToChanged(listOf(Valid(emailAddress)))

        // When
        val newState = reducer.newStateFrom(currentState, operation)

        // Then
        assertEquals(
            Submittable(
                fields = ComposerFields(
                    draftId = messageId,
                    from = "",
                    to = listOf(Valid(emailAddress)),
                    cc = emptyList(),
                    bcc = emptyList(),
                    subject = "",
                    body = ""
                )
            ),
            newState
        )
    }

    @Test
    fun `Should generate not submittable state with error when adding a new invalid email address in the to field`() {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        val emailAddress = UUID.randomUUID().toString()
        val reducer = ComposerReducer()
        val currentState = ComposerDraftState.empty(messageId)
        val operation = RecipientsToChanged(listOf(Invalid(emailAddress)))

        // When
        val newState = reducer.newStateFrom(currentState, operation)

        // Then
        assertEquals(
            NotSubmittable(
                fields = ComposerFields(
                    draftId = messageId,
                    from = "",
                    to = listOf(Invalid(emailAddress)),
                    cc = emptyList(),
                    bcc = emptyList(),
                    subject = "",
                    body = ""
                ),
                error = Effect.of(TextUiModel(R.string.composer_error_invalid_email))
            ),
            newState
        )
    }

    @Test
    fun `Should generate submittable state when adding a new valid email address in the cc field`() {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        val emailAddress = "a@b.c"
        val reducer = ComposerReducer()
        val currentState = ComposerDraftState.empty(messageId)
        val operation = RecipientsCcChanged(listOf(Valid(emailAddress)))

        // When
        val newState = reducer.newStateFrom(currentState, operation)

        // Then
        assertEquals(
            Submittable(
                fields = ComposerFields(
                    draftId = messageId,
                    from = "",
                    to = emptyList(),
                    cc = listOf(Valid(emailAddress)),
                    bcc = emptyList(),
                    subject = "",
                    body = ""
                )
            ),
            newState
        )
    }

    @Test
    fun `Should generate not submittable state with error when adding a new invalid email address in the cc field`() {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        val emailAddress = UUID.randomUUID().toString()
        val reducer = ComposerReducer()
        val currentState = ComposerDraftState.empty(messageId)
        val operation = RecipientsCcChanged(listOf(Invalid(emailAddress)))

        // When
        val newState = reducer.newStateFrom(currentState, operation)

        // Then
        assertEquals(
            NotSubmittable(
                fields = ComposerFields(
                    draftId = messageId,
                    from = "",
                    to = emptyList(),
                    cc = listOf(Invalid(emailAddress)),
                    bcc = emptyList(),
                    subject = "",
                    body = ""
                ),
                error = Effect.of(TextUiModel(R.string.composer_error_invalid_email))
            ),
            newState
        )
    }

    @Test
    fun `Should generate submittable state when adding a new valid email address in the bcc field`() {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        val emailAddress = "a@b.c"
        val reducer = ComposerReducer()
        val currentState = ComposerDraftState.empty(messageId)
        val operation = RecipientsBccChanged(listOf(Valid(emailAddress)))

        // When
        val newState = reducer.newStateFrom(currentState, operation)

        // Then
        assertEquals(
            Submittable(
                fields = ComposerFields(
                    draftId = messageId,
                    from = "",
                    to = emptyList(),
                    cc = emptyList(),
                    bcc = listOf(Valid(emailAddress)),
                    subject = "",
                    body = ""
                )
            ),
            newState
        )
    }

    @Test
    fun `Should generate not submittable state with error when adding a new invalid email address in the bcc field`() {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        val emailAddress = UUID.randomUUID().toString()
        val reducer = ComposerReducer()
        val currentState = ComposerDraftState.empty(messageId)
        val operation = RecipientsBccChanged(listOf(Invalid(emailAddress)))

        // When
        val newState = reducer.newStateFrom(currentState, operation)

        // Then
        assertEquals(
            NotSubmittable(
                fields = ComposerFields(
                    draftId = messageId,
                    from = "",
                    to = emptyList(),
                    cc = emptyList(),
                    bcc = listOf(Invalid(emailAddress)),
                    subject = "",
                    body = ""
                ),
                error = Effect.of(TextUiModel(R.string.composer_error_invalid_email))
            ),
            newState
        )
    }

    @Test
    fun `Should generate not submittable state without error when adding invalid followed by valid address`() {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        val emailAddress = UUID.randomUUID().toString()
        val reducer = ComposerReducer()
        val currentState = ComposerDraftState.empty(messageId)
        val operation1 = RecipientsToChanged(listOf(Invalid(emailAddress)))
        val operation2 = RecipientsToChanged(listOf(Invalid(emailAddress), Valid(emailAddress)))

        // When
        var newState = reducer.newStateFrom(currentState, operation1)
        newState = reducer.newStateFrom(newState, operation2)

        // Then
        assertEquals(
            NotSubmittable(
                fields = ComposerFields(
                    draftId = messageId,
                    from = "",
                    to = listOf(Invalid(emailAddress), Valid(emailAddress)),
                    cc = emptyList(),
                    bcc = emptyList(),
                    subject = "",
                    body = ""
                ),
                error = Effect.empty()
            ),
            newState
        )
    }

    @Test
    fun `Should generate not submittable state with error when adding invalid followed by invalid address`() {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        val emailAddress = UUID.randomUUID().toString()
        val reducer = ComposerReducer()
        val currentState = ComposerDraftState.empty(messageId)
        val operation1 = RecipientsToChanged(listOf(Invalid(emailAddress)))
        val operation2 = RecipientsToChanged(listOf(Invalid(emailAddress), Invalid(emailAddress)))

        // When
        var newState = reducer.newStateFrom(currentState, operation1)
        newState = reducer.newStateFrom(newState, operation2)

        // Then
        assertEquals(
            NotSubmittable(
                fields = ComposerFields(
                    draftId = messageId,
                    from = "",
                    to = listOf(Invalid(emailAddress), Invalid(emailAddress)),
                    cc = emptyList(),
                    bcc = emptyList(),
                    subject = "",
                    body = ""
                ),
                error = Effect.of(TextUiModel(R.string.composer_error_invalid_email))
            ),
            newState
        )
    }

    @Test
    fun `Should generate not submittable state without error when removing invalid address`() {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        val emailAddress = UUID.randomUUID().toString()
        val reducer = ComposerReducer()
        val currentState = ComposerDraftState.empty(messageId)
        val operation1 = RecipientsToChanged(listOf(Invalid(emailAddress), Invalid(emailAddress)))
        val operation2 = RecipientsToChanged(listOf(Invalid(emailAddress)))

        // When
        var newState = reducer.newStateFrom(currentState, operation1)
        newState = reducer.newStateFrom(newState, operation2)

        // Then
        assertEquals(
            NotSubmittable(
                fields = ComposerFields(
                    draftId = messageId,
                    from = "",
                    to = listOf(Invalid(emailAddress)),
                    cc = emptyList(),
                    bcc = emptyList(),
                    subject = "",
                    body = ""
                ),
                error = Effect.empty()
            ),
            newState
        )
    }
}
